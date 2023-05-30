import Foundation
import RxSwift
import RxBluetoothKit
import CoreBluetooth
import SwiftCBOR
import greenaddress

public class JadeCommands: JadeChannel {

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

    public func xpubs(network: String, path: [Int]) -> Observable<String> {
        let cmd = JadeGetXpub(network: network, path: getUnsignedPath(path))
        return exchange(JadeRequest<JadeGetXpub>(method: "get_xpub", params: cmd))
            .compactMap { (res: JadeResponse<String>) -> String in
                res.result!
            }
    }

    // Get blinding key for script
    public func getBlindingKey(scriptHex: String) -> Observable<String?> {
        return exchange(JadeRequest(method: "get_blinding_key", params: JadeGetBlindingKey(scriptHex: scriptHex)))
            .compactMap { (res: JadeResponse<Data>) -> String? in
                return res.result?.hex
            }
    }

    public func getSharedNonce(pubkey: String, scriptHex: String) -> Observable<String?> {
        return exchange(JadeRequest(method: "get_shared_nonce", params: JadeGetSharedNonce(scriptHex: scriptHex, theirPubkeyHex: pubkey)))
            .compactMap { (res: JadeResponse<Data>) -> String? in
                return res.result?.hex
            }
    }

    public func getBlindingFactor(_ params: JadeGetBlingingFactor) -> Observable<Data> {
        return exchange(JadeRequest(method: "get_blinding_factor", params: params))
            .compactMap { (res: JadeResponse<Data>) -> Data in
                return res.result!
            }
    }

    public func getMasterBlindingKey() -> Observable<String> {
        return exchange(JadeRequest<JadeEmpty>(method: "get_master_blinding_key", params: nil))
            .compactMap { (res: JadeResponse<Data>) -> String in
                dataToHex(res.result!)
            }
    }

    public func signLiquidTx(params: JadeSignTx) -> Observable<Bool> {
        return exchange(JadeRequest<JadeSignTx>(method: "sign_liquid_tx", params: params))
            .compactMap { (res: JadeResponse<Bool>) -> Bool in
                if let result = res.result, !result {
                    throw HWError.Abort("Error response from initial sign_liquid_tx call: \(res.error?.message ?? "")")
                }
                return res.result ?? false
            }
    }

    public func getReceiveAddress(_ params: JadeGetReceiveMultisigAddress) -> Observable<String> {
        return exchange(JadeRequest<JadeGetReceiveMultisigAddress>(method: "get_receive_address", params: params))
            .compactMap { (res: JadeResponse<String>) -> String in
                res.result ?? ""
            }
    }

    public func getReceiveAddress(_ params: JadeGetReceiveSinglesigAddress) -> Observable<String> {
        return exchange(JadeRequest<JadeGetReceiveSinglesigAddress>(method: "get_receive_address", params: params))
            .compactMap { (res: JadeResponse<String>) -> String in
                res.result ?? ""
            }
    }

    public func signMessage(_ params: JadeSignMessage) -> Observable<Data> {
        return exchange(JadeRequest<JadeSignMessage>(method: "sign_message", params: params))
            .compactMap { (res: JadeResponse<Data>) -> Data in
                res.result ?? Data()
            }
    }

    public func getSignature(_ params: JadeGetSignature) -> Observable<String> {
        return exchange(JadeRequest<JadeGetSignature>(method: "get_signature", params: params))
            .compactMap { (res: JadeResponse<String>) -> String in
                res.result ?? ""
            }
    }
}
