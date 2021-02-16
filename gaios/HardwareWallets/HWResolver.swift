import Foundation
import UIKit
import PromiseKit
import RxSwift

class HWResolver {
    public static let shared = HWResolver()
    var hw: HWResolverProtocol?
    var noncesCache = [String: Any]()

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
            _ = Observable.just(message)
                .observeOn(SerialDispatchQueueScheduler(qos: .background))
                .flatMap { _ in self.hw!.signMessage(path: path!, message: message!) }
                .takeLast(1)
                .subscribe(onNext: { data in
                    let value = "{\"signature\":\"\(data.description)\"}"
                    seal.fulfill(value)
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
            // Increment connection timeout for sign transaction command
            Ledger.shared.TIMEOUT = 120
            _ = self.hw!.signTransaction(tx: tx!, inputs: signingInputs!, outputs: txOutputs!, transactions: signingTxs ?? [:], addressTypes: signingAddressTypes!)
                .takeLast(1)
                .subscribe(onNext: { data in
                    let value = "{\"signatures\":\(data.description)}"
                    seal.fulfill(value)
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

    func getBlindingNonces(_ json: [String: Any]) -> Promise<String> {
        let blindedScripts = json["blinded_scripts"] as? [[String: Any]]
        let promises: [()->Promise<String>] = blindedScripts!.map { bscript in
            return {
                let pubkey = bscript["pubkey"] as? String
                let script = bscript["script"] as? String
                return self.getBlindingNonce(pubkey: pubkey!, script: script!)
            }
        }
        return Promise.chain(promises).compactMap { res in
            return "{\"nonces\":\(res.description)}"
        }
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

    func getBlindingKeys(_ json: [String: Any]) -> Promise<String> {
        let tx = json["transaction"] as? [String: Any]
        let change = tx?["change_address"] as? [String: Any]
        let promises: [()->Promise<(String, String)>] = change!.map { change in
            return {
                let value = change.value as? [String: Any]
                let script = value?["blinding_script_hash"] as? String
                return self.getBlindingKey(script: script!).compactMap { res in
                    return (change.key, res)
                }
            }
        }
        return Promise.chain(promises).compactMap { list in
            var dict = [String: String]()
            list.forEach { dict[$0.0] = $0.1 }
            return "{\"blinding_keys\":\(dict.description)}"
        }
    }

}
