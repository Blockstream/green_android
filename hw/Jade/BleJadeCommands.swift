import Foundation
import AsyncBluetooth
import Combine
import SwiftCBOR

public class BleJadeCommands: BleJadeConnection {
    
    public func version() async throws -> JadeVersionInfo {
        let res: JadeResponse<JadeVersionInfo> = try await exchange(JadeRequest<JadeEmpty>(method: "get_version_info"))
        guard let res = res.result else { throw HWError.Abort("Invalid response") }
        return res
    }
    
    public func addEntropy() async throws -> Bool {
        let buffer = [UInt8](repeating: 0, count: 32).map { _ in UInt8(arc4random_uniform(0xff))}
        let cmd = JadeAddEntropy(entropy: Data(buffer))
        let res: JadeResponse<Bool> = try await exchange(JadeRequest<JadeAddEntropy>(method: "add_entropy", params: cmd))
        guard let res = res.result else { throw HWError.Abort("Invalid response") }
        return res
    }
    
    public func xpubs(network: String, path: [Int]) async throws -> String {
        let cmd = JadeGetXpub(network: network, path: getUnsignedPath(path))
        let res: JadeResponse<String> = try await exchange(JadeRequest<JadeGetXpub>(method: "get_xpub", params: cmd))
        guard let res = res.result else { throw HWError.Abort("Invalid response") }
        return res
    }
    
    // Get blinding key for script
    public func getBlindingKey(scriptHex: String) async throws -> String {
        let cmd = JadeGetBlindingKey(scriptHex: scriptHex)
        let res: JadeResponse<Data> = try await exchange(JadeRequest<JadeGetBlindingKey>(method: "get_blinding_key", params: cmd))
        guard let res = res.result?.hex else { throw HWError.Abort("Invalid response") }
        return res
    }
    
    public func getSharedNonce(pubkey: String, scriptHex: String) async throws -> String {
        let cmd = JadeGetSharedNonce(scriptHex: scriptHex, theirPubkeyHex: pubkey)
        let res: JadeResponse<Data> = try await exchange(JadeRequest<JadeGetSharedNonce>(method: "get_shared_nonce", params: cmd))
        guard let res = res.result?.hex else { throw HWError.Abort("Invalid response") }
        return res
    }
    
    public func getBlindingFactor(_ params: JadeGetBlingingFactor) async throws -> Data {
        let res: JadeResponse<Data> = try await exchange(JadeRequest(method: "get_blinding_factor", params: params))
        guard let res = res.result else { throw HWError.Abort("Invalid response") }
        return res
    }
    
    public func getMasterBlindingKey() async throws -> String {
        let res: JadeResponse<Data> = try await exchange(JadeRequest<JadeEmpty>(method: "get_master_blinding_key", params: nil))
        guard let res = res.result?.hex else { throw HWError.Abort("Invalid response") }
        return res
    }
    
    public func signLiquidTx(params: JadeSignTx) async throws -> Bool {
        let res: JadeResponse<Bool> = try await exchange(JadeRequest<JadeSignTx>(method: "sign_liquid_tx", params: params))
        guard let res = res.result else { throw HWError.Abort("Error response from initial sign_liquid_tx call: \(res.error?.message ?? "")") }
        return res
    }
    
    public func getReceiveAddress(_ params: JadeGetReceiveMultisigAddress) async throws -> String {
        let res: JadeResponse<String> = try await exchange(JadeRequest<JadeGetReceiveMultisigAddress>(method: "get_receive_address", params: params))
        guard let res = res.result else { throw HWError.Abort("Invalid response") }
        return res
    }

    public func getReceiveAddress(_ params: JadeGetReceiveSinglesigAddress) async throws -> String {
        let res: JadeResponse<String> = try await exchange(JadeRequest<JadeGetReceiveSinglesigAddress>(method: "get_receive_address", params: params))
        guard let res = res.result else { throw HWError.Abort("Invalid response") }
        return res
    }

    public func signMessage(_ params: JadeSignMessage) async throws -> Data {
        let res: JadeResponse<Data> = try await exchange(JadeRequest<JadeSignMessage>(method: "sign_message", params: params))
        guard let res = res.result else { throw HWError.Abort("Invalid response") }
        return res
    }

    public func getSignature(_ params: JadeGetSignature) async throws -> String {
        let res: JadeResponse<String> = try await exchange(JadeRequest<JadeGetSignature>(method: "get_signature", params: params))
        guard let res = res.result else { throw HWError.Abort("Invalid response") }
        return res
    }

    func exchange<T: Decodable, K: Decodable>(_ request: JadeRequest<T>) async throws -> JadeResponse<K> {
#if DEBUG
        print("=> \(request)")
#endif
        guard let buffer = request.encoded else { throw HWError.Abort("Invalid message") }
        let res = try await exchange(buffer)
        let response = try CodableCBORDecoder().decode(JadeResponse<K>.self, from: res)
#if DEBUG
        print("<= \(response)")
#endif
        if let error = response.error {
            throw HWError.from(code: error.code, message: error.message)
        }
        return response
    }
}
