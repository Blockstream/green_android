import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth
import ga.wally
import SwiftCBOR

enum JadeError: Error {
    case Abort(_ localizedDescription: String)
    case URLError(_ localizedDescription: String)
    case Declined(_ localizedDescription: String)
}

final class Jade: JadeDevice, HWResolverProtocol {

    public static let shared = Jade()
    var xPubsCached = [String: String]()
    let SIGHASH_ALL: UInt8 = 1

    var info: [String: Any] { get { ["name": "Jade", "supports_arbitrary_scripts": true, "supports_liquid": 1, "supports_low_r": true] } }
    var connected: Bool { get { !self.xPubsCached.isEmpty }}

    func version() -> Observable<[String: Any]> {
        return Jade.shared.exchange(method: "get_version_info")
    }

    func addEntropy() -> Observable<[String: Any]> {
        let buffer = [UInt8].init(repeating: 0, count: 32).map { _ in UInt8(arc4random_uniform(0xff))}
        return Jade.shared.exchange(method: "add_entropy", params: ["entropy": Data(buffer)])
    }

    func httpRequest(_ httpRequest: [String: Any]) -> [String: Any] {
        let httpParams = httpRequest["params"] as? [String: Any]
        print("httpParams \(httpParams?.description)")
        let httpResponse = try? getSession().httpRequest(params: httpParams ?? [:])
        let httpResponseBody = httpResponse?["body"] as? [String: Any]
        print("httpResponseBody \(httpResponseBody?.description)")
        return httpResponseBody ?? [:]
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

    func signMessage(path: [Int], message: String) -> Observable<String> {
        let pathstr = path.map { UInt32($0) }
        let params = ["path": pathstr, "message": message] as [String: Any]
        return Jade.shared.exchange(method: "sign_message", params: params)
            .do(onNext: { print($0) })
            .flatMap { res -> Observable<String> in
                let encoded = res["result"] as? String
                let decoded = Data(base64Encoded: encoded!)
                let der = try sigToDer(sigDecoded: Array(decoded!))
                let hexSig = der.map { String(format: "%02hhx", $0) }.joined()
                return Observable.just(hexSig)
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
        let key = path.map { String($0) }.joined(separator: "/")
        if let xpub = xPubsCached[key] {
            return Observable.just(xpub)
        }
        let pathstr: [UInt32] = path.map { UInt32($0) }
        let params = [ "path": pathstr, "network": getNetwork()] as [String: Any]
        return Jade.shared.exchange(method: "get_xpub", params: params)
            .do(onNext: { print($0) })
            .flatMap { res -> Observable<String> in
                let xpub = res["result"] as? String
                self.xPubsCached[key] = xpub
                return Observable.just(xpub ?? "")
            }
    }

    func signTransaction(tx: [String: Any], inputs: [[String: Any]], outputs: [[String: Any]], transactions: [String: String], addressTypes: [String]) -> Observable<[String]> {

        if addressTypes.contains("p2pkh") {
            return Observable.error(JadeError.Abort("Hardware Wallet cannot sign sweep inputs"))
        }
        if transactions.isEmpty {
            return Observable.error(JadeError.Abort("Input transactions missing"))
        }

        let txInputs = inputs.map { input -> TxInputBtc? in
            let swInput = !(input["address_type"] as? String == "p2sh")
            let prevoutScript = input["prevout_script"] as? String
            let script = hexToData(prevoutScript!)
            let userPath = input["user_path"] as? [UInt32]

            if swInput && inputs.count == 1 {
                let satoshi = input["satoshi"] as? UInt64
                return TxInputBtc(isWitness: swInput, inputTx: nil, script: script, satoshi: satoshi, path: userPath)
            }
            let txhash = input["txhash"] as? String
            guard let txhex = transactions[txhash ?? ""] else {
                return nil
            }
            return TxInputBtc(isWitness: swInput, inputTx: hexToData(txhex), script: script, satoshi: nil, path: userPath)
        }

        if txInputs.contains(where: { $0 == nil }) {
            return Observable.error(JadeError.Abort("Input transactions missing"))
        }

        // Get the change outputs and paths
        guard let txOutputs = try? JSONDecoder().decode([TxInputOutputData].self, from: JSONSerialization.data(withJSONObject: outputs)) else {
            return Observable.error(JadeError.Abort("Decode error"))
        }
        let change = getChangeData(outputs: txOutputs)
        let changeParams = try? JSONSerialization.jsonObject(with: JSONEncoder().encode(change)) as? [String: Any] ?? [:]

        let network = getNetwork()
        let txhex = tx["transaction"] as? String
        let txn = hexToData(txhex ?? "")

        let params = [ "change": changeParams!, "network": network,
                       "num_inputs": inputs.count, "txn": txn] as [String: Any]
        return Jade.shared.exchange(method: "sign_tx", params: params)
            .do(onNext: { print($0) })
            .flatMap { _ in
                self.signTxInputs(baseId: 0, inputs: txInputs)
            }
    }

    func signTxInputs(baseId: Int, inputs: [TxInputProtocol?]) -> Observable<[String]> {
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
                print(signatures)
                return signatures
            }
    }

    func signTxInput(_ input: TxInputProtocol) -> Observable<Data> {
        return Observable.create { observer in
            let inputParams = input.encode()
            let package = Jade.shared.build(method: "tx_input", params: inputParams)
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

    // Helper to get the change paths for auto-validation
    func getChangeData(outputs: [TxInputOutputData]) -> [TxChangeOutput?] {
        // Get the change outputs and paths
        return outputs.map { output -> TxChangeOutput? in
            if !(output.isChange ?? true) {
                    return nil
                }
                var csvBlock = 0
                if let addressType = output.addressType, addressType == "csv" {
                    csvBlock = output.subtype ?? 0
                }
                return TxChangeOutput(path: output.userPath ?? [], recoveryxpub: output.recoveryXpub ?? "", csvBlocks: csvBlock)
            }
    }

    func inputBytes(_ input: [String: Any], isSegwit: Bool) -> Data? {
        let txHashHex = input["txhash"] as? String
        let ptIdx = input["pt_idx"] as? UInt
        let txId: [UInt8]? = hexToData(txHashHex!).reversed()
        return Data(txId! + ptIdx!.uint32LE() + (isSegwit ? (input["satoshi"] as? UInt64)!.uint64LE() : []))
    }

    // Get (receive) green address
    // swiftlint:disable:next function_parameter_count
    func newReceiveAddress(network: String, subaccount: UInt32, branch: UInt32, pointer: UInt32, recoveryChainCode: String?, recoveryPubKey: String?, csvBlocks: UInt32) -> Observable<String> {
        // Jade expects any 'recoveryxpub' to be at the subact/branch level, consistent with tx outputs - but gdk
        // subaccount data has the base subaccount chain code and pubkey - so we apply the branch derivation here.
        var recoveryxpub: String?
        if let chaincode = recoveryChainCode, !chaincode.isEmpty {
            let isNetworkMainnet = ["mainnet", "liquid"].contains(network)
            recoveryxpub = try? bip32KeyFromParentToBase58(isMainnet: isNetworkMainnet, pubKey: [UInt8](hexToData(recoveryPubKey!)), chainCode: [UInt8](hexToData(chaincode)), branch: branch)
        }
        let params = [ "network": network, "subaccount": subaccount, "pointer": pointer,
                       "branch": branch, "recovery_xpub": recoveryxpub ?? "", "csv_blocks": csvBlocks
        ] as [String: Any]

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

    static let MIN_ALLOWED_FW_VERSION = "0.1.23"
    static let FW_VERSIONS_FILE = "LATEST"
    static let FW_SERVER_HTTPS = "https://jadefw.blockstream.com"
    static let FW_SERVER_ONION = "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion"
    static let FW_JADE_PATH = "/bin/jade/"
    static let FW_JADEDEV_PATH = "/bin/jadedev/"
    static let FW_SUFFIX = "fw.bin"
    static let BOARD_TYPE_JADE = "JADE"
    static let FEATURE_SECURE_BOOT = "SB"

    // Check Jade fw against minimum allowed firmware version
    func isJadeFwValid(_ version: String) -> Bool {
        return Jade.MIN_ALLOWED_FW_VERSION <= version
    }

    func firmwarePath(_ info: [String: Any]) -> String? {
        let boardType = info["BOARD_TYPE"] as? String
        if boardType != nil && boardType != Jade.BOARD_TYPE_JADE {
            return nil
        }
        // Alas the first version of the jade fw didn't have 'BoardType' - so we assume an early jade.
        if let jadeFeatures = info["JADE_FEATURES"] as? String, jadeFeatures.contains(Jade.FEATURE_SECURE_BOOT) {
            // Production Jade (Secure-Boot [and flash-encryption] enabled)
            return Jade.FW_JADE_PATH
        } else {
            // Unsigned/development/testing Jade
            return Jade.FW_JADEDEV_PATH
        }
    }

    func download(_ fwpath: String, base64: Bool = false) -> [String: Any]? {
        let path = Bundle.main.path(forResource: "fwserver", ofType: "pem")
        let rootCert = try? String(contentsOfFile: path!, encoding: String.Encoding.utf8)
        let params: [String: Any] = [
            "method": "GET",
            "root_certificates": [rootCert!],
            "accept": base64 ? "base64": "text",
            "urls": ["\(Jade.FW_SERVER_HTTPS)\(fwpath)",
                     "\(Jade.FW_SERVER_ONION)\(fwpath)"] ]
        return try? getSession().httpRequest(params: params)
    }

    func checkFirmware(_ verInfo: [String: Any]) throws -> [String: String]? {
        guard let currentVersion = verInfo["JADE_VERSION"] as? String,
            let config = verInfo["JADE_CONFIG"] as? String else {
            throw JadeError.Abort("Invalid version")
        }
        guard let hasPin = verInfo["JADE_HAS_PIN"] as? Bool, hasPin else {
            throw JadeError.Abort("Authentication required")
        }
        // Get relevant fw path (or if hw not supported)
        guard let fwPath = firmwarePath(verInfo) else {
            throw JadeError.Abort("Unsupported hardware, firmware updates not available")
        }
        // Get the index file from that path
        guard let res = download("\(fwPath)\(Jade.FW_VERSIONS_FILE)"),
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

    func updateFirmare( verInfo: [String: Any], fwFile: [String: String])-> Observable<Bool> {
        guard let res = download(fwFile["filepath"] ?? "", base64: true),
            let body = res["body"] as? String,
            let fw = Data(base64Encoded: body) else {
            return Observable.error(JadeError.Abort("Error downloading firmware file"))
        }
        let chunk = verInfo["JADE_OTA_MAX_CHUNK"] as? UInt64
        let uncompressedSize = Int(fwFile["fwSize"] ?? "")
        let compressedSize = fw.count
        return Jade.shared.exchange(method: "ota", params: ["fwsize": uncompressedSize!,
                                                            "cmpsize": compressedSize,
                                                            "otachunk": chunk!])
        .flatMap { _ in
            self.otaSend(fw, size: uncompressedSize!, chunksize: chunk!)
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

    func signLiquidTransaction(tx: [String: Any], inputs: [[String: Any]], outputs: [[String: Any]], transactions: [String: String], addressTypes: [String]) -> Observable<[String: Any]> {

        if addressTypes.contains("p2pkh") {
            return Observable.error(JadeError.Abort("Hardware Wallet cannot sign sweep inputs"))
        }

        guard let dataOutputs = try? JSONSerialization.data(withJSONObject: outputs),
              let txOutputs = try? JSONDecoder().decode([TxInputOutputData].self, from: dataOutputs) else {
            return Observable.error(JadeError.Abort("Decode error"))
        }

        guard let dataInputs = try? JSONSerialization.data(withJSONObject: inputs),
              let inputs = try? JSONDecoder().decode([TxInputOutputData].self, from: dataInputs) else {
            return Observable.error(JadeError.Abort("Decode error"))
        }

        let txInputs = inputs.map { input -> TxInputLiquid? in
            let swInput = !(input.addressType == "p2sh")
            let script = hexToData(input.prevoutScript ?? "")
            let commitment = hexToData(input.commitment ?? "")
            return TxInputLiquid(isWitness: swInput, script: script, valueCommitment: commitment, path: input.userPath)
        }

        // Get values, abfs and vbfs from inputs (needed to compute the final output vbf)
        var values = inputs.map { $0.satoshi ?? 0 }
        var abfs = inputs.map { $0.abf }
        var vbfs = inputs.map { $0.vbf }

        var inputPrevouts = [Data]()
        inputs.forEach { input in
            inputPrevouts += [Data(hexToData(input.txhash!).reversed())]
            inputPrevouts += [Data(input.ptIdx!.uint32LE())]
        }

        // Compute the hash of all input prevouts for making deterministic blinding factors
        let prevOuts = flatten(inputPrevouts.map { [UInt8]($0) }, fixedSize: nil)
        let hashPrevOuts = Data(try! sha256d(prevOuts))

        // Get trusted commitments per output - null for unblinded outputs
        var trustedCommitments = [Commitment?]()

        // For all-but-last blinded entry, do not pass custom vbf, so one is generated
        // Append the output abfs and vbfs to the arrays
        // assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
        let lastBlindedIndex = txOutputs.count - 2;  // Could determine this properly
        var obsCommitments = [Observable<Commitment>]()
        let lastBlindedOutput = txOutputs[lastBlindedIndex]
        for (i, output) in txOutputs.enumerated() where i < lastBlindedIndex {
            values.append(output.satoshi ?? 0)
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
            values.append(lastBlindedOutput.satoshi ?? 0)
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
        }.flatMap { lastCommitment -> Observable<[String]> in
            trustedCommitments.append(lastCommitment)
            // Add a 'null' commitment for the final (fee) output
            trustedCommitments.append(nil)
            // Get the change outputs and paths
            let change = self.getChangeData(outputs: txOutputs)
            // Make jade-api call to sign the txn
            let network = getNetwork()
            let txhex = tx["transaction"] as? String
            let txn = hexToData(txhex ?? "")
            return Jade.shared.signLiquidTx(network: network, txn: txn, inputs: txInputs, trustedCommitments: trustedCommitments, changes: change)
        }.compactMap { signatures -> [String: Any] in
            let assetGenerators = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.assetGenerator)) : nil) }
            let valueCommitments = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.valueCommitment)) : nil) }
            let abfs = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.abf.reversed())) : nil) }
            let vbfs = trustedCommitments.map { (($0 != nil) ? dataToHex(Data($0!.vbf.reversed())) : nil) }
            return ["signatures": signatures,
                    "asset_commitments": assetGenerators,
                    "value_commitments": valueCommitments,
                    "assetblinders": abfs,
                    "amountblinders": vbfs]
        }
    }

    func signLiquidTx(network: String, txn: Data, inputs: [TxInputLiquid?], trustedCommitments: [Commitment?], changes: [TxChangeOutput?]) -> Observable<[String]> {
        let changeParams = changes.map { change -> [String: Any]? in
            let data = try? JSONEncoder().encode(change)
            var dict = try? JSONSerialization.jsonObject(with: data!) as? [String: Any]
            dict?["path"] = change?.path ?? []
            return dict
        }
        let commitmentsParams = trustedCommitments.map { comm -> [String: Any]? in
            let data = try? JSONEncoder().encode(comm)
            let dict = try? JSONSerialization.jsonObject(with: data!) as? [String: Any]
            return dict
        }
        let params = ["change": changeParams,
                      "network": network,
                      "num_inputs": inputs.count,
                      "trusted_commitments": commitmentsParams,
                      "txn": txn] as [String: Any]

        return Jade.shared.exchange(method: "sign_liquid_tx", params: params)
            .flatMap { res -> Observable<[String]> in
                guard res["result"] as? Bool != nil else {
                    throw JadeError.Abort("Error response from initial sign_liquid_tx call: \(res.description)")
                }
                return self.signTxInputs(baseId: 0, inputs: inputs)
            }
    }

    // Helper to get the commitment and blinding key from Jade
    func getTrustedCommitment(index: Int, output: TxInputOutputData, hashPrevOuts: Data, customVbf: Data?) -> Observable<Commitment> {
        let assetId = hexToData(output.assetId ?? "")
        var params = [  "hash_prevouts": hashPrevOuts,
                        "output_index": index,
                        "asset_id": assetId,
                        "value": output.satoshi ?? 0 ] as [String: Any]
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
                comm.blindingKey = [UInt8](hexToData(output.publicKey!))
                return comm
            }
    }
}
