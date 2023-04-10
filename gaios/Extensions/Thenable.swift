import Foundation
import PromiseKit
import gdk

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
}
