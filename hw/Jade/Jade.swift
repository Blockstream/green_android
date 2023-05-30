import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth
import SwiftCBOR
import greenaddress

public protocol JadeGdkRequest: AnyObject {
    func httpRequest(params: [String: Any]) -> [String: Any]?
}

final public class Jade: JadeOTA, HWProtocol {

    public static let shared = Jade()
    let SIGHASH_ALL: UInt8 = 1
    public weak var gdkRequestDelegate: JadeGdkRequest?

    public func httpRequest<T: Codable, K: Codable>(_ httpRequest: JadeHttpRequest<T>) throws -> K {
        let encoded = try JSONEncoder().encode(httpRequest.params)
        let serialized = try JSONSerialization.jsonObject(with: encoded, options: .allowFragments) as? [String: Any]
        let httpResponse = gdkRequestDelegate?.httpRequest(params: serialized ?? [:])
        let httpResponseBody = httpResponse?["body"] as? [String: Any]
        let deserialized = try JSONSerialization.data(withJSONObject: httpResponseBody ?? [:], options: .fragmentsAllowed)
        return try JSONDecoder().decode(K.self, from: deserialized)
    }

    public func unlock(network: String) -> Observable<Bool> {
        // Send initial auth user request
        let epoch = Date().timeIntervalSince1970
        let cmd = JadeAuthRequest(network: network, epoch: UInt32(epoch))
        return exchange(JadeRequest<JadeAuthRequest>(method: "auth_user", params: cmd))
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .compactMap { (res: JadeResponse<Bool>) -> Bool in
                if let result = res.result, result {
                    return result
                }
                throw HWError.Abort(res.error?.message ?? "Invalid pin")
            }
    }

    public func auth(network: String) -> Observable<Bool> {
        // Send initial auth user request
        let epoch = Date().timeIntervalSince1970
        let cmd = JadeAuthRequest(network: network, epoch: UInt32(epoch))
        return exchange(JadeRequest<JadeAuthRequest>(method: "auth_user", params: cmd))
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { (res: JadeResponse<JadeAuthResponse<String>>) -> Observable<JadeResponse<JadeAuthResponse<JadeHandshakeComplete>>> in
                let request: JadeHttpRequest<String> = res.result!.httpRequest
                let cmd: JadeHandshakeInit = try self.httpRequest(request)
                let jadePackage = JadeRequest<JadeHandshakeInit>(method: res.result?.httpRequest.onReply ?? "",
                                                                params: cmd)
                return self.exchange(jadePackage)
            }.flatMap { (res: JadeResponse<JadeAuthResponse<JadeHandshakeComplete>>) -> Observable<JadeResponse<Bool>> in
                let request: JadeHttpRequest<JadeHandshakeComplete> = res.result!.httpRequest
                let package: JadeHandshakeCompleteReply = try self.httpRequest(request)
                let jadePackage = JadeRequest<JadeHandshakeCompleteReply>(method: res.result?.httpRequest.onReply ?? "",
                                                                        params: package)
                return self.exchange(jadePackage)
            }.compactMap { (res: JadeResponse<Bool>) -> Bool in
                if let result = res.result, result {
                    return result
                }
                throw HWError.Abort(res.error?.message ?? "Invalid pin")
            }
    }
    
    public func signMessage(_ params: HWSignMessageParams) -> Observable<HWSignMessageResult> {
        let pathstr = getUnsignedPath(params.path)
        var obs: Observable<HWSignMessageResult>?
        if params.useAeProtocol ?? false {
            // Anti-exfil protocol:
            // We send the signing request with the host-commitment and receive the signer-commitment
            // in reply once the user confirms.
            // We can then request the actual signature passing the host-entropy.
            var signerCommitment: String?
            let msg = JadeSignMessage(message: params.message, path: pathstr, aeHostCommitment: params.aeHostCommitment?.hexToData())
            obs = signMessage(msg)
                .compactMap { signerCommitment = $0.hex }
                .compactMap { JadeGetSignature(aeHostEntropy: params.aeHostEntropy?.hexToData() ?? Data()) }
                .flatMap { self.getSignature($0) }
                .compactMap { HWSignMessageResult(signature: $0, signerCommitment: signerCommitment) }
        } else {
            // Standard EC signature, simple case
            let msg = JadeSignMessage(message: params.message, path: pathstr, aeHostCommitment: nil)
            obs = signMessage(msg).compactMap { HWSignMessageResult(signature: $0.hex, signerCommitment: nil) }
        }
        return obs!.compactMap { res -> HWSignMessageResult in
            // Convert the signature from Base64 into DER hex for GDK
            guard var sigDecoded = Data(base64Encoded: res.signature ?? "") else {
                throw HWError.Abort("Invalid signature")
            }
            // Need to truncate lead byte if recoverable signature
            if sigDecoded.count == WALLY_EC_SIGNATURE_RECOVERABLE_LEN {
                sigDecoded = sigDecoded[1...sigDecoded.count-1]
            }
            let sigDer = try sigToDer(sig: Array(sigDecoded))
            return HWSignMessageResult(signature: sigDer.hex, signerCommitment: res.signerCommitment)
        }
    }

