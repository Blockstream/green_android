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

    public func version() -> Observable<JadeVersionInfo> {
        return exchange(JadeRequest<JadeEmpty>(method: "get_version_info"))
            .compactMap { (res: JadeResponse<JadeVersionInfo>) -> JadeVersionInfo in
                res.result!
            }
    }

    public func addEntropy() -> Observable<Bool> {
        let buffer = [UInt8](repeating: 0, count: 32).map { _ in UInt8(arc4random_uniform(0xff))}
        let cmd = JadeAddEntropy(entropy: Data(buffer))
        return exchange(JadeRequest<JadeAddEntropy>(method: "add_entropy", params: cmd))
            .compactMap { (res: JadeResponse<Bool>) -> Bool in
                res.result!
            }
    }

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

    public func signMessage(path: [Int]?,
                     message: String?,
                     useAeProtocol: Bool?,
                     aeHostCommitment: String?,
                     aeHostEntropy: String?)
    -> Observable<(signature: String?, signerCommitment: String?)> {
        let pathstr = path?.map { UInt32($0) }
        var signMessage: Observable<(String?, String?)>?
        if useAeProtocol ?? false {
            // Anti-exfil protocol:
            // We send the signing request with the host-commitment and receive the signer-commitment
            // in reply once the user confirms.
            // We can then request the actual signature passing the host-entropy.
            let aeHostCommitment = hexToData(aeHostCommitment ?? "")
            let aeHostEntropy = hexToData(aeHostEntropy ?? "")
            var signerCommitment: String?
            let cmd = JadeSignMessage(message: message ?? "", path: pathstr ?? [], aeHostCommitment: aeHostCommitment)
            signMessage = exchange(JadeRequest<JadeSignMessage>(method: "sign_message", params: cmd))
                .compactMap { (res: JadeResponse<Data>) in
                    signerCommitment = res.result?.map { String(format: "%02hhx", $0) }.joined()
                }.flatMap { _ -> Observable<JadeResponse<String>> in
                    let cmd = JadeGetSignature(aeHostEntropy: aeHostEntropy)
                    return self.exchange(JadeRequest<JadeGetSignature>(method: "get_signature", params: cmd))
                }.compactMap { (res: JadeResponse<String>) -> (String?, String?) in
                    return (res.result!, signerCommitment)
                }
        } else {
            // Standard EC signature, simple case
            let cmd = JadeSignMessage(message: message ?? "", path: pathstr ?? [], aeHostCommitment: nil)
            signMessage = exchange(JadeRequest<JadeSignMessage>(method: "sign_message", params: cmd))
                .compactMap { (res: JadeResponse<String>) -> (String?, String?) in
                    return (res.result!, "")
                }
        }
        return signMessage!.compactMap { (sign, signerCom) -> (signature: String?, signerCommitment: String?) in
            // Convert the signature from Base64 into DER hex for GDK
            guard var sigDecoded = Data(base64Encoded: sign ?? "") else {
                throw HWError.Abort("Invalid signature")
            }

            // Need to truncate lead byte if recoverable signature
            if sigDecoded.count == WALLY_EC_SIGNATURE_RECOVERABLE_LEN {
                sigDecoded = sigDecoded[1...sigDecoded.count-1]
            }

            let sigDer = try sigToDer(sig: Array(sigDecoded))
            let hexSig = sigDer.map { String(format: "%02hhx", $0) }.joined()
            return (signature: hexSig, signerCommitment: signerCom)
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

    public func xpubs(network: String, path: [Int]) -> Observable<String> {
        let pathstr: [UInt32] = path.map { UInt32($0) }
        let cmd = JadeGetXpub(network: network, path: pathstr)
        return exchange(JadeRequest<JadeGetXpub>(method: "get_xpub", params: cmd))
            .compactMap { (res: JadeResponse<String>) -> String in
                res.result!
            }
    }

    // swiftlint:disable:next function_parameter_count
    public func signTransaction(network: String, tx: AuthTx, inputs: [AuthTxInput], outputs: [AuthTxOutput], transactions: [String: String], useAeProtocol: Bool) -> Observable<AuthSignTransactionResponse> {

        if transactions.isEmpty {
            return Observable.error(HWError.Abort("Input transactions missing"))
        }

        let txInputs = inputs.map { input -> TxInputBtc? in
            var txhash: String? = input.txhash
            var satoshi: UInt64? = input.satoshi
            if isSegwit(input.addressType) && inputs.count == 1 {
                txhash = nil
            } else if let hash = txhash, let tx = transactions[hash] {
                satoshi = nil
                txhash = tx
            } else {
                return nil
            }
            return TxInputBtc(
                isWitness: isSegwit(input.addressType),
                inputTxHex: txhash,
                scriptHex: input.prevoutScript,
                satoshi: satoshi,
                path: input.userPath,
                aeHostEntropyHex: input.aeHostEntropy,
                aeHostCommitmentHex: input.aeHostCommitment)
        }

        if txInputs.contains(where: { $0 == nil }) {
            return Observable.error(HWError.Abort("Input transactions missing"))
        }

        let changes = getChangeData(outputs: outputs)
        let txn = hexToData(tx.transaction)

        let signtx = JadeSignTx(change: changes,
                                network: network,
                                numInputs: inputs.count,
                                trustedCommitments: nil,
                                useAeProtocol: useAeProtocol,
                                txn: txn)
        return exchange(JadeRequest(method: "sign_tx", params: signtx))
            .flatMap { (_ : JadeResponse<Bool>) -> Observable<(commitments: [String], signatures: [String])> in
                if useAeProtocol {
                    return self.signTxInputsAntiExfil(inputs: txInputs)
                } else {
                    return self.signTxInputs(inputs: txInputs)
                }
            }.compactMap { (commitments, signatures) in
                return AuthSignTransactionResponse(signatures: signatures, signerCommitments: commitments)
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
                        return Data(inputBtc.aeHostEntropy!)
                    } else if let inputLiquid = input as? TxInputLiquid {
                        return Data(inputLiquid.aeHostEntropy!)
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
    func getChangeData(outputs: [AuthTxOutput]) -> [TxChangeOutput?] {
        return outputs.map { out -> TxChangeOutput? in
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
            let cmd = JadeGetReceiveMultisigAddress(network: chain,
                                                    pointer: pointer ,
                                                             subaccount: walletPointer ?? 0,
                                                             branch: branch,
                                                             recoveryXpub: recoveryxpub,
                                                             csvBlocks: csvBlocks)
            return exchange(JadeRequest<JadeGetReceiveMultisigAddress>(method: "get_receive_address", params: cmd))
                .compactMap { (res: JadeResponse<String>) -> String in
                    res.result!
                }
        } else {
            // Green Electrum Singlesig
            let variant = mapAddressType(walletType)
            let cmd = JadeGetReceiveSinglesigAddress(network: chain, path: path, variant: variant ?? "")
            return exchange(JadeRequest<JadeGetReceiveSinglesigAddress>(method: "get_receive_address", params: cmd))
                .compactMap { (res: JadeResponse<String>) -> String in
                    res.result!
                }
        }
    }

}
// Liquid calls
extension Jade {

    // Get blinding key for script
    public func getBlindingKey(scriptHex: String) -> Observable<String?> {
        return exchange(JadeRequest(method: "get_blinding_key", params: JadeGetBlindingKey(scriptHex: scriptHex)))
            .compactMap { (res: JadeResponse<Data>) -> String? in
                return res.result?.map { String(format: "%02hhx", $0) }.joined()
            }
    }

    public func getSharedNonce(pubkey: String, scriptHex: String) -> Observable<String?> {
        return exchange(JadeRequest(method: "get_shared_nonce", params: JadeGetSharedNonce(scriptHex: scriptHex, theirPubkeyHex: pubkey)))
            .compactMap { (res: JadeResponse<Data>) -> String? in
                return res.result?.map { String(format: "%02hhx", $0) }.joined()
            }
    }

    func getBlindingFactor(hashPrevouts: Data, outputIdx: Int, type: String) -> Observable<Data?> {
        let cmd = JadeGetBlingingFactor(hashPrevouts: hashPrevouts, outputIndex: outputIdx, type: type)
        return exchange(JadeRequest(method: "get_blinding_factor", params: cmd))
            .compactMap { (res: JadeResponse<Data>) -> Data? in
                return res.result
            }
    }

    // swiftlint:disable:next function_parameter_count
    public func signLiquidTransaction(network: String, tx: AuthTx, inputs: [AuthTxInput], outputs: [AuthTxOutput], transactions: [String: String], useAeProtocol: Bool) -> Observable<AuthSignTransactionResponse> {
        let txInputs = inputs.map { input -> TxInputLiquid? in
            return TxInputLiquid(
                isWitness: !(input.addressType == "p2sh"),
                scriptHex: input.prevoutScript,
                valueCommitmentHex: input.commitment,
                path: input.userPath,
                aeHostEntropyHex: input.aeHostEntropy,
                aeHostCommitmentHex: input.aeHostCommitment)
        }

        // Get values, abfs and vbfs from inputs (needed to compute the final output vbf)
        var values = inputs.map { $0.satoshi }
        var abfs = inputs.map { $0.assetblinderHex }
        var vbfs = inputs.map { $0.amountblinderHex }

        var inputPrevouts = [Data]()
        inputs.forEach { input in
            inputPrevouts += [Data(input.txhashHex)]
            inputPrevouts += [Data(input.ptIdxHex)]
        }

        // Compute the hash of all input prevouts for making deterministic blinding factors
        let prevOuts = flatten(inputPrevouts.map { [UInt8]($0) }, fixedSize: nil)
        let hashPrevOuts = Data(try! sha256d(prevOuts))

        // Get trusted commitments per output - null for unblinded outputs
        var trustedCommitments = [Commitment?]()

        // For all-but-last blinded entry, do not pass custom vbf, so one is generated
        // Append the output abfs and vbfs to the arrays
        // assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
        let lastBlindedIndex = outputs.count - 2;  // Could determine this properly
        var obsCommitments = [Observable<Commitment>]()
        let lastBlindedOutput = outputs[lastBlindedIndex]
        for (i, output) in outputs.enumerated() where i < lastBlindedIndex {
            values.append(output.satoshi ?? 0)
            obsCommitments.append( Observable.just(output).flatMap {
                self.getTrustedCommitment(index: i, output: $0, hashPrevOuts: hashPrevOuts, customVbf: nil)
            })
        }
        return Observable.concat(obsCommitments)
        .reduce([], accumulator: { _, commitment in
            trustedCommitments.append(commitment)
            abfs.append([UInt8](commitment.abf))
            vbfs.append([UInt8](commitment.vbf))
            return []
        }).flatMap { _ -> Observable<Data?> in
            // For the last blinded output, get the abf only
            values.append(lastBlindedOutput.satoshi ?? 0)
            return self.getBlindingFactor(hashPrevouts: hashPrevOuts, outputIdx: lastBlindedIndex, type: "ASSET")
        }.flatMap { lastAbf -> Observable<Commitment> in
            abfs.append([UInt8](lastAbf!))
            // For the last blinded output we need to calculate the correct vbf so everything adds up
            let flattenAbfs = flatten(abfs, fixedSize: WALLY_BLINDING_FACTOR_LEN)
            let flattenVbfs = flatten(vbfs, fixedSize: WALLY_BLINDING_FACTOR_LEN)
            let lastVbf = try asset_final_vbf(values: values, numInputs: inputs.count, abf: flattenAbfs, vbf: flattenVbfs)
            vbfs.append(lastVbf)
            // Fetch the last commitment using that explicit vbf
            return self.getTrustedCommitment(index: lastBlindedIndex, output: lastBlindedOutput, hashPrevOuts: hashPrevOuts, customVbf: Data(lastVbf))
        }.flatMap { lastCommitment -> Observable<(commitments: [String], signatures: [String])> in
            trustedCommitments.append(lastCommitment)
            // Add a 'null' commitment for the final (fee) output
            trustedCommitments.append(nil)
            // Get the change outputs and paths
            let change = self.getChangeData(outputs: outputs)
            // Make jade-api call to sign the txn
            let txn = hexToData(tx.transaction )
            return self.signLiquidTx(network: network, txn: txn, inputs: txInputs, trustedCommitments: trustedCommitments, changes: change, useAeProtocol: useAeProtocol)
        }.compactMap { (commitments: [String], signatures: [String]) in
            let assetGenerators = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.assetGenerator)) : nil) }
            let valueCommitments = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.valueCommitment)) : nil) }
            let abfs = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.abf.reversed())) : nil) }
            let vbfs = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.vbf.reversed())) : nil) }
            return AuthSignTransactionResponse(
                signatures: signatures,
                signerCommitments: commitments,
                assetCommitments: assetGenerators,
                valueCommitments: valueCommitments,
                assetblinders: abfs,
                amountblinders: vbfs)
        }
    }

    // swiftlint:disable:next function_parameter_count
    func signLiquidTx(network: String, txn: Data, inputs: [TxInputLiquid?], trustedCommitments: [Commitment?], changes: [TxChangeOutput?], useAeProtocol: Bool) -> Observable<(commitments: [String], signatures: [String])> {
        let cmd =
        JadeSignTx(change: changes, network: network, numInputs: inputs.count, trustedCommitments: trustedCommitments, useAeProtocol: useAeProtocol, txn: txn)
        return exchange(JadeRequest(method: "sign_liquid_tx", params: cmd))
            .flatMap { (res: JadeResponse<Bool>) -> Observable<(commitments: [String], signatures: [String])> in
                if let result = res.result, !result {
                    throw HWError.Abort("Error response from initial sign_liquid_tx call: \(res.error?.message ?? "")")
                }
                if useAeProtocol {
                    return self.signTxInputsAntiExfil(inputs: inputs)
                } else {
                    return self.signTxInputs(inputs: inputs)
                }
            }
    }

    // Helper to get the commitment and blinding key from Jade
    func getTrustedCommitment(index: Int, output: AuthTxOutput, hashPrevOuts: Data, customVbf: Data?) -> Observable<Commitment> {
        let package = JadeGetCommitment(hashPrevouts: hashPrevOuts,
                                        outputIdx: index,
                                        assetId: hexToData(output.asset_id ?? ""),
                                        value: output.satoshi ?? 0,
                                        vbf: customVbf)
        return Jade.shared.exchange(JadeRequest(method: "get_commitments", params: package))
            .compactMap { (res: JadeResponse<Commitment>) in
                // Add the script blinding key
                var comm = res.result
                comm?.blindingKey = (output.blindingKey ?? "").hexToData()
                return comm!
            }
    }

    public func getMasterBlindingKey() -> Observable<String> {
        return exchange(JadeRequest<JadeEmpty>(method: "get_master_blinding_key", params: nil))
            .compactMap { (res: JadeResponse<Data>) -> String in
                dataToHex(res.result!)
            }
    }
}
