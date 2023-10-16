import Foundation
import AsyncBluetooth
import Combine
import greenaddress
import SwiftCBOR
import CommonCrypto

public protocol JadeGdkRequest: AnyObject {
    func httpRequest(params: [String: Any]) async -> [String: Any]?
    func urlValidation(urls: [String]) async -> Bool
}

public class BleJade: BleJadeCommands, HWProtocol {

    public static let MIN_ALLOWED_FW_VERSION = "0.1.24"
    public static let FW_SERVER_HTTPS = "https://jadefw.blockstream.com"
    public static let FW_SERVER_ONION = "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion"
    public static let PIN_SERVER_HTTPS = "https://jadepin.blockstream.com"
    public static let PIN_SERVER_ONION = "http://mrrxtq6tjpbnbm7vh5jt6mpjctn7ggyfy5wegvbeff3x7jrznqawlmid.onion"
    public static let FW_JADE_PATH = "/bin/jade/"
    public static let FW_JADEDEV_PATH = "/bin/jadedev/"
    public static let FW_JADE1_1_PATH = "/bin/jade1.1/"
    public static let FW_JADE1_1DEV_PATH = "/bin/jade1.1dev/"
    public static let BOARD_TYPE_JADE = "JADE"
    public static let BOARD_TYPE_JADE_V1_1 = "JADE_V1.1"
    public static let FEATURE_SECURE_BOOT = "SB"

    public weak var gdkRequestDelegate: JadeGdkRequest?

    public let blockstreamUrls = [
        BleJade.FW_SERVER_HTTPS,
        BleJade.FW_SERVER_ONION,
        BleJade.PIN_SERVER_HTTPS,
        BleJade.PIN_SERVER_ONION]

    public func httpRequest<T: Codable, K: Codable>(_ httpRequest: JadeHttpRequest<T>) async throws -> K {
        let isUrlSafe = httpRequest.params.urls.allSatisfy { url in !blockstreamUrls.filter { url.starts(with: $0) }.isEmpty }
        if !isUrlSafe {
            let validated = await gdkRequestDelegate?.urlValidation(urls: httpRequest.params.urls)
            if !(validated ?? false) {
                throw HWError.Abort("Unkwnown url")
            }
        }
        let httpResponse = await gdkRequestDelegate?.httpRequest(params: httpRequest.params.toDict() ?? [:])
        if let error = httpResponse?["error"] as? String {
            throw HWError.Abort(error)
        }
        let httpResponseBody = httpResponse?["body"] as? [String: Any]
        if let decoded = K.from(httpResponseBody ?? [:]) as? K {
            return decoded
        }
        throw HWError.Abort("id_action_canceled")
    }
    
    public func unlock(network: String) async throws -> Bool {
        // Send initial auth user request
        let epoch = Date().timeIntervalSince1970
        let cmd = JadeAuthRequest(network: network, epoch: UInt32(epoch))
        let res: JadeResponse<Bool> = try await exchange(JadeRequest<JadeAuthRequest>(method: "auth_user", params: cmd))
        guard let res = res.result else { throw HWError.Abort("Invalid pin") }
        return res
    }
    
    public func auth(network: String) async throws -> Bool {
        // Send initial auth user request
        let epoch = Date().timeIntervalSince1970
        let cmd1 = JadeAuthRequest(network: network, epoch: UInt32(epoch))
        let req1 = JadeRequest<JadeAuthRequest>(method: "auth_user", params: cmd1)
        let res1: JadeResponse<JadeAuthResponse<String>> = try await exchange(req1)
        guard let res1 = res1.result else { throw HWError.Abort("Invalid auth") }
        let httpRequest1: JadeHttpRequest<String> = res1.httpRequest
        let cmd2: JadeHandshakeInit = try await self.httpRequest(httpRequest1)
        let req2 = JadeRequest<JadeHandshakeInit>(method: httpRequest1.onReply, params: cmd2)
        let res2: JadeResponse<JadeAuthResponse<JadeHandshakeComplete>> = try await exchange(req2)
        guard let res2 = res2.result else { throw HWError.Abort("Invalid auth") }
        let httpRequest2: JadeHttpRequest<JadeHandshakeComplete> = res2.httpRequest
        let cmd3: JadeHandshakeCompleteReply = try await self.httpRequest(httpRequest2)
        let req3 = JadeRequest<JadeHandshakeCompleteReply>(method: httpRequest2.onReply, params: cmd3)
        let res3: JadeResponse<Bool> = try await exchange(req3)
        guard let res3 = res3.result else { throw HWError.Abort("Invalid auth") }
        return res3
    }
    