    public func xpubs(network: String, paths: [[Int]]) -> Observable<[String]> {
        let allObservables = paths
            .map {
                Observable.just($0)
                    .flatMap { self.xpubs(network: network, path: $0) }
            }
        return Observable.concat(allObservables)
            .reduce([], accumulator: { result, element in
                result + [element]
            })
    }

    // swiftlint:disable:next function_parameter_count
    public func signTransaction(network: String,
                                params: HWSignTxParams) -> Observable<HWSignTxResponse> {
        if params.signingInputs.isEmpty {
            return Observable.error(HWError.Abort("Input transactions missing"))
        }
        let txInputs = params.signingInputs.map { input -> TxInputBtc? in
            var txhash: String? = input.txHash
            var satoshi: UInt64? = input.satoshi
            if input.isSegwit && params.signingInputs.count == 1 {
                txhash = nil
            } else if let hash = txhash, let tx = params.signingTxs[hash] {
                satoshi = nil
                txhash = tx
            } else {
                return nil
            }
            return TxInputBtc(
                isWitness: isSegwit(input.addressType),
                inputTx: txhash?.hexToData(),
                script: input.prevoutScript?.hexToData(),
                satoshi: satoshi,
                path: input.userPath ?? [],
                aeHostEntropy: input.aeHostEntropy?.hexToData(),
                aeHostCommitment: input.aeHostCommitment?.hexToData())
        }

        if txInputs.contains(where: { $0 == nil }) {
            return Observable.error(HWError.Abort("Input transactions missing"))
        }

        let changes = getChangeData(outputs: params.txOutputs)
        let signtx = JadeSignTx(change: changes,
                                network: network,
                                numInputs: params.signingInputs.count,
                                trustedCommitments: nil,
                                useAeProtocol: params.useAeProtocol,
                                txn: params.transaction?.transaction?.hexToData() ?? Data())
        return exchange(JadeRequest(method: "sign_tx", params: signtx))
            .flatMap { (_ : JadeResponse<Bool>) -> Observable<(commitments: [String], signatures: [String])> in
                if params.useAeProtocol {
                    return self.signTxInputsAntiExfil(inputs: txInputs)
                } else {
                    return self.signTxInputs(inputs: txInputs)
                }
            }.compactMap { (commitments, signatures) in
                return HWSignTxResponse(signatures: signatures, signerCommitments: commitments)
            }
    }

