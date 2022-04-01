import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth
import ga.wally
import SwiftCBOR

final class Jade: JadeChannel, HWProtocol {

    public static let shared = Jade()
    static let EXCLUDED_CERTIFICATE = "-----BEGIN CERTIFICATE-----\nMIIDSjCCAjKgAwIBAgIQRK+wgNajJ7qJMDmGLvhAazANBgkqhkiG9w0BAQUFADA/\nMSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT\nDkRTVCBSb290IENBIFgzMB4XDTAwMDkzMDIxMTIxOVoXDTIxMDkzMDE0MDExNVow\nPzEkMCIGA1UEChMbRGlnaXRhbCBTaWduYXR1cmUgVHJ1c3QgQ28uMRcwFQYDVQQD\nEw5EU1QgUm9vdCBDQSBYMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\nAN+v6ZdQCINXtMxiZfaQguzH0yxrMMpb7NnDfcdAwRgUi+DoM3ZJKuM/IUmTrE4O\nrz5Iy2Xu/NMhD2XSKtkyj4zl93ewEnu1lcCJo6m67XMuegwGMoOifooUMM0RoOEq\nOLl5CjH9UL2AZd+3UWODyOKIYepLYYHsUmu5ouJLGiifSKOeDNoJjj4XLh7dIN9b\nxiqKqy69cK3FCxolkHRyxXtqqzTWMIn/5WgTe1QLyNau7Fqckh49ZLOMxt+/yUFw\n7BZy1SbsOFU5Q9D8/RhcQPGX69Wam40dutolucbY38EVAjqr2m7xPi71XAicPNaD\naeQQmxkqtilX4+U9m5/wAl0CAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNV\nHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFMSnsaR7LHH62+FLkHX/xBVghYkQMA0GCSqG\nSIb3DQEBBQUAA4IBAQCjGiybFwBcqR7uKGY3Or+Dxz9LwwmglSBd49lZRNI+DT69\nikugdB/OEIKcdBodfpga3csTS7MgROSR6cz8faXbauX+5v3gTt23ADq1cEmv8uXr\nAvHRAosZy5Q6XkjEGB5YGV8eAlrwDPGxrancWYaLbumR9YbK+rlmM6pZW87ipxZz\nR8srzJmwN0jP41ZL9c8PDHIyh8bwRLtTcm1D9SZImlJnt1ir/md2cXjbDaJWFBM5\nJDGFoqgCWjBH4d1QB7wCCZAA62RjYJsWvIjJEubSfZGL+T0yjWW06XyxV3bqxbYo\nOb8VZRzI9neWagqNdwvYkQsEjgfbKbYK7p2CNTUQ\n-----END CERTIFICATE-----\n"
    let SIGHASH_ALL: UInt8 = 1

    func version() -> Observable<[String: Any]> {
        return Jade.shared.exchange(method: "get_version_info")
    }

    func addEntropy() -> Observable<[String: Any]> {
        let buffer = [UInt8].init(repeating: 0, count: 32).map { _ in UInt8(arc4random_uniform(0xff))}
        return Jade.shared.exchange(method: "add_entropy", params: ["entropy": Data(buffer)])
    }

    func httpRequest(_ httpRequest: [String: Any]) -> [String: Any]? {
        // Temporary workaround - prune old cert (from Jade pre- 0.1.28 fw)
        // until gdk better at ignoring bad/invalid/expired certs
        var httpParams = httpRequest["params"] as? [String: Any]
        if let rootCert = httpParams?["root_certificates"] as? [String], rootCert.count == 1 && rootCert.contains(Jade.EXCLUDED_CERTIFICATE) {
            httpParams?.removeValue(forKey: "root_certificates")
        }
        let httpResponse = try? SessionsManager.current?.httpRequest(params: httpParams ?? [:])
        let httpResponseBody = httpResponse?["body"] as? [String: Any]
        return httpResponseBody
    }

