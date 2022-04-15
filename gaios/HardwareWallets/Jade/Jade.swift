import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth
import ga.wally
import SwiftCBOR

final class Jade: JadeChannel, HWProtocol {

    public static let shared = Jade()
    let SIGHASH_ALL: UInt8 = 1

    func version() -> Observable<JadeVersionInfo> {
        return exchange(JadeRequest<JadeEmpty>(method: "get_version_info"))
            .compactMap { (res: JadeResponse<JadeVersionInfo>) -> JadeVersionInfo in
                res.result!
            }
    }

    func addEntropy() -> Observable<Bool> {
        let buffer = [UInt8](repeating: 0, count: 32).map { _ in UInt8(arc4random_uniform(0xff))}
        let cmd = JadeAddEntropy(entropy: Data(buffer))
        return exchange(JadeRequest<JadeAddEntropy>(method: "add_entropy", params: cmd))
            .compactMap { (res: JadeResponse<Bool>) -> Bool in
                res.result!
            }
    }
    func httpRequest<T: Codable, K: Codable>(_ httpRequest: JadeHttpRequest<T>) -> K {
        let encoded = try? JSONEncoder().encode(httpRequest.params)
        let serialized = try? JSONSerialization.jsonObject(with: encoded!, options: .allowFragments) as? [String: Any]
        let httpResponse = try? SessionsManager.current?.httpRequest(params: serialized ?? [:])
        let httpResponseBody = httpResponse?["body"] as? [String: Any]
        let deserialized = try? JSONSerialization.data(withJSONObject: httpResponseBody ?? [:], options: .fragmentsAllowed)
        return try! JSONDecoder().decode(K.self, from: deserialized!)
    }

    func auth(network: String) -> Observable<Bool> {
        // Send initial auth user request
        let cmd = JadeAuthRequest(network: network)
        return exchange(JadeRequest<JadeAuthRequest>(method: "auth_user", params: cmd))
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { (res: JadeResponse<JadeAuthResponse<String>>) -> Observable<JadeResponse<JadeAuthResponse<JadeHandshakeComplete>>> in
                let request: JadeHttpRequest<String> = res.result!.httpRequest
                let cmd: JadeHandshakeInit = self.httpRequest(request)
                let jadePackage = JadeRequest<JadeHandshakeInit>(method: res.result?.httpRequest.onReply ?? "",
                                                                params: cmd)
                return self.exchange(jadePackage)
            }.flatMap { (res: JadeResponse<JadeAuthResponse<JadeHandshakeComplete>>) -> Observable<JadeResponse<Bool>> in
                let request: JadeHttpRequest<JadeHandshakeComplete> = res.result!.httpRequest
                let package: JadeHandshakeCompleteReply = self.httpRequest(request)
                let jadePackage = JadeRequest<JadeHandshakeCompleteReply>(method: res.result?.httpRequest.onReply ?? "",
                                                                        params: package)
                return self.exchange(jadePackage)
            }.flatMap { (res: JadeResponse<Bool>) -> Observable<Bool> in
                return Observable.just(res.result ?? true)
            }
    }