    func signTxInputsAntiExfil(inputs: [TxInputProtocol?]) -> Observable<(commitments: [String], signatures: [String])> {
        /**
         * Anti-exfil protocol:
         * We send one message per input (which includes host-commitment *but
         * not* the host entropy) and receive the signer-commitment in reply.
         * Once all n input messages are sent, we can request the actual signatures
         * (as the user has a chance to confirm/cancel at this point).
         * We request the signatures passing the host-entropy for each one.
         */
        // Send inputs one at a time, receiving 'signer-commitment' in reply
        let signerCommitments = inputs.map {
            Observable.just($0!)
                .flatMap { inp -> Observable<JadeResponse<Data>> in
                    if let inputBtc = inp as? TxInputBtc {
                        return self.exchange(JadeRequest(method: "tx_input", params: inputBtc))
                    } else if let inputLiquid = inp as? TxInputLiquid {
                        return self.exchange(JadeRequest(method: "tx_input", params: inputLiquid))
                    } else {
                        return Observable.error(HWError.Abort(""))
                    }
                }.compactMap { (res: JadeResponse<Data>) -> String in
                    return dataToHex(res.result!)
                }
        }
        let signatures = inputs.map {
            Observable.just($0!)
                .compactMap { input -> Data? in
                    if let inputBtc = input as? TxInputBtc {
                        return inputBtc.aeHostEntropy
                    } else if let inputLiquid = input as? TxInputLiquid {
                        return inputLiquid.aeHostEntropy
                    } else {
                        return nil
                    }
                }.flatMap { aeHostEntropy in
                    self.exchange(JadeRequest(method: "get_signature", params: JadeGetSignature(aeHostEntropy: aeHostEntropy!)))
                }.compactMap { (res: JadeResponse<Data>) -> String in
                    return dataToHex(res.result!)
                }
        }

        var commitments = [String]()
        return Observable.concat(signerCommitments)
            .reduce([], accumulator: { result, element in
                result + [element]
            }).compactMap { signerCommitments in
                commitments = signerCommitments
            }.flatMap { _ in
                Observable.concat(signatures)
            }.reduce([], accumulator: { result, element in
                result + [element]
            }).compactMap { signatures in
                return (commitments: commitments, signatures: signatures)
            }
    }

    func signTxInputs(inputs: [TxInputProtocol?]) -> Observable<(commitments: [String], signatures: [String])> {
        /**
         * Legacy Protocol:
         * Send one message per input - without expecting replies.
         * Once all n input messages are sent, the hw then sends all n replies
         * (as the user has a chance to confirm/cancel at this point).
         * Then receive all n replies for the n signatures.
         * NOTE: *NOT* a sequence of n blocking rpc calls.
         */
        let allWrites = inputs.map {
                Observable.just($0!)
                    .flatMap { self.signTxInput($0) }
                    .asObservable()
        }
        let allReads = inputs.map {
                Observable.just($0!)
                    .flatMap { _ in Jade.shared.read() }
                    .compactMap { buffer in
                        #if DEBUG
                        print("<= " + buffer.map { String(format: "%02hhx", $0) }.joined())
                        #endif
                        return try CodableCBORDecoder().decode(JadeResponse<Data>.self, from: buffer)
                    }.flatMap { (res: JadeResponse<Data>) -> Observable<Data> in
                        return Observable<Data>.create { observer in
                            if let result = res.result {
                                observer.onNext(result)
                                observer.onCompleted()
                            } else if let error = res.error {
                                observer.onError(HWError.Declined(error.message))
                            } else {
                                observer.onError(HWError.Declined(""))
                            }
                            return Disposables.create { }
                        }
                    }.compactMap { $0.map { String(format: "%02x", $0) }.joined() }
                    .asObservable()
        }
        return Observable.concat(allWrites)
            .reduce([], accumulator: { result, element in
                result + [element]
            }).flatMap { _ in
                Observable.concat(allReads)
            }.reduce([], accumulator: { result, element in
                result + [element]
            }).compactMap { signatures in
                return (commitments: [], signatures: signatures)
            }
    }

    func signTxInput(_ input: TxInputProtocol) -> Observable<Data> {
        return Observable.create { observer in
            var encoded: Data?
            if let inputBtc = input as? TxInputBtc {
                let request = JadeRequest<TxInputBtc>(method: "tx_input", params: inputBtc)
                encoded = request.encoded
            } else if let inputLiquid = input as? TxInputLiquid {
                let request = JadeRequest<TxInputLiquid>(method: "tx_input", params: inputLiquid)
                encoded = request.encoded
            }
            #if DEBUG
            print("=> " + encoded!.map { String(format: "%02hhx", $0) }.joined())
            #endif
            return self.write(encoded!)
                .subscribe(onNext: { _ in
                    observer.onNext(Data())
                    observer.onCompleted()
                }, onError: { err in
                    observer.onError(err)
                })
        }
    }

    func isSegwit(_ addrType: String?) -> Bool {
        return ["csv", "p2wsh", "p2wpkh", "p2sh-p2wpkh"].contains(addrType)
    }

    func mapAddressType(_ addrType: String?) -> String? {
        switch addrType {
        case "p2pkh": return "pkh(k)"
        case "p2wpkh": return "wpkh(k)"
        case "p2sh-p2wpkh": return "sh(wpkh(k))"
        default: return nil
        }
    }