    func auth(network: String) -> Observable<[String: Any]> {
        // Send initial auth user request
        return Jade.shared.exchange(method: "auth_user", params: ["network": network])
        .observeOn(SerialDispatchQueueScheduler(qos: .background))
        .flatMap { res -> Observable<[String: Any]> in
            if let result = res["result"] as? Bool, result == true {
                return Observable.just(res)
            }
            if let error = res["error"] as? [String: Any] {
                let message = error["message"] as? String
                return Observable.error(JadeError.Abort(message ?? ""))
            }
            guard let r = res["result"] as? [String: Any],
                let httpRequest = r["http_request"] as? [String: Any] else {
                return Observable.error(JadeError.Abort(""))
            }
            let onHttpReplyCall = httpRequest["on-reply"] as? String
            let httpResponseBody = self.httpRequest(httpRequest)
            return Jade.shared.exchange(method: onHttpReplyCall ?? "", params: httpResponseBody)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { res -> Observable<[String: Any]> in
                guard let r = res["result"] as? [String: Any],
                    let httpRequest = r["http_request"] as? [String: Any] else {
                    return Observable.error(JadeError.Abort(""))
                }
                let onHttpReplyCall = httpRequest["on-reply"] as? String
                let httpResponseBody = self.httpRequest(httpRequest)
                return Jade.shared.exchange(method: onHttpReplyCall ?? "", params: httpResponseBody)
            }.flatMap { res -> Observable<[String: Any]>  in
                if let result = res["result"] as? Bool, result {
                    return Observable.just(res)
                }
                return Observable.error(JadeError.Abort(NSLocalizedString("id_login_failed", comment: "")))
            }
        }
    }

