import Foundation
import UIKit
import PromiseKit
import RxSwift

class LedgerResolver {

    func getXpubs(_ json: [String: Any]) -> Promise<String> {
        return Promise { seal in
            let paths = json["paths"] as? [[Int]]
            _ = Observable.just(paths)
                .observeOn(SerialDispatchQueueScheduler(qos: .background))
                .flatMap { _ in Ledger.shared.xpubs(paths: paths!) }
                .flatMapLatest { data -> Observable<String> in
                    let xpubs = "{\"xpubs\":\(data.description)}"
                    seal.fulfill(xpubs)
                    return Observable.just(xpubs)
                }.subscribe(onError: { err in
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
                .flatMap { _ in Ledger.shared.signMessage(path: path!, message: message!) }
                .flatMapLatest { data -> Observable<String> in
                    let value = "{\"signature\":\"\(data.description)\"}"
                    seal.fulfill(value)
                    return Observable.just(value)
                }.subscribe(onError: { _ in
                    seal.reject(LedgerWrapper.LedgerError.IOError)
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
            _ = Observable.just(json)
                .observeOn(SerialDispatchQueueScheduler(qos: .background))
                .flatMap { _ in Ledger.shared.signTransaction(tx: tx!, inputs: signingInputs!, outputs: txOutputs!, transactions: signingTxs ?? [:], addressTypes: signingAddressTypes!)
                }.flatMapLatest { data -> Observable<String> in
                    let value = "{\"signatures\":\(data.description)}"
                    seal.fulfill(value)
                    return Observable.just(value)
                }.subscribe(onNext: { _ in
                    Ledger.shared.TIMEOUT = 30
                }, onError: { _ in
                    seal.reject(LedgerWrapper.LedgerError.IOError)
                })
        }
    }
}
