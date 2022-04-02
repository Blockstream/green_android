import Foundation
import UIKit
import PromiseKit
import RxSwift

class HWResolver {
    public static let shared = HWResolver()

    func resolveCode(action: String, device: HWDevice, requiredData: [String: Any]) -> Promise<String> {
        let hw: HWProtocol = device.isJade ? Jade.shared : Ledger.shared
        switch action {
        case "get_xpubs":
            guard let paths = requiredData["paths"] as? [[Int]] else {
                return Promise { $0.reject(GaError.GenericError) }
            }
            return HWResolver.shared.getXpubs(hw: hw, paths: paths).compactMap {
                let data = try JSONSerialization.data(withJSONObject: ["xpubs": $0], options: .fragmentsAllowed)
                return String(data: data, encoding: .utf8)
            }
        case "sign_message":
            return HWResolver.shared.signMessage(hw: hw, params: requiredData).compactMap {
                let data = try? JSONSerialization.data(withJSONObject: $0, options: .fragmentsAllowed)
                return String(data: data ?? Data(), encoding: .utf8)
            }
        case "sign_tx":
            return HWResolver.shared.signTransaction(hw: hw, params: requiredData).compactMap {
                let data = try? JSONSerialization.data(withJSONObject: $0, options: .fragmentsAllowed)
                return String(data: data ?? Data(), encoding: .utf8)
            }
        case "get_blinding_nonces":
            guard let scripts = requiredData["scripts"] as? [String],
                  let publicKeys = requiredData["public_keys"] as? [String] else {
                return Promise { $0.reject(GaError.GenericError) }
            }
            var output = [String: Any]()
            return HWResolver.shared.getBlindingNonces(hw: hw, scripts: scripts, publicKeys: publicKeys)
                .then { nonces -> Promise<[String]> in
                    output["nonces"] = nonces
                    if let blindingKeysRequired = requiredData["blinding_keys_required"] as? Bool,
                       blindingKeysRequired {
                        return HWResolver.shared.getBlindingPublicKeys(hw: hw, scripts: scripts)
                    }
                    return Promise<[String]> { seal in seal.fulfill([]) }
                }.compactMap { keys in
                    output["public_keys"] = keys
                    let data = try JSONSerialization.data(withJSONObject: output, options: .fragmentsAllowed)
                    return String(data: data, encoding: .utf8)
                }
        case "get_blinding_public_keys":
            guard let scripts = requiredData["scripts"] as? [String] else {
                return Promise { $0.reject(GaError.GenericError) }
            }
            return HWResolver.shared.getBlindingPublicKeys(hw: hw, scripts: scripts).compactMap {
                let data = try JSONSerialization.data(withJSONObject: ["public_keys": $0], options: .fragmentsAllowed)
                return String(data: data, encoding: .utf8)
            }
        case "get_master_blinding_key":
            return HWResolver.shared.getMasterBlindingKey(hw: hw).compactMap {
                let data = try JSONSerialization.data(withJSONObject: ["master_blinding_key": $0], options: .fragmentsAllowed)
                return String(data: data, encoding: .utf8)
            }
        default:
            return Promise { $0.reject(GaError.GenericError) }
        }
    }

    func getXpubs(hw: HWProtocol, paths: [[Int]]) -> Promise<[String]> {
        let network = AccountsManager.shared.current?.gdkNetwork
        return Promise { seal in
            _ = hw.xpubs(network: network?.chain ?? "mainnet", paths: paths)
                .subscribe(onNext: { data in
                    seal.fulfill(data)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func signMessage(hw: HWProtocol, params: [String: Any]) -> Promise<[String: Any]> {
        return Promise { seal in
            let path = params["path"] as? [Int]
            let message = params["message"] as? String
            let useAeProtocol = params["use_ae_protocol"] as? Bool
            let aeHostCommitment = params["ae_host_commitment"] as? String
            let aeHostEntropy = params["ae_host_entropy"] as? String
            _ = Observable.just(message)
                .observeOn(SerialDispatchQueueScheduler(qos: .background))
                .flatMap { _ in hw.signMessage(path: path, message: message, useAeProtocol: useAeProtocol, aeHostCommitment: aeHostCommitment, aeHostEntropy: aeHostEntropy) }
                .takeLast(1)
                .subscribe(onNext: { (signature, signatureCommitment) in
                    seal.fulfill(["signature": signature ?? "",
                                  "signer_commitment": signatureCommitment ?? ""])
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func signTransaction(hw: HWProtocol, params: [String: Any]) -> Promise<[String: Any]> {
        return Promise { seal in
            let tx = params["transaction"] as? [String: Any]
            let signingInputs = params["signing_inputs"] as? [[String: Any]]
            let txOutputs = params["transaction_outputs"] as? [[String: Any]]
            let signingTxs = params["signing_transactions"] as? [String: String]
            let useAeProtocol = params["use_ae_protocol"] as? Bool
            let network = AccountsManager.shared.current?.gdkNetwork
            // Increment connection timeout for sign transaction command
            Ledger.shared.TIMEOUT = 120
            _ = Observable.just(hw)
                .flatMap { hw -> Observable<[String: Any]> in
                    if network?.liquid ?? false {
                        return hw.signLiquidTransaction(network: network?.chain ?? "mainnet",
                                                        tx: tx!,
                                                        inputs: signingInputs!,
                                                        outputs: txOutputs!,
                                                        transactions: signingTxs ?? [:],
                                                        useAeProtocol: useAeProtocol ?? false)
                    }
                    return hw.signTransaction(network: network?.chain ?? "mainnet",
                                              tx: tx!,
                                              inputs: signingInputs!,
                                              outputs: txOutputs!,
                                              transactions: signingTxs ?? [:],
                                              useAeProtocol: useAeProtocol ?? false)
                }.subscribe(onNext: { res in
                    seal.fulfill(res)
                    Ledger.shared.TIMEOUT = 30
                }, onError: { err in
                    seal.reject(err)
                })
            }
    }

    func getBlindingNonce(hw: HWProtocol, pubkey: String, script: String) -> Promise<String> {
        return Promise { seal in
            _ = hw.getSharedNonce(pubkey: pubkey, scriptHex: script)
                .subscribe(onNext: { data in
                    seal.fulfill(data!.description)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func getBlindingNonces(hw: HWProtocol, scripts: [String], publicKeys: [String]) -> Promise<[String]> {
        let promises = zip(scripts, publicKeys)
            .map { (script, publicKey) in { self.getBlindingNonce(hw: hw, pubkey: publicKey, script: script) }        }
        return Promise.chain(promises)
    }

    func getBlindingKey(hw: HWProtocol, script: String) -> Promise<String> {
        return Promise { seal in
            _ = hw.getBlindingKey(scriptHex: script)
                .subscribe(onNext: { data in
                    seal.fulfill(data!.description)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func getBlindingPublicKeys(hw: HWProtocol, scripts: [String]) -> Promise<[String]> {
        let promises = scripts.map { script in { self.getBlindingKey(hw: hw, script: script) } }
        return Promise.chain(promises)
    }

    func getMasterBlindingKey(hw: HWProtocol) -> Promise<String> {
        return Promise { seal in
            _ = hw.getMasterBlindingKey()
                .subscribe(onNext: { data in
                    seal.fulfill(data.description)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }
}