    public func xpubs(network: String, paths: [[Int]]) async throws -> [String] {
        var list = [String]()
        for path in paths {
            list += [try await xpubs(network: network, path: path)]
        }
        return list
    }
    
    public func signMessage(_ params: HWSignMessageParams) async throws -> HWSignMessageResult {
        let pathstr = getUnsignedPath(params.path)
        var result: HWSignMessageResult?
        if params.useAeProtocol ?? false {
            // Anti-exfil protocol:
            // We send the signing request with the host-commitment and receive the signer-commitment
            // in reply once the user confirms.
            // We can then request the actual signature passing the host-entropy.
            let msg = JadeSignMessage(message: params.message, path: pathstr, aeHostCommitment: params.aeHostCommitment?.hexToData())
            let signerCommitment = try await signMessage(msg).hex
            let cmd = JadeGetSignature(aeHostEntropy: params.aeHostEntropy?.hexToData() ?? Data())
            let signature = try await getSignature(cmd)
            result = HWSignMessageResult(signature: signature, signerCommitment: signerCommitment)
        } else {
            // Standard EC signature, simple case
            let msg = JadeSignMessage(message: params.message, path: pathstr, aeHostCommitment: nil)
            let cmd = try await signSimpleMessage(msg)
            result = HWSignMessageResult(signature: cmd, signerCommitment: nil)
        }
        // Convert the signature from Base64 into DER hex for GDK
        guard var sigDecoded = Data(base64Encoded: result?.signature ?? "") else {
            throw HWError.Abort("Invalid signature")
        }
        // Need to truncate lead byte if recoverable signature
        if sigDecoded.count == Wally.WALLY_EC_SIGNATURE_RECOVERABLE_LEN {
            sigDecoded = sigDecoded[1..<sigDecoded.count]
        }
        let sigDer = try Wally.sigToDer(sig: Array(sigDecoded))
        return HWSignMessageResult(signature: sigDer.hex, signerCommitment: result?.signerCommitment)
    }

