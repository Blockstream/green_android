import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth
import ga.wally

enum JadeError: Error {
    case Abort(_ localizedDescription: String)
    case URLError(_ localizedDescription: String)
}

final class Jade: JadeDevice, HWResolverProtocol {

    public static let shared = Jade()
    let hwDevice: [String: Any] = ["name": "Jade", "supports_arbitrary_scripts": true, "supports_liquid": 1, "supports_low_r": true]
    var xPubsCached = [String: String]()
    let SIGHASH_ALL: UInt8 = 1
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
                return Observable.error(JadeError.Abort("Handshake failure"))
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
                let der = try self.sigToDer(sigDecoded: Array(decoded!))
                let hexSig = der.map { String(format: "%02hhx", $0) }.joined()
                return Observable.just(hexSig)
            }
    }

    public func sigToDer(sigDecoded: [UInt8]) throws -> [UInt8] {
        let sig = sigDecoded[1..<sigDecoded.count]
        let sigPtr: UnsafePointer<UInt8> = UnsafePointer(Array(sig))
        let derPtr = UnsafeMutablePointer<UInt8>.allocate(capacity: Int(EC_SIGNATURE_DER_MAX_LEN))
        var written: Int = 0
        if wally_ec_sig_to_der(sigPtr, sig.count, derPtr, Int(EC_SIGNATURE_DER_MAX_LEN), &written) != WALLY_OK {
            throw GaError.GenericError
        }
        let der = Array(UnsafeBufferPointer(start: derPtr, count: written))
        //derPtr.deallocate()
        return der
    }

    func xpubs(paths: [[Int]]) -> Observable<[String]> {
        let allObservables = paths
            .map {
                Observable.just($0)
                    .flatMap { self.xpubs(path: $0) }
                    .asObservable()
                    .take(1)
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
            let prevoutScript = inputs.first?["prevout_script"] as? String
            let script = swInput ? hexToData(prevoutScript!) : nil
            let userPath = input["user_path"] as? [UInt32]

            if swInput && inputs.count == 1 {
                let satoshi = input["satoshi"] as? UInt64
                return TxInputBtc(isWitness: swInput, inputTx: nil, script: script, satoshi: satoshi, path: userPath)
            }
            let txhash = input["txhash"] as? String
            guard let txhex = transactions[txhash ?? ""] else {
                return nil
            }
            let inputTx = hexToData(txhex).reversed()
            return TxInputBtc(isWitness: swInput, inputTx: Data(inputTx), script: script, satoshi: nil, path: userPath)
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

    func signTxInputs(baseId: Int, inputs: [TxInputBtc?]) -> Observable<[String]> {
        //return Observable.just([Data()])
        let allObservables = inputs
            .map {
                Observable.just($0!)
                    .flatMap { self.signTxInput($0) }
                    .compactMap { $0.map { String(format: "%02x", $0) }.joined() }
                    .asObservable()
                    .take(1)
        }
        return Observable.concat(allObservables)
        .reduce([], accumulator: { result, element in
            result + [element]
        })
    }

    func signTxInput(_ input: TxInputBtc) -> Observable<Data> {
        return Observable.create { observer in
            let inputParams = input.encode()
            return Jade.shared.exchange(method: "tx_input", params: inputParams)
                .subscribe(onNext: { res in
                    let result = res["result"] as? [UInt8]
                    observer.onNext(Data(result ?? []))
                    observer.onCompleted()
                }, onError: { err in
                    observer.onError(err)
                })
        }
    }

    // Helper to get the change paths for auto-validation
    func getChangeData(outputs: [TxInputOutputData]) -> [TxChangeOutput] {
        // Get the change outputs and paths
        return outputs.filter { $0.isChange }
            .map { output -> TxChangeOutput in
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

    func download(_ fwname: String) -> Data? {
        let path = Bundle.main.path(forResource: "fwserver", ofType: "pem")
        let rootCert = try? String(contentsOfFile: path!, encoding: String.Encoding.utf8)
        let params: [String: Any] = [
            "method": "GET",
            "root_certificates": [rootCert!],
            "accept": "base64",
            "urls": ["https://jadefw.blockstream.com/bin/\(fwname)"] ]
        let response = try? getSession().httpRequest(params: params)
        let responseBody = response?["body"] as? String
        return Data(base64Encoded: responseBody ?? "")
    }

    func latestFW() -> String? {
        let path = Bundle.main.path(forResource: "fwserver", ofType: "pem")
        let rootCert = try? String(contentsOfFile: path!, encoding: String.Encoding.utf8)
        let params: [String: Any] = [
            "method": "GET",
            "root_certificates": [rootCert!],
            "urls": ["https://jadefw.blockstream.com/bin/LATEST"] ]
        let response = try? getSession().httpRequest(params: params)
        let responseBody = response?["body"] as? String
        return responseBody?.split(separator: "\n")
            .map { String($0) }
            .filter({ $0.contains("ble") })
            .first
    }

    func ota() -> Observable<Bool> {
        guard let fwname = latestFW(),
            let fwlen = Int(fwname.split(separator: "_")[2]),
            let fwcmp = download(String(fwname)) else {
            return Observable.error(JadeError.Abort(""))
        }
        print("firmware \(fwname) (\(fwlen))")
        return otaUpdate(fwcmp: fwcmp, len: fwlen, chunksize: 1024 * 4)
    }

    func otaUpdate(fwcmp: Data, len: Int, chunksize: Int) -> Observable<Bool> {
        let params = ["fwsize": len, "cmpsize": fwcmp.count] as [String: Any]
        return Jade.shared.exchange(method: "ota", params: params)
        .flatMap { _ in
            self.otaSend(fwcmp, size: len, chunksize: chunksize)
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

    func otaSend(_ data: Data, size: Int, chunksize: Int = 4 * 1024) -> Observable<Bool> {
        let chunks = stride(from: 0, to: data.count, by: chunksize).map {
            Array(data[$0 ..< Swift.min($0 + chunksize, data.count)])
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

    // Get (receive) green address
    // swiftlint:disable:next function_parameter_count
    func getReceiveAddress(network: String, subaccount: UInt32, branch: UInt32, pointer: UInt32, recoveryxpub: String, csvBlocks: UInt32) -> Observable<String> {
        let params = [ "network": network, "subaccount": subaccount,
                       "branch": branch, "recovery_xpub": recoveryxpub,
                       "csv_blocks": csvBlocks
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
                return res["result"] as? Data
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

    func signLiquidTransaction(inputs: [[String: Any]], outputs: [[String: Any]], transactions: [String: String], addressTypes: [String]) -> Observable<LiquidHWResult> {

        if addressTypes.contains("p2pkh") {
            return Observable.error(JadeError.Abort("Hardware Wallet cannot sign sweep inputs"))
        }

        var values = [UInt32]()
        var abfs = [Data]()
        var vbfs = [Data]()
        var inputPrevouts = [Data]()

        let txInputs = inputs.map { input -> TxInputLiquid? in
            let swInput = !(input["address_type"] as? String == "p2sh")
            let script = hexToData(input["prevout_script"] as? String ?? "")
            let commitment = hexToData(input["commitment"] as? String ?? "")
            let userPath = input["user_path"] as? [UInt32]

            // Get values, abfs and vbfs from inputs (needed to compute the final output vbf)
            values.append(input["satoshi"] as? UInt32 ?? 0)
            abfs.append(hexToData(input["abf"] as? String ?? ""))
            vbfs.append(hexToData(input["vbf"] as? String ?? ""))

            let txhash = input["txhash"] as? String
            let txhex = transactions[txhash ?? ""]
            inputPrevouts.append(Data(hexToData(txhex!).reversed()))

            let ptIdx = input["ptIdx"] as? UInt
            inputPrevouts += [Data(ptIdx!.uint32BE())]

            return TxInputLiquid(isWitness: swInput, script: script, valueCommitment: commitment, path: userPath)
        }
        //SHA256.init()
        //flatten(inputPrevouts)
        //final byte[] hashPrevOuts = Wally.sha256d(flatten(inputPrevouts));

        return Observable.error(JadeError.Abort(""))
    }

    func flatten(_ list: [Data]) -> Data {
        guard !list.isEmpty else {
            return Data()
        }
        return list.reduce(Data(), { (a, b) in Data(a.encode() + b.encode()) })
    }

}