    // Helper to get the change paths for auto-validation
    func getChangeData(outputs: [InputOutput]) -> [TxChangeOutput?] {
        return outputs.map { (out: InputOutput) -> TxChangeOutput? in
            if out.isChange == false {
                return nil
            }
            var csvBlock: UInt32 = 0
            if out.addressType == "csv" {
                csvBlock = out.subtype ?? 0
            }
            return TxChangeOutput(path: out.userPath ?? [],
                                  recoveryxpub: out.recoveryXpub,
                                  csvBlocks: csvBlock,
                                  variant: mapAddressType(out.addressType))
        }
    }

    // swiftlint:disable:next function_parameter_count
    public func newReceiveAddress(chain: String,
                                  mainnet: Bool,
                                  multisig: Bool,
                                  chaincode: String?,
                                  recoveryPubKey: String?,
                                  walletPointer: UInt32?,
                                  walletType: String?,
                                  path: [UInt32],
                                  csvBlocks: UInt32) -> Observable<String> {
        if multisig {
            // Green Multisig Shield - pathlen should be 2 for subact 0, and 4 for subact > 0
            // In any case the last two entries are 'branch' and 'pointer'
            let pathlen = path.count
            let branch = path[pathlen - 2]
            let pointer = path[pathlen - 1]
            var recoveryxpub: String?
            if let chaincode = chaincode, !chaincode.isEmpty {
                recoveryxpub = try? bip32KeyFromParentToBase58(isMainnet: mainnet,
                                                               pubKey: [UInt8](recoveryPubKey?.hexToData() ?? Data()),
                                                               chainCode: [UInt8](chaincode.hexToData()),
                                                               branch: branch)
            }
            // Get receive address from Jade for the path elements given
            let params = JadeGetReceiveMultisigAddress(network: chain,
                                                    pointer: pointer ,
                                                             subaccount: walletPointer ?? 0,
                                                             branch: branch,
                                                             recoveryXpub: recoveryxpub,
                                                             csvBlocks: csvBlocks)
            return getReceiveAddress(params)
        } else {
            // Green Electrum Singlesig
            let variant = mapAddressType(walletType)
            let params = JadeGetReceiveSinglesigAddress(network: chain, path: path, variant: variant ?? "")
            return getReceiveAddress(params)
        }
    }

}
// Liquid calls
extension Jade {

    public func signLiquidTransaction(network: String,
                                      params: HWSignTxParams) -> Observable<HWSignTxResponse> {
        version().flatMap { self.signLiquidTransaction_(network: network, version: $0, params: params) }
    }
    
    private func signLiquidTransaction_(network: String,
                                        version: JadeVersionInfo,
                                        params: HWSignTxParams) -> Observable<HWSignTxResponse> {
        // Load the tx into wally for legacy fw versions as will need it later
        // to access the output's asset[generator] and value[commitment].
        // NOTE: 0.1.48+ Jade fw does need these extra values passed explicitly so
        // no need to parse/load the transaction into wally.
        // FIXME: remove when 0.1.48 is made minimum allowed version.
        let wallytx = !version.hasSwapSupport ? wallyTxFromBytes(tx: params.transaction?.transaction?.hexToBytes() ?? []) : nil
        let txInputs = params.signingInputs
            .map { (txInput: InputOutput) -> TxInputLiquid in
            return TxInputLiquid(isWitness: txInput.isSegwit,
                                 script: txInput.prevoutScript?.hexToData(),
                                 valueCommitment: txInput.commitment?.hexToData(),
                                 path: txInput.userPath,
                                 aeHostEntropy: txInput.aeHostEntropy?.hexToData(),
                                 aeHostCommitment: txInput.aeHostCommitment?.hexToData())
        }
        // Get blinding factors and unblinding data per output - null for unblinded outputs
        // Assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
        let trustedCommitments = params.txOutputs.enumerated().map { res -> Commitment? in
            let out = res.element
            // Add a 'null' commitment for unblinded output
            guard out.blindingKey != nil else { return nil }
            var commitment = Commitment(assetId: out.getAssetIdBytes?.data,
                       value: out.satoshi,
                       abf: out.getAbfs?.data,
                       vbf: out.getVbfs?.data,
                       assetGenerator: nil,
                       valueCommitment: nil,
                       blindingKey: out.getPublicKeyBytes?.data)
            // Add asset-generator and value-commitment for legacy fw versions
            // NOTE: 0.1.48+ Jade fw does need these extra values passed explicitly
            if let wallytx = wallytx, let asset = wallyTxGetOutputAsset(wallyTx: wallytx, index: res.offset) {
                commitment.assetGenerator = asset.data
            }
            if let wallytx = wallytx, let value = wallyTxGetOutputValue(wallyTx: wallytx, index: res.offset) {
                commitment.valueCommitment = value.data
            }
            return commitment
        }
        // Get the change outputs and paths
        let change = getChangeData(outputs: params.txOutputs)
        // Make jade-api call to sign the txn
        let params = JadeSignTx(change: change,
                                network: network,
                                numInputs: txInputs.count,
                                trustedCommitments: trustedCommitments,
                                useAeProtocol: params.useAeProtocol,
                                txn: params.transaction?.transaction?.hexToData() ?? Data())
        return signLiquidTx(params: params)
            .flatMap { _ -> Observable<(commitments: [String], signatures: [String])> in
                if params.useAeProtocol {
                    return self.signTxInputsAntiExfil(inputs: txInputs)
                } else {
                    return self.signTxInputs(inputs: txInputs)
                }
            }
            .compactMap { (commitments: [String], signatures: [String]) in
                HWSignTxResponse(
                    signatures: signatures,
                    signerCommitments: commitments)
            }
    }

