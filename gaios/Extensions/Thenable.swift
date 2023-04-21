import Foundation
import PromiseKit
import gdk
import greenaddress

extension Thenable {

    // extend PromiseKit::Thenable to handle exceptions
    func tapLogger(on: DispatchQueue? = conf.Q.return, flags: DispatchWorkItemFlags? = nil) -> Promise<T> {
        return tap(on: on, flags: flags) {
            switch $0 {
            case .rejected(let error):
                switch error {
                case TwoFactorCallError.failure(let txt), TwoFactorCallError.cancel(let txt):
                    AnalyticsManager.shared.recordException(txt)
                case GaError.GenericError(let txt), GaError.TimeoutError(let txt), GaError.SessionLost(let txt), GaError.NotAuthorizedError(let txt):
                    AnalyticsManager.shared.recordException(txt ?? "")
                default:
                    break
                }
            default:
                break
            }
        }
    }

    func thenIf<U: Thenable>(on: DispatchQueue? = conf.Q.map, flags: DispatchWorkItemFlags? = nil, _ condition: Bool, _ body: @escaping(T) throws -> U) -> Promise<Void> {
        condition ? self.then(on: on, flags: flags, body).asVoid() : Promise().asVoid()
    }
}

extension Promise {
    static func chain<T, S>(_ datas: [T], _ concurrency: Int, _ iteratorHandle: @escaping (T) -> Promise<S>?) -> Promise<[S]> {
        var generator = datas.makeIterator()
        let iterator = AnyIterator<Promise<S>> {
            guard let next = generator.next() else {
                return nil
            }
            return iteratorHandle(next)
        }
        
        return when(fulfilled: iterator, concurrently: concurrency)
    }
}
