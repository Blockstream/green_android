import Foundation
import UIKit
import PromiseKit
import RxSwift

class HWResolver {
    public static let shared = HWResolver()
    var hw: HWResolverProtocol?
    var noncesCache = [String: Any]()

    func resolveCode(action: String, device: [String: Any], requiredData: [String: Any]) -> Promise<String> {
        switch action {
        case "get_xpubs":
            return HWResolver.shared.getXpubs(requiredData)
        case "sign_message":
            return HWResolver.shared.signMessage(requiredData)
        case "sign_tx":
            return HWResolver.shared.signTransaction(requiredData)
        case "get_blinding_nonces":
            guard let scripts = requiredData["script"] as? [String],
                  let publicKeys = requiredData["public_keys"] as? [String] else {
                return Promise { $0.reject(GaError.GenericError) }
            }
            return HWResolver.shared.getBlindingNonces(scripts: scripts, publicKeys: publicKeys).compactMap {
                let data = try JSONSerialization.data(withJSONObject: ["nonces": $0], options: .fragmentsAllowed)
                return String(data: data, encoding: .utf8)
            }
        case "get_blinding_public_keys":
            guard let scripts = requiredData["scripts"] as? [String] else {
                return Promise { $0.reject(GaError.GenericError) }
            }
            return HWResolver.shared.getBlindingPublicKeys(scripts).compactMap {
                let data = try JSONSerialization.data(withJSONObject: ["public_keys": $0], options: .fragmentsAllowed)
                return String(data: data, encoding: .utf8)
            }
        default:
            return Promise { $0.reject(GaError.GenericError) }
        }
    }

    func getXpubs(_ json: [String: Any]) -> Promise<String> {
        return Promise { seal in
            let paths = json["paths"] as? [[Int]]
            _ = Observable.just(paths)
                .observeOn(SerialDispatchQueueScheduler(qos: .background))
                .flatMap { _ in self.hw!.xpubs(paths: paths!) }
                .takeLast(1)
                .subscribe(onNext: { data in
                    let xpubs = "{\"xpubs\":\(data.description)}"
                    seal.fulfill(xpubs)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func signMessage(_ json: [String: Any]) -> Promise<String> {
        return Promise { seal in
            let path = json["path"] as? [Int]
            let message = json["message"] as? String
            let useAeProtocol = json["use_ae_protocol"] as? Bool
            let aeHostCommitment = json["ae_host_commitment"] as? String
            let aeHostEntropy = json["ae_host_entropy"] as? String
            _ = Observable.just(message)
                .observeOn(SerialDispatchQueueScheduler(qos: .background))
                .flatMap { _ in self.hw!.signMessage(path: path, message: message, useAeProtocol: useAeProtocol, aeHostCommitment: aeHostCommitment, aeHostEntropy: aeHostEntropy) }
                .takeLast(1)
                .subscribe(onNext: { (signature, signatureCommitment) in
                    let result = ["signature": signature ?? "",
                     "signer_commitment": signatureCommitment ?? ""]
                    let data = try? JSONSerialization.data(withJSONObject: result, options: .fragmentsAllowed)
                    seal.fulfill(String(data: data ?? Data(), encoding: .utf8) ?? "")
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func signTransaction(_ json: [String: Any]) -> Promise<String> {
        return Promise { seal in
            let tx = json["transaction"] as? [String: Any]
            let signingInputs = json["signing_inputs"] as? [[String: Any]]
            let txOutputs = json["transaction_outputs"] as? [[String: Any]]
            let signingAddressTypes = json["signing_address_types"] as? [String]
            let signingTxs = json["signing_transactions"] as? [String: String]
            let useAeProtocol = json["use_ae_protocol"] as? Bool
            let isLiquid = getGdkNetwork(getNetwork()).liquid
            // Increment connection timeout for sign transaction command
            Ledger.shared.TIMEOUT = 120
            _ = Observable.just(self.hw!)
                .flatMap { hw -> Observable<[String: Any]> in
                    if isLiquid {
                        return hw.signLiquidTransaction(tx: tx!, inputs: signingInputs!, outputs: txOutputs!, transactions: signingTxs ?? [:], addressTypes: signingAddressTypes!, useAeProtocol: useAeProtocol ?? false)
                    }
                    var txinfo = tx
                    if let name = hw.info["name"] as? String,
                       let txhash = tx?["txhash"] as? String,
                       name == "Ledger" {
                        txinfo = try getSession().getTransactionDetails(txhash: txhash)
                    }
                    return hw.signTransaction(tx: txinfo!, inputs: signingInputs!, outputs: txOutputs!, transactions: signingTxs ?? [:], addressTypes: signingAddressTypes!)
                        .compactMap { res in
                            return ["signatures": res]
                        }
                }.subscribe(onNext: { res in
                    if let data = try?  JSONSerialization.data(withJSONObject: res, options: .fragmentsAllowed),
                       let text = String(data: data, encoding: String.Encoding.ascii) {
                        seal.fulfill(text)
                    }
                    Ledger.shared.TIMEOUT = 30
                }, onError: { err in
                    seal.reject(err)
                })
            }
    }

    func getBlindingNonce(pubkey: String, script: String) -> Promise<String> {
        return Promise { seal in
            _ = self.hw!.getSharedNonce(pubkey: pubkey, scriptHex: script)
                .subscribe(onNext: { data in
                    seal.fulfill(data!.description)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func getBlindingNonces(scripts: [String], publicKeys: [String]) -> Promise<[String]> {
        let promises = zip(scripts, publicKeys)
            .map { (script, publicKey) in { self.getBlindingNonce(pubkey: publicKey, script: script) }        }
        return Promise.chain(promises)
    }

    func getBlindingKey(script: String) -> Promise<String> {
        return Promise { seal in
            _ = self.hw!.getBlindingKey(scriptHex: script)
                .subscribe(onNext: { data in
                    seal.fulfill(data!.description)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }

    func getBlindingPublicKeys(_ scripts: [String]) -> Promise<[String]> {
        let promises = scripts.map { script in { self.getBlindingKey(script: script) } }
        return Promise.chain(promises)
    }

}