    public func getBlindingFactors(params: HWBlindingFactorsParams) -> Observable<HWBlindingFactorsResult> {
        version().flatMap { self.getBlindingFactors_(params: params, version: $0) }
    }

    private func getBlindingFactors_(params: HWBlindingFactorsParams, version: JadeVersionInfo) -> Single<HWBlindingFactorsResult> {
        // Compute hashPrevouts to derive deterministic blinding factors from
        let txhashes = params.usedUtxos.map { $0.getTxid ?? []}.lazy.joined()
        let outputIdxs = params.usedUtxos.map { $0.ptIdx }
        let hashPrevouts = getHashPrevouts(txhashes: [UInt8](txhashes), outputIdxs: outputIdxs)
        // Enumerate the outputs and provide blinding factors as needed
        // Assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
        return Observable.from(params.transactionOutputs)
            .enumerated()
            .concatMap() { i, output -> Observable<(String, String)> in
                // Call Jade to get the blinding factors
                // NOTE: 0.1.48+ Jade fw accepts 'ASSET_AND_VALUE', and returns abf and vbf concatenated abf||vbf
                // (Previous versions need two calls, for 'ASSET' and 'VALUE' separately)
                // FIXME: remove when 0.1.48 is made minimum allowed version.
                if output.blindingKey == nil {
                    return Observable.just(("", ""))
                } else if version.hasSwapSupport {
                    return self.getBlindingFactor(JadeGetBlingingFactor(hashPrevouts: hashPrevouts?.data, outputIndex: i, type: "ASSET_AND_VALUE"))
                        .compactMap { bfs in
                            let assetblinder = bfs[0..<WALLY_BLINDING_FACTOR_LEN].reversed()
                            let amountblinder = bfs[WALLY_BLINDING_FACTOR_LEN..<2*WALLY_BLINDING_FACTOR_LEN].reversed()
                            return (Data(assetblinder).hex, Data(amountblinder).hex)
                        }
                } else {
                    var abf = Data()
                    return self.getBlindingFactor(JadeGetBlingingFactor(hashPrevouts: hashPrevouts?.data, outputIndex: i, type: "ASSET"))
                        .compactMap { abf = $0 }
                        .flatMap { self.getBlindingFactor(JadeGetBlingingFactor(hashPrevouts: hashPrevouts?.data, outputIndex: i, type: "VALUE")) }
                        .compactMap { vbf in
                            return (Data(abf.reversed()).hex, Data(vbf.reversed()).hex)
                        }
                }
            }
            .toArray()
            .flatMap { res -> Single<HWBlindingFactorsResult> in
                let bfr = HWBlindingFactorsResult(assetblinders: res.map { $0.0 },
                                                amountblinders: res.map { $0.1 })
                return Single.just(bfr)
            }
    }
}