    func signMessage(path: [Int]?,
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
                .compactMap { (res: JadeResponse<Data>) -> (String?, String?) in
                    let signerCommitment = res.result?.map { String(format: "%02hhx", $0) }.joined()
                    return ("", signerCommitment)
                }
        }
        return signMessage!.compactMap { (sign, signerCom) -> (signature: String?, signerCommitment: String?) in
            // Convert the signature from Base64 into DER hex for GDK
            guard var sigDecoded = Data(base64Encoded: sign ?? "") else {
                throw JadeError.Abort("Invalid signature")
            }

            // Need to truncate lead byte if recoverable signature
            if sigDecoded.count == EC_SIGNATURE_RECOVERABLE_LEN {
                sigDecoded = sigDecoded[1...sigDecoded.count]
            }

            let sigDer = try sigToDer(sig: Array(sigDecoded))
            let hexSig = sigDer.map { String(format: "%02hhx", $0) }.joined()
            return (signature: hexSig, signerCommitment: signerCom)
        }
    }

    func xpubs(network: String, paths: [[Int]]) -> Observable<[String]> {
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

    func xpubs(network: String, path: [Int]) -> Observable<String> {
        let pathstr: [UInt32] = path.map { UInt32($0) }
        let cmd = JadeGetXpub(network: network, path: pathstr)
        return exchange(JadeRequest<JadeGetXpub>(method: "get_xpub", params: cmd))
            .compactMap { (res: JadeResponse<String>) -> String in
                res.result!
            }
    }

    // swiftlint:disable:next function_parameter_count
    func signTransaction(network: String, tx: [String: Any], inputs: [[String: Any]], outputs: [[String: Any]], transactions: [String: String], useAeProtocol: Bool) -> Observable<[String: Any]> {

        if transactions.isEmpty {
            return Observable.error(JadeError.Abort("Input transactions missing"))
        }

        let txInputs = inputs.map { input -> TxInputBtc? in
            let addressType = input["address_type"] as? String
            let swInput = isSegwit(addressType)
            let prevoutScript = input["prevout_script"] as? String
            let userPath = input["user_path"] as? [UInt32]
            let aeHostCommitment = input["ae_host_commitment"] as? String
            let aeHostEntropy = input["ae_host_entropy"] as? String
            var satoshi = input["satoshi"] as? UInt64
            var txhash = input["txhash"] as? String
            if swInput && inputs.count == 1 {
                txhash = nil
            } else if let hash = txhash, let tx = transactions[hash] {
                satoshi = nil
                txhash = tx
            } else {
                return nil
            }
            return TxInputBtc(isWitness: swInput, inputTxHex: txhash, scriptHex: prevoutScript, satoshi: satoshi, path: userPath, aeHostEntropyHex: aeHostEntropy, aeHostCommitmentHex: aeHostCommitment)
        }

        if txInputs.contains(where: { $0 == nil }) {
            return Observable.error(JadeError.Abort("Input transactions missing"))
        }

        let changes = getChangeData(outputs: outputs)
        let txhex = tx["transaction"] as? String
        let txn = hexToData(txhex ?? "")

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
                return ["signatures": signatures, "signer_commitments": commitments]
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
                        return Observable.error(JadeError.Abort(""))
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
                        return try CodableCBORDecoder().decode(JadeResponse<[UInt8]>.self, from: buffer)
                    }.flatMap { (res: JadeResponse<[UInt8]>) -> Observable<[UInt8]> in
                        return Observable<[UInt8]>.create { observer in
                            if let result = res.result {
                                observer.onNext(result)
                                observer.onCompleted()
                            } else if let error = res.error {
                                observer.onError(JadeError.Declined(error.message))
                            } else {
                                observer.onError(JadeError.Declined(""))
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
    func getChangeData(outputs: [[String: Any]]) -> [TxChangeOutput?] {
        return outputs.map { output -> TxChangeOutput? in
            let isChange = output["is_change"] as? Bool
            if isChange == false {
                return nil
            }
            var csvBlock = 0
            let addressType = output["address_type"] as? String
            if addressType == "csv" {
                csvBlock = output["subtype"] as? Int  ?? 0
            }
            return TxChangeOutput(path: output["user_path"] as? [UInt32] ?? [],
                                  recoveryxpub: output["recovery_xpub"] as? String,
                                  csvBlocks: csvBlock,
                                  variant: mapAddressType(addressType))
        }
    }

    func inputBytes(_ input: [String: Any], isSegwit: Bool) -> Data? {
        let txHashHex = input["txhash"] as? String
        let ptIdx = input["pt_idx"] as? UInt
        let txId: [UInt8]? = hexToData(txHashHex!).reversed()
        return Data(txId! + ptIdx!.uint32LE() + (isSegwit ? (input["satoshi"] as? UInt64)!.uint64LE() : []))
    }

    func newReceiveAddress(network: GdkNetwork, wallet: WalletItem, path: [UInt32], csvBlocks: UInt32) -> Observable<String> {
        if network.multisig {
            // Green Multisig Shield - pathlen should be 2 for subact 0, and 4 for subact > 0
            // In any case the last two entries are 'branch' and 'pointer'
            let pathlen = path.count
            let branch = path[pathlen - 2]
            let pointer = path[pathlen - 1]
            var recoveryxpub: String?
            if let chaincode = wallet.recoveryChainCode, !chaincode.isEmpty {
                recoveryxpub = try? bip32KeyFromParentToBase58(isMainnet: !network.mainnet,
                                                               pubKey: [UInt8](hexToData(wallet.recoveryPubKey!)),
                                                               chainCode: [UInt8](hexToData(chaincode)),
                                                               branch: branch)
            }
            // Get receive address from Jade for the path elements given
            let cmd = JadeGetReceiveMultisigAddress(network: network.chain,
                                                             pointer: pointer,
                                                             subaccount: wallet.pointer,
                                                             branch: branch,
                                                             recoveryXpub: recoveryxpub,
                                                             csvBlocks: csvBlocks)
            return exchange(JadeRequest<JadeGetReceiveMultisigAddress>(method: "get_receive_address", params: cmd))
                .compactMap { (res: JadeResponse<String>) -> String in
                    res.result!
                }
        } else {
            // Green Electrum Singlesig
            let variant = mapAddressType(wallet.type)
            let cmd = JadeGetReceiveSinglesigAddress(network: network.chain, path: path, variant: variant ?? "")
            return exchange(JadeRequest<JadeGetReceiveSinglesigAddress>(method: "get_receive_address", params: cmd))
                .compactMap { (res: JadeResponse<String>) -> String in
                    res.result!
                }
        }
    }

}
// Jade ota
extension Jade {

    static let MIN_ALLOWED_FW_VERSION = "0.1.24"
    static let FW_VERSIONS_FILE = "LATEST"
    static let FW_SERVER_HTTPS = "https://jadefw.blockstream.com"
    static let FW_SERVER_ONION = "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion"
    static let FW_JADE_PATH = "/bin/jade/"
    static let FW_JADEDEV_PATH = "/bin/jadedev/"
    static let FW_JADE1_1_PATH = "/bin/jade1.1/"
    static let FW_JADE1_1DEV_PATH = "/bin/jade1.1dev/"
    static let FW_SUFFIX = "fw.bin"
    static let BOARD_TYPE_JADE = "JADE"
    static let BOARD_TYPE_JADE_V1_1 = "JADE_V1.1"
    static let FEATURE_SECURE_BOOT = "SB"

    // Check Jade fmw against minimum allowed firmware version
    func isJadeFwValid(_ version: String) -> Bool {
        return Jade.MIN_ALLOWED_FW_VERSION <= version
    }

    func firmwarePath(_ info: JadeVersionInfo) -> String? {
        if ![Jade.BOARD_TYPE_JADE, Jade.BOARD_TYPE_JADE_V1_1].contains(info.boardType) {
            return nil
        }
        let isV1BoardType = info.boardType == Jade.BOARD_TYPE_JADE
        // Alas the first version of the jade fmw didn't have 'BoardType' - so we assume an early jade.
        if info.jadeFeatures.contains(Jade.FEATURE_SECURE_BOOT) {
            // Production Jade (Secure-Boot [and flash-encryption] enabled)
            return isV1BoardType ? Jade.FW_JADE_PATH : Jade.FW_JADE1_1_PATH
        } else {
            // Unsigned/development/testing Jade
            return isV1BoardType ? Jade.FW_JADEDEV_PATH : Jade.FW_JADE1_1DEV_PATH
        }
    }

    func download(_ fwpath: String, base64: Bool = false) -> [String: Any]? {
        let params: [String: Any] = [
            "method": "GET",
            "accept": base64 ? "base64": "text",
            "urls": ["\(Jade.FW_SERVER_HTTPS)\(fwpath)",
                     "\(Jade.FW_SERVER_ONION)\(fwpath)"] ]
        return try? SessionsManager.current?.httpRequest(params: params)
    }

    func checkFirmware(_ verInfo: JadeVersionInfo) throws -> [String: String]? {
        guard verInfo.jadeHasPin else {
            throw JadeError.Abort("Authentication required")
        }
        // Get relevant fmw path (or if hw not supported)
        guard let fwPath = firmwarePath(verInfo) else {
            throw JadeError.Abort("Unsupported hardware, firmware updates not available")
        }
        // Get the index file from that path
        #if DEBUG
        let link = "\(fwPath)BETA"
        #else
        let link = "\(fwPath)\(Jade.FW_VERSIONS_FILE)"
        #endif
        guard let res = download(link),
              let body = res["body"] as? String else {
            throw JadeError.Abort("Failed to fetch firmware file")
        }
        let versions = body.split(separator: "\n")
            .map { String($0) }
            .filter {
                let parts = $0.split(separator: "_")
                return parts.count == 4 && parts[3] == Jade.FW_SUFFIX
            }.map {
                let parts = $0.split(separator: "_")
                return ["filepath": "\(fwPath)\($0)",
                        "version": String(parts[0]),
                        "config": String(parts[1]),
                        "fwSize": String(parts[2])]
            }.filter { (v: [String: String]) in
                v["version"]! > verInfo.jadeVersion &&
                v["config"]!.lowercased() == verInfo.jadeConfig.lowercased()
            }
        return versions.first
    }

    func updateFirmare( verInfo: JadeVersionInfo, fmwFile: [String: String])-> Observable<Bool> {
        guard let res = download(fmwFile["filepath"] ?? "", base64: true),
            let body = res["body"] as? String,
            let fmw = Data(base64Encoded: body) else {
            return Observable.error(JadeError.Abort("Error downloading firmware file"))
        }
        let cmd = JadeOta(fwsize: Int(fmwFile["fwSize"] ?? "") ?? 0,
                          cmpsize: fmw.count,
                          otachunk: verInfo.jadeOtaMaxChunk)
        return exchange(JadeRequest(method: "ota", params: cmd))
            .flatMap { (_: JadeResponse<Bool>) in
                self.otaSend(fmw, size: fmw.count, chunksize: verInfo.jadeOtaMaxChunk)
            }.flatMap { _ in
                self.exchange(JadeRequest<JadeEmpty>(method: "ota_complete"))
            }.compactMap { (res: JadeResponse<Bool>) in
                return res.result ?? false
            }
    }

    func send(_ chunk: Data) -> Observable<Bool> {
        return Observable.create { observer in
            return self.exchange(JadeRequest(method: "ota_data", params: chunk))
                .subscribe(onNext: { (res: JadeResponse<Bool>) in
                    observer.onNext(res.result ?? false)
                    observer.onCompleted()
                }, onError: { err in
                    observer.onError(err)
                })
        }
    }

    func otaSend(_ data: Data, size: Int, chunksize: Int = 4 * 1024) -> Observable<Bool> {
        let chunks = stride(from: 0, to: data.count, by: Int(chunksize)).map {
            Array(data[$0 ..< Swift.min($0 + Int(chunksize), data.count)])
        }
        let sequence: [Observable<Bool>] = chunks.map { Observable.just(Data($0)) }
            .map { obs in
                return obs.flatMap { chunk in
                    self.send(chunk)
                }
            }
        return Observable.concat(sequence).takeLast(1)
    }
}
// Liquid calls
extension Jade {

    // Get blinding key for script
    func getBlindingKey(scriptHex: String) -> Observable<String?> {
        return exchange(JadeRequest(method: "get_blinding_key", params: JadeGetBlindingKey(scriptHex: scriptHex)))
            .compactMap { (res: JadeResponse<Data>) -> String? in
                return res.result?.map { String(format: "%02hhx", $0) }.joined()
            }
    }

    func getSharedNonce(pubkey: String, scriptHex: String) -> Observable<String?> {
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
    func signLiquidTransaction(network: String, tx: [String: Any], inputs: [[String: Any]], outputs: [[String: Any]], transactions: [String: String], useAeProtocol: Bool) -> Observable<[String: Any]> {
        let txInputs = inputs.map { input -> TxInputLiquid? in
            let swInput = !(input["address_type"] as? String == "p2sh")
            let script = input["prevout_script"] as? String
            let commitment = input["commitment"] as? String
            let aeHostCommitment = input["ae_host_commitment"] as? String
            let aeHostEntropy = input["ae_host_entropy"] as? String
            let path = input["user_path"] as? [UInt32]
            return TxInputLiquid.init(isWitness: swInput, scriptHex: script, valueCommitmentHex: commitment, path: path, aeHostEntropyHex: aeHostEntropy, aeHostCommitmentHex: aeHostCommitment)
        }

        // Get values, abfs and vbfs from inputs (needed to compute the final output vbf)
        var values = inputs.map { $0["satoshi"] as? UInt64 ?? 0 }
        var abfs = inputs.map { input -> [UInt8] in
            return [UInt8](hexToData(input["assetblinder"] as? String ?? "")).reversed()
        }
        var vbfs = inputs.map { input -> [UInt8] in
            return [UInt8](hexToData(input["amountblinder"] as? String ?? "")).reversed()
        }

        var inputPrevouts = [Data]()
        inputs.forEach { input in
            inputPrevouts += [Data(hexToData(input["txhash"] as? String ?? "").reversed())]
            inputPrevouts += [Data((input["pt_idx"] as? UInt ?? 0).uint32LE())]
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
            values.append(output["satoshi"] as? UInt64 ?? 0)
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
            values.append(lastBlindedOutput["satoshi"] as? UInt64 ?? 0)
            return self.getBlindingFactor(hashPrevouts: hashPrevOuts, outputIdx: lastBlindedIndex, type: "ASSET")
        }.flatMap { lastAbf -> Observable<Commitment> in
            abfs.append([UInt8](lastAbf!))
            // For the last blinded output we need to calculate the correct vbf so everything adds up
            let flattenAbfs = flatten(abfs, fixedSize: BLINDING_FACTOR_LEN)
            let flattenVbfs = flatten(vbfs, fixedSize: BLINDING_FACTOR_LEN)
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
            let txhex = tx["transaction"] as? String
            let txn = hexToData(txhex ?? "")
            return self.signLiquidTx(network: network, txn: txn, inputs: txInputs, trustedCommitments: trustedCommitments, changes: change, useAeProtocol: useAeProtocol)
        }.compactMap { (commitments: [String], signatures: [String]) in
            let assetGenerators = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.assetGenerator)) : nil) }
            let valueCommitments = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.valueCommitment)) : nil) }
            let abfs = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.abf.reversed())) : nil) }
            let vbfs = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.vbf.reversed())) : nil) }
            return ["signatures": signatures,
                    "signer_commitments": commitments,
                    "asset_commitments": assetGenerators,
                    "value_commitments": valueCommitments,
                    "assetblinders": abfs,
                    "amountblinders": vbfs]
        }
    }

    // swiftlint:disable:next function_parameter_count
    func signLiquidTx(network: String, txn: Data, inputs: [TxInputLiquid?], trustedCommitments: [Commitment?], changes: [TxChangeOutput?], useAeProtocol: Bool) -> Observable<(commitments: [String], signatures: [String])> {
        let cmd =
        JadeSignTx(change: changes, network: network, numInputs: inputs.count, trustedCommitments: trustedCommitments, useAeProtocol: useAeProtocol, txn: txn)
        return exchange(JadeRequest(method: "sign_liquid_tx", params: cmd))
            .flatMap { (res: JadeResponse<Bool>) -> Observable<(commitments: [String], signatures: [String])> in
                if let result = res.result, !result {
                    throw JadeError.Abort("Error response from initial sign_liquid_tx call: \(res.error?.message ?? "")")
                }
                if useAeProtocol {
                    return self.signTxInputsAntiExfil(inputs: inputs)
                } else {
                    return self.signTxInputs(inputs: inputs)
                }
            }
    }

    // Helper to get the commitment and blinding key from Jade
    func getTrustedCommitment(index: Int, output: [String: Any], hashPrevOuts: Data, customVbf: Data?) -> Observable<Commitment> {
        let package = JadeGetCommitment(hashPrevouts: hashPrevOuts,
                                        outputIdx: index,
                                        assetId: hexToData(output["asset_id"] as? String ?? ""),
                                        value: output["satoshi"] as? UInt64 ?? 0,
                                        vbf: customVbf)
        return Jade.shared.exchange(JadeRequest(method: "get_commitments", params: package))
            .compactMap { (res: JadeResponse<Commitment>) in
                // Add the script blinding key
                var comm = res.result
                comm?.blindingKey = hexToData(output["public_key"] as? String ?? "")
                return comm!
            }
    }

    func getMasterBlindingKey() -> Observable<String> {
        return exchange(JadeRequest<JadeEmpty>(method: "get_master_blinding_key", params: nil))
            .compactMap { (res: JadeResponse<[UInt8]>) -> String in
                dataToHex(Data(res.result ?? []))
            }
    }
}