    // swiftlint:disable:next function_parameter_count
    public func newReceiveAddress(chain: String, mainnet: Bool, multisig: Bool, chaincode: String?, recoveryPubKey: String?, walletPointer: UInt32?, walletType: String?, path: [UInt32], csvBlocks: UInt32) async throws -> String {
        
        if multisig {
            // Green Multisig Shield - pathlen should be 2 for subact 0, and 4 for subact > 0
            // In any case the last two entries are 'branch' and 'pointer'
            let pathlen = path.count
            let branch = path[pathlen - 2]
            let pointer = path[pathlen - 1]
            var recoveryxpub: String?
            if let chaincode = chaincode, !chaincode.isEmpty {
                recoveryxpub = try? Wally.bip32KeyFromParentToBase58(isMainnet: mainnet,
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
            return try await getReceiveAddress(params)
        } else {
            // Green Electrum Singlesig
            let variant = mapAddressType(walletType)
            let params = JadeGetReceiveSinglesigAddress(network: chain, path: path, variant: variant ?? "")
            return try await getReceiveAddress(params)
        }
    }

    func mapAddressType(_ addrType: String?) -> String? {
        switch addrType {
        case "p2pkh": return "pkh(k)"
        case "p2wpkh": return "wpkh(k)"
        case "p2sh-p2wpkh": return "sh(wpkh(k))"
        default: return nil
        }
    }
    
    func getBlindingFactors(index: Int, output: InputOutput, version: JadeVersionInfo, hashPrevouts: [UInt8]?) async throws -> (String, String ) {
        // Call Jade to get the blinding factors
        // NOTE: 0.1.48+ Jade fw accepts 'ASSET_AND_VALUE', and returns abf and vbf concatenated abf||vbf
        // (Previous versions need two calls, for 'ASSET' and 'VALUE' separately)
        // FIXME: remove when 0.1.48 is made minimum allowed version.
        if output.blindingKey == nil {
            return ("", "")
        } else if version.hasSwapSupport {
            let bfs = try await getBlindingFactor(JadeGetBlingingFactor(hashPrevouts: hashPrevouts?.data, outputIndex: index, type: "ASSET_AND_VALUE"))
            let assetblinder = bfs[0..<Wally.WALLY_BLINDING_FACTOR_LEN].reversed()
            let amountblinder = bfs[Wally.WALLY_BLINDING_FACTOR_LEN..<2*Wally.WALLY_BLINDING_FACTOR_LEN].reversed()
            return (Data(assetblinder).hex, Data(amountblinder).hex)
        } else {
            let abf = try await getBlindingFactor(JadeGetBlingingFactor(hashPrevouts: hashPrevouts?.data, outputIndex: index, type: "ASSET"))
            let vbf = try await getBlindingFactor(JadeGetBlingingFactor(hashPrevouts: hashPrevouts?.data, outputIndex: index, type: "VALUE"))
            return (Data(abf.reversed()).hex, Data(vbf.reversed()).hex)
        }
    }
    
    public func getBlindingFactors(params: HWBlindingFactorsParams) async throws -> HWBlindingFactorsResult {
        let version = try await version()
        // Compute hashPrevouts to derive deterministic blinding factors from
        let txhashes = params.usedUtxos.map { $0.getTxid ?? []}.lazy.joined()
        let outputIdxs = params.usedUtxos.map { $0.ptIdx }
        let hashPrevouts = Wally.getHashPrevouts(txhashes: [UInt8](txhashes), outputIdxs: outputIdxs)
        // Enumerate the outputs and provide blinding factors as needed
        // Assumes last entry is unblinded fee entry - assumes all preceding entries are blinded
        var factors = [(String, String)]()
        for item in params.transactionOutputs.enumerated() {
            let factor = try await getBlindingFactors(index: item.offset, output: item.element, version: version, hashPrevouts: hashPrevouts)
            factors += [factor]
        }
        return HWBlindingFactorsResult(assetblinders: factors.map { $0.0 }, amountblinders: factors.map { $0.1 })
    }

    func isSegwit(_ addrType: String?) -> Bool {
        return ["csv", "p2wsh", "p2wpkh", "p2sh-p2wpkh"].contains(addrType)
    }

    // Helper to get the change paths for auto-validation
    func getChangeData(outputs: [InputOutput]) -> [TxChangeOutput?] {
        return outputs.map { (out: InputOutput) -> TxChangeOutput? in
            if out.isChange ?? false == false {
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

    public func signTransaction(network: String, params: HWSignTxParams) async throws -> HWSignTxResponse {
        if params.signingInputs.isEmpty {
            throw HWError.Abort("Input transactions missing")
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
            throw HWError.Abort("Input transactions missing")
        }
        
        let changes = getChangeData(outputs: params.txOutputs)
        let signtx = JadeSignTx(change: changes,
                                network: network,
                                numInputs: params.signingInputs.count,
                                trustedCommitments: nil,
                                useAeProtocol: params.useAeProtocol,
                                txn: params.transaction?.hexToData() ?? Data())
        let res: JadeResponse<Bool> = try await exchange(JadeRequest(method: "sign_tx", params: signtx))
        if let result = res.result, !result {
            throw HWError.Abort("Invalid signature")
        }
        var (commitments, signatures) = ([""], [""])
        if params.useAeProtocol {
            (commitments, signatures) = try await self.signTxInputsAntiExfil(inputs: txInputs)
        } else {
            (commitments, signatures) = try await self.signTxInputs(inputs: txInputs)
        }
        return HWSignTxResponse(signatures: signatures, signerCommitments: commitments)
    }
    
    func signTxInputs(inputs: [TxInputProtocol?]) async throws -> (commitments: [String], signatures: [String]) {
        /**
         * Legacy Protocol:
         * Send one message per input - without expecting replies.
         * Once all n input messages are sent, the hw then sends all n replies
         * (as the user has a chance to confirm/cancel at this point).
         * Then receive all n replies for the n signatures.
         * NOTE: *NOT* a sequence of n blocking rpc calls.
         */
        var allReads = [String]()
        for input in inputs {
            if let input = input {
                try await signTxInput(input)
            }
        }
        for _ in inputs {
            if let buffer = try await read() {
#if DEBUG
                print("<= " + buffer.map { String(format: "%02hhx", $0) }.joined())
#endif
                let res = try CodableCBORDecoder().decode(JadeResponse<Data>.self, from: buffer)
                allReads += [res.result?.hex ?? ""]
            }
        }
        return (commitments: [], signatures: allReads)
    }

    func signTxInput(_ input: TxInputProtocol) async throws {
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
        try await self.write(encoded!)
    }

    func signTxInputsAntiExfil(inputs: [TxInputProtocol?]) async throws -> (commitments: [String], signatures: [String]) {
        /**
         * Anti-exfil protocol:
         * We send one message per input (which includes host-commitment *but
         * not* the host entropy) and receive the signer-commitment in reply.
         * Once all n input messages are sent, we can request the actual signatures
         * (as the user has a chance to confirm/cancel at this point).
         * We request the signatures passing the host-entropy for each one.
         */
        // Send inputs one at a time, receiving 'signer-commitment' in reply
        var signerCommitments = [String]()
        for input in inputs {
            if let inputBtc = input as? TxInputBtc {
                let res: JadeResponse<Data> = try await exchange(JadeRequest(method: "tx_input", params: inputBtc))
                signerCommitments += [res.result?.hex ?? ""]
            } else if let inputLiquid = input as? TxInputLiquid {
                let res: JadeResponse<Data> = try await exchange(JadeRequest(method: "tx_input", params: inputLiquid))
                signerCommitments += [res.result?.hex ?? ""]
            } else {
                throw HWError.Abort("")
            }
        }
        var signatures = [String]()
        for input in inputs {
            var aeHostEntropy: Data?
            if let inputBtc = input as? TxInputBtc {
                aeHostEntropy = inputBtc.aeHostEntropy
            } else if let inputLiquid = input as? TxInputLiquid {
                aeHostEntropy = inputLiquid.aeHostEntropy
            }
            if let aeHostEntropy = aeHostEntropy {
                let params = JadeGetSignature(aeHostEntropy: aeHostEntropy)
                let res: JadeResponse<Data> = try await exchange(JadeRequest(method: "get_signature", params: params))
                signatures += [res.result?.hex ?? ""]
            }
        }
        return (commitments: signerCommitments, signatures: signatures)
    }

    public func signLiquidTransaction(network: String, params: HWSignTxParams) async throws -> HWSignTxResponse {
        let version = try await version()
        // Load the tx into wally for legacy fw versions as will need it later
        // to access the output's asset[generator] and value[commitment].
        // NOTE: 0.1.48+ Jade fw does need these extra values passed explicitly so
        // no need to parse/load the transaction into wally.
        // FIXME: remove when 0.1.48 is made minimum allowed version.
        let wallytx = !version.hasSwapSupport ? Wally.txFromBytes(tx: params.transaction?.hexToBytes() ?? [], elements: true) : nil
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
            if let wallytx = wallytx, let asset = Wally.txGetOutputAsset(wallyTx: wallytx, index: res.offset) {
                commitment.assetGenerator = asset.data
            }
            if let wallytx = wallytx, let value = Wally.txGetOutputValue(wallyTx: wallytx, index: res.offset) {
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
                                txn: params.transaction?.hexToData() ?? Data())
        guard try await signLiquidTx(params: params) else {
            throw HWError.Abort("Invalid sign tx")
        }
        var (commitments, signatures) = ([""], [""])
        if params.useAeProtocol {
            (commitments, signatures) = try await signTxInputsAntiExfil(inputs: txInputs)
        } else {
            (commitments, signatures) = try await signTxInputs(inputs: txInputs)
        }
        return HWSignTxResponse(signatures: signatures, signerCommitments: commitments)
    }
}
// OTA functions
extension BleJade {
    
    // Check Jade fmw against minimum allowed firmware version
    public func isJadeFwValid(_ version: String) -> Bool {
        return BleJade.MIN_ALLOWED_FW_VERSION <= version
    }

    public func download(_ fwpath: String, base64: Bool = false) async -> [String: Any]? {
        let params: [String: Any] = [
            "method": "GET",
            "accept": base64 ? "base64": "json",
            "urls": ["\(BleJade.FW_SERVER_HTTPS)\(fwpath)",
                     "\(BleJade.FW_SERVER_ONION)\(fwpath)"] ]
        return await gdkRequestDelegate?.httpRequest(params: params)
    }

    public func firmwarePath(_ verInfo: JadeVersionInfo) -> String? {
        if ![BleJade.BOARD_TYPE_JADE, BleJade.BOARD_TYPE_JADE_V1_1].contains(verInfo.boardType) {
            return nil
        }
        let isV1BoardType = verInfo.boardType == BleJade.BOARD_TYPE_JADE
        // Alas the first version of the jade fmw didn't have 'BoardType' - so we assume an early jade.
        if verInfo.jadeFeatures.contains(BleJade.FEATURE_SECURE_BOOT) {
            // Production Jade (Secure-Boot [and flash-encryption] enabled)
            return isV1BoardType ? BleJade.FW_JADE_PATH : BleJade.FW_JADE1_1_PATH
        } else {
            // Unsigned/development/testing Jade
            return isV1BoardType ? BleJade.FW_JADEDEV_PATH : BleJade.FW_JADE1_1DEV_PATH
        }
    }

    var dev: Bool {
        return Bundle.main.bundleIdentifier == "io.blockstream.greendev"
    }

    public func firmwareData(_ verInfo: JadeVersionInfo) async throws -> Firmware {
        // Get relevant fmw path (or if hw not supported)
        guard let fwPath = firmwarePath(verInfo) else {
            throw HWError.Abort("Unsupported hardware")
        }
        guard let res = await download("\(fwPath)index.json"),
              let body = res["body"] as? [String: Any],
              let json = try? JSONSerialization.data(withJSONObject: body, options: []),
              let channels = try? JSONDecoder().decode(FirmwareChannels.self, from: json) else {
            throw HWError.Abort("Failed to fetch firmware index")
        }
        var images = [channels.stable?.delta, channels.stable?.full]
        if dev {
            images = [channels.beta?.delta, channels.beta?.full, channels.stable?.delta, channels.stable?.full]
        }
        for image in images {
            if let fmw = image?.filter({ $0.upgradable(verInfo.jadeVersion) }).first {
                return fmw
            }
        }
        throw HWError.Abort("No newer firmware found")
    }

    public func getBinary(_ verInfo: JadeVersionInfo, _ fmw: Firmware) async throws -> Data {
        guard let fwPath = firmwarePath(verInfo) else {
            throw HWError.Abort("Unsupported hardware")
        }
        if let res = await download("\(fwPath)\(fmw.filename)", base64: true),
            let body = res["body"] as? String,
            let data = Data(base64Encoded: body) {
            return data
        }
        throw HWError.Abort("Error downloading firmware file")
    }

    public func sha256(_ data: Data) -> Data {
        let length = Int(CC_SHA256_DIGEST_LENGTH)
        var hash = [UInt8](repeating: 0, count: length)
        data.withUnsafeBytes {
            _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &hash)
        }
        return Data(hash)
    }
    
    public func updateFirmware(version: JadeVersionInfo, firmware: Firmware, binary: Data) async throws -> Bool {
        let hash = sha256(binary)
        let cmd = JadeOta(fwsize: firmware.fwsize,
                          cmpsize: binary.count,
                          otachunk: version.jadeOtaMaxChunk,
                          cmphash: hash,
                          patchsize: firmware.patchSize)
        let _: JadeResponse<Bool> = try await exchange(JadeRequest(method: firmware.isDelta ? "ota" : "ota_delta", params: cmd))
        let _: Bool = try await otaSend(binary, size: binary.count, chunksize: version.jadeOtaMaxChunk)
        let completed: JadeResponse<Bool> = try await exchange(JadeRequest<JadeEmpty>(method: "ota_complete"))
        return completed.result ?? false
    }

    public func otaSend(_ data: Data, size: Int, chunksize: Int = 4 * 1024) async throws -> Bool {
        let chunks = data.chunked(into: chunksize)
        var completed = false
        for chunk in chunks {
            let res: JadeResponse<Bool> = try await exchange(JadeRequest(method: "ota_data", params: chunk))
            completed = res.result ?? false
        }
        return completed
    }
}