    func signMessage(path: [Int]?,
                     message: String?,
                     useAeProtocol: Bool?,
                     aeHostCommitment: String?,
                     aeHostEntropy: String?)
    -> Observable<(signature: String?, signerCommitment: String?)> {
        let pathstr = path?.map { UInt32($0) }
        var signMessage: Observable<[String: Any]>?
        var signerCommitment: String?
        if useAeProtocol ?? false {
            // Anti-exfil protocol:
            // We send the signing request with the host-commitment and receive the signer-commitment
            // in reply once the user confirms.
            // We can then request the actual signature passing the host-entropy.
            let aeHostCommitment = hexToData(aeHostCommitment ?? "")
            let aeHostEntropy = hexToData(aeHostEntropy ?? "")
            let params = ["path": pathstr ?? [],
                          "message": message ?? "",
                          "ae_host_commitment": aeHostCommitment
            ] as [String: Any]
            signMessage = Jade.shared.exchange(method: "sign_message", params: params)
                .compactMap { res in
                    let result = res["result"] as? [UInt8]
                    signerCommitment = result?.map { String(format: "%02hhx", $0) }.joined()
                }.flatMap { _ -> Observable<[String: Any]> in
                    Jade.shared.exchange(method: "get_signature",
                                         params: ["ae_host_entropy": aeHostEntropy])
                }
        } else {
            // Standard EC signature, simple case
            let params = ["path": pathstr ?? [],
                          "message": message ?? ""] as [String: Any]
            signMessage = Jade.shared.exchange(method: "sign_message", params: params)
        }
        return signMessage!.compactMap { res -> (String?, String?) in
            let signature = res["result"] as? String
            return (signature, signerCommitment)
        }.compactMap { (sign, signerCom) -> (signature: String?, signerCommitment: String?) in
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

    func xpubs(paths: [[Int]]) -> Observable<[String]> {
        let allObservables = paths
            .map {
                Observable.just($0)
                    .flatMap { self.xpubs(path: $0) }
        }
        return Observable.concat(allObservables)
        .reduce([], accumulator: { result, element in
            result + [element]
        })
    }

    func xpubs(path: [Int]) -> Observable<String> {
        let pathstr: [UInt32] = path.map { UInt32($0) }
        let params = [ "path": pathstr, "network": getNetwork()] as [String: Any]
        return Jade.shared.exchange(method: "get_xpub", params: params)
            .flatMap { res -> Observable<String> in
                let xpub = res["result"] as? String
                return Observable.just(xpub ?? "")
            }
    }

    func signTransaction(tx: [String: Any], inputs: [[String: Any]], outputs: [[String: Any]], transactions: [String: String], useAeProtocol: Bool) -> Observable<[String: Any]> {

        if transactions.isEmpty {
            return Observable.error(JadeError.Abort("Input transactions missing"))
        }

        let txInputs = inputs.map { input -> TxInputBtc? in
            let addressType = input["address_type"] as? String
            let swInput = isSegwit(addressType)
            let prevoutScript = input["prevout_script"] as? String
            let script = hexToData(prevoutScript!)
            let userPath = input["user_path"] as? [UInt32]
            let aeHostCommitment = hexToData(input["ae_host_commitment"] as? String ?? "")
            let aeHostEntropy = hexToData(input["ae_host_entropy"] as? String ?? "")

            if swInput && inputs.count == 1 {
                let satoshi = input["satoshi"] as? UInt64
                return TxInputBtc(isWitness: swInput, inputTx: nil, script: Array(script), satoshi: satoshi, path: userPath, aeHostEntropy: Array(aeHostEntropy), aeHostCommitment: Array(aeHostCommitment))
            }
            let txhash = input["txhash"] as? String
            guard let txhex = transactions[txhash ?? ""] else {
                return nil
            }
            return TxInputBtc(isWitness: swInput, inputTx: Array(hexToData(txhex)), script: Array(script), satoshi: nil, path: userPath, aeHostEntropy: Array(aeHostEntropy), aeHostCommitment: Array(aeHostCommitment))
        }

        if txInputs.contains(where: { $0 == nil }) {
            return Observable.error(JadeError.Abort("Input transactions missing"))
        }

        let change = getChangeData(outputs: outputs)
        let changeParams = try? JSONSerialization.jsonObject(with: JSONEncoder().encode(change)) as? [String: Any] ?? [:]

        let network = getNetwork()
        let txhex = tx["transaction"] as? String
        let txn = hexToData(txhex ?? "")

        let params = [ "change": changeParams!,
                       "network": network,
                       "num_inputs": inputs.count,
                       "use_ae_signatures": useAeProtocol,
                       "txn": txn] as [String: Any]
        return Jade.shared.exchange(method: "sign_tx", params: params)
            .flatMap { _ -> Observable<(commitments: [String], signatures: [String])> in
                if useAeProtocol {
                    return self.signTxInputsAntiExfil(baseId: 0, inputs: txInputs)
                } else {
                    return self.signTxInputs(baseId: 0, inputs: txInputs)
                }
            }.compactMap { (commitments, signatures) in
                return ["signatures": signatures, "signer_commitments": commitments]
            }
    }

    func signTxInputsAntiExfil(baseId: Int, inputs: [TxInputProtocol?]) -> Observable<(commitments: [String], signatures: [String])> {
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
                .flatMap {
                    Jade.shared.exchange(method: "tx_input", params: $0.encode())
                }.compactMap { res ->  String in
                    let result = res["result"] as? [UInt8]
                    return dataToHex(Data(result ?? []))
                }
        }
        let signatures = inputs.map {
            Observable.just($0!)
                .compactMap { input -> [UInt8]? in
                    if let inputBtc = input as? TxInputBtc {
                        return inputBtc.aeHostEntropy
                    } else if let inputLiquid = input as? TxInputLiquid {
                        return inputLiquid.aeHostEntropy
                    } else {
                        return nil
                    }
                }.flatMap { aeHostEntropy in
                    Jade.shared.exchange(method: "get_signature", params: ["ae_host_entropy": aeHostEntropy!])
                }.compactMap { res -> String in
                    let result = res["result"] as? [UInt8]
                    return dataToHex(Data(result ?? []))
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

    func signTxInputs(baseId: Int, inputs: [TxInputProtocol?]) -> Observable<(commitments: [String], signatures: [String])> {
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
                        let decoded = try? CBOR.decode([UInt8](buffer))
                        return CBOR.parser(decoded ?? CBOR("")) as? [String: Any] ?? [:]
                    }.flatMap { (res: [String: Any]) -> Observable<[UInt8]> in
                        return Observable<[UInt8]>.create { observer in
                            if let error = res["error"] as? [String: Any],
                               let message = error["message"] as? String {
                                observer.onError(JadeError.Declined(message))
                            } else if let result = res["result"] as? [UInt8] {
                                observer.onNext(result)
                                observer.onCompleted()
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
            let package = Jade.shared.build(method: "tx_input", params: input.encode())
            #if DEBUG
            print("=> " + package.map { String(format: "%02hhx", $0) }.joined())
            #endif
            return Jade.shared.write(package)
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
                                  recoveryxpub: output["recovery_xpub"] as? String ?? "",
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
            return newReceiveAddress(network: network.network,
                                     subaccount: wallet.pointer,
                                     branch: branch,
                                     pointer: pointer,
                                     recoveryPubKey: recoveryxpub,
                                     csvBlocks: csvBlocks)
        } else {
            // Green Electrum Singlesig
            let variant = mapAddressType(wallet.type)
            return newReceiveAddress(network: network.network,
                                     variant: variant ?? "",
                                     path: path)
        }
    }

    // Get (receive) green address - multisig
    // swiftlint:disable:next function_parameter_count
    func newReceiveAddress(network: String, subaccount: UInt32, branch: UInt32, pointer: UInt32, recoveryPubKey: String?, csvBlocks: UInt32) -> Observable<String> {
        var params = [ "network": network,
                       "pointer": pointer,
                       "subaccount": subaccount,
                       "branch": branch ] as [String: Any]
        // Optional fields
        if let recoveryPubKey = recoveryPubKey, !recoveryPubKey.isEmpty {
            params["recovery_xpub"] = recoveryPubKey
        }
        if csvBlocks > 0 {
            params["csv_blocks"] = csvBlocks
        }
        return Jade.shared.exchange(method: "get_receive_address", params: params)
        .flatMap { res -> Observable<String> in
            guard let result = res["result"] as? String else {
                return Observable.error(JadeError.Abort(""))
            }
            return Observable.just(result)
        }
    }

    // Get (receive) green address - singlesig
    func newReceiveAddress(network: String, variant: String, path: [UInt32]) -> Observable<String> {
        let params = [ "network": network,
                       "path": path,
                       "variant": variant ] as [String: Any]
        return Jade.shared.exchange(method: "get_receive_address", params: params)
        .flatMap { res -> Observable<String> in
            guard let result = res["result"] as? String else {
                return Observable.error(JadeError.Abort(""))
            }
            return Observable.just(result)
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

    func firmwarePath(_ info: [String: Any]) -> String? {
        let boardType = info["BOARD_TYPE"] as? String
        if boardType != nil && ![Jade.BOARD_TYPE_JADE, Jade.BOARD_TYPE_JADE_V1_1].contains(boardType) {
            return nil
        }
        let isV1BoardType = boardType == Jade.BOARD_TYPE_JADE
        // Alas the first version of the jade fmw didn't have 'BoardType' - so we assume an early jade.
        if let jadeFeatures = info["JADE_FEATURES"] as? String, jadeFeatures.contains(Jade.FEATURE_SECURE_BOOT) {
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

    func checkFirmware(_ verInfo: [String: Any]) throws -> [String: String]? {
        guard let currentVersion = verInfo["JADE_VERSION"] as? String,
            let config = verInfo["JADE_CONFIG"] as? String else {
            throw JadeError.Abort("Invalid version")
        }
        guard let hasPin = verInfo["JADE_HAS_PIN"] as? Bool, hasPin else {
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
                v["version"]! > currentVersion &&
                    v["config"]!.lowercased() == config.lowercased()
            }
        return versions.first
    }

    func updateFirmare( verInfo: [String: Any], fmwFile: [String: String])-> Observable<Bool> {
        guard let res = download(fmwFile["filepath"] ?? "", base64: true),
            let body = res["body"] as? String,
            let fmw = Data(base64Encoded: body) else {
            return Observable.error(JadeError.Abort("Error downloading firmware file"))
        }
        let chunk = verInfo["JADE_OTA_MAX_CHUNK"] as? UInt64
        let uncompressedSize = Int(fmwFile["fwSize"] ?? "")
        let compressedSize = fmw.count
        return Jade.shared.exchange(method: "ota", params: ["fwsize": uncompressedSize!,
                                                            "cmpsize": compressedSize,
                                                            "otachunk": chunk!])
        .flatMap { _ in
            self.otaSend(fmw, size: uncompressedSize!, chunksize: chunk!)
        }.flatMap { _ in
            return Observable.just(true)
        }
    }

    func send(_ chunk: [UInt8]) -> Observable<Bool> {
        return Observable.create { observer in
            return Jade.shared.exchange(method: "ota_data", params: chunk)
                .subscribe(onNext: { res in
                    let result = res["result"] as? Bool
                    observer.onNext(result ?? false)
                    observer.onCompleted()
                }, onError: { err in
                    observer.onError(err)
                })
        }
    }

    func otaSend(_ data: Data, size: Int, chunksize: UInt64 = 4 * 1024) -> Observable<Bool> {
        let chunks = stride(from: 0, to: data.count, by: Int(chunksize)).map {
            Array(data[$0 ..< Swift.min($0 + Int(chunksize), data.count)])
        }
        let sequence: [Observable<Bool>] = chunks.map { Observable.just($0) }
            .map { obs in
                return obs.flatMap { chunk in
                    self.send(chunk)
                }
            }
        return Observable.concat(sequence)
            .takeLast(1)
            .flatMap { _ in
                Jade.shared.exchange(method: "ota_complete")
            }.compactMap { res in
                if let result = res["result"] as? Bool, result {
                    return true
                }
                return false
            }
    }
}
// Liquid calls
extension Jade {

    // Get blinding key for script
    func getBlindingKey(scriptHex: String) -> Observable<String?> {
        let script = hexToData(scriptHex)
        return exchange(method: "get_blinding_key", params: [ "script": script ])
            .compactMap { res -> String? in
                let bkey = res["result"] as? [UInt8]
                return bkey?.map { String(format: "%02hhx", $0) }.joined()
            }
    }

    func getSharedNonce(pubkey: String, scriptHex: String) -> Observable<String?> {
        let pkey = hexToData(pubkey)
        let script = hexToData(scriptHex)
        return exchange(method: "get_shared_nonce", params: [ "script": script, "their_pubkey": pkey ])
            .compactMap { res -> String? in
                let bkey = res["result"] as? [UInt8]
                return bkey?.map { String(format: "%02hhx", $0) }.joined()
            }
    }

    func getBlindingFactor(hashPrevouts: Data, outputIdx: Int, type: String) -> Observable<Data?> {
        let params = ["hash_prevouts": hashPrevouts, "output_index": outputIdx, "type": type] as [String: Any]
        return exchange(method: "get_blinding_factor", params: params)
            .compactMap { res -> Data? in
                let result = res["result"] as? [UInt8]
                return Data(result!)
            }
    }

    func getCommitments(assetId: Data, value: UInt32, hashPrevouts: Data, outputIdx: Int, vbf: Data?) -> Observable<[String: Any]?> {
        var params = ["hash_prevouts": hashPrevouts, "output_index": outputIdx, "asset_id": assetId, "value": value] as [String: Any]
        if let vbf_ = vbf {
            params["vbf"] = vbf_
        }
        return exchange(method: "get_commitments", params: params)
            .compactMap { res -> [String: Any]? in
                return res["result"] as? [String: Any]
            }
    }

    func signLiquidTransaction(tx: [String: Any], inputs: [[String: Any]], outputs: [[String: Any]], transactions: [String: String], useAeProtocol: Bool) -> Observable<[String: Any]> {

        let txInputs = inputs.map { input -> TxInputLiquid? in
            let swInput = !(input["address_type"] as? String == "p2sh")
            let script = hexToData(input["prevout_script"] as? String ?? "")
            let commitment = hexToData(input["commitment"] as? String ?? "")
            let aeHostCommitment = hexToData(input["ae_host_commitment"] as? String ?? "")
            let aeHostEntropy = hexToData(input["ae_host_entropy"] as? String ?? "")
            return TxInputLiquid(isWitness: swInput, script: Array(script), valueCommitment: Array(commitment), path: input["user_path"] as? [UInt32], aeHostEntropy: Array(aeHostEntropy), aeHostCommitment: Array(aeHostCommitment))
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
            abfs.append(commitment.abf)
            vbfs.append(commitment.vbf)
            return []
        }).flatMap { _ -> Observable<Data?> in
            // For the last blinded output, get the abf only
            values.append(lastBlindedOutput["satoshi"] as? UInt64 ?? 0)
            return Jade.shared.getBlindingFactor(hashPrevouts: hashPrevOuts, outputIdx: lastBlindedIndex, type: "ASSET")
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
            let network = getNetwork()
            let txhex = tx["transaction"] as? String
            let txn = hexToData(txhex ?? "")
            return Jade.shared.signLiquidTx(network: network, txn: txn, inputs: txInputs, trustedCommitments: trustedCommitments, changes: change, useAeProtocol: useAeProtocol)
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
        let changeParams = changes.map { change -> [String: Any]? in
            let data = try? JSONEncoder().encode(change)
            var dict = try? JSONSerialization.jsonObject(with: data!) as? [String: Any]
            dict?["path"] = change?.path ?? []
            return dict
        }
        let commitmentsParams = trustedCommitments.map { comm -> [String: Any]? in
            let data = try? JSONEncoder().encode(comm)
            var dict = try? JSONSerialization.jsonObject(with: data!) as? [String: Any]
            dict?["value"] = comm?.value ?? 0
            return dict
        }
        let params = ["change": changeParams,
                      "network": network,
                      "num_inputs": inputs.count,
                      "trusted_commitments": commitmentsParams,
                      "use_ae_signatures": useAeProtocol,
                      "txn": txn] as [String: Any]

        return Jade.shared.exchange(method: "sign_liquid_tx", params: params)
            .flatMap { res -> Observable<(commitments: [String], signatures: [String])> in
                guard res["result"] as? Bool != nil else {
                    throw JadeError.Abort("Error response from initial sign_liquid_tx call: \(res.description)")
                }
                if useAeProtocol {
                    return self.signTxInputsAntiExfil(baseId: 0, inputs: inputs)
                } else {
                    return self.signTxInputs(baseId: 0, inputs: inputs)
                }
            }
    }

    // Helper to get the commitment and blinding key from Jade
    func getTrustedCommitment(index: Int, output: [String: Any], hashPrevOuts: Data, customVbf: Data?) -> Observable<Commitment> {
        let assetId = hexToData(output["asset_id"] as? String ?? "")
        var params = [  "hash_prevouts": hashPrevOuts,
                        "output_index": index,
                        "asset_id": assetId,
                        "value": output["satoshi"] as? UInt64 ?? 0 ] as [String: Any]
        if let vbf = customVbf, !vbf.isEmpty {
            params["vbf"] = vbf
        }
        return Jade.shared.exchange(method: "get_commitments", params: params)
            .compactMap { res in
                let result = res["result"] as? [String: Any]
                let json = try JSONSerialization.data(withJSONObject: result!, options: [])
                let comm = try JSONDecoder().decode(Commitment.self, from: json)
                return comm
            }.compactMap { (res: Commitment) in
                // Add the script blinding key
                var comm = res
                comm.blindingKey = [UInt8](hexToData(output["public_key"] as? String ?? ""))
                return comm
            }
    }

    func getMasterBlindingKey() -> Observable<String> {
        return Jade.shared.exchange(method: "get_master_blinding_key", params: [:])
            .compactMap { res in
                dataToHex(Data(res["result"] as? [UInt8] ?? []))
            }.catchError { _ in
                Observable.just("")
            }
    }
}
