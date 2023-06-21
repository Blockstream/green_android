import Foundation
import UIKit
import PromiseKit
import gdk

class RecoveryTransactionsViewModel {

    var session: SessionManager
    private let bgq = DispatchQueue.global(qos: .background)

    init(session: SessionManager) {
        self.session = session
    }

//    var twoFactorConfig: TwoFactorConfig?

    func getTwoFactorItemEmail() -> Promise<TwoFactorItem?> {
        if !session.logged {
            return Promise.value(nil)
        }
        return session.loadTwoFactorConfig()
            .map { twoFactorConfig in
//                self.twoFactorConfig = twoFactorConfig
                return TwoFactorItem(name: NSLocalizedString("id_email", comment: ""), enabled: twoFactorConfig.email.enabled && twoFactorConfig.email.confirmed, maskedData: twoFactorConfig.email.data, type: TwoFactorType.email)
            }
    }

    func setEmail(session: SessionManager, email: String, isSetRecovery: Bool) -> Promise<Void> {
        return Guarantee()
            .compactMap { TwoFactorConfigItem(enabled: isSetRecovery ? false : true, confirmed: true, data: email) }
            .then(on: bgq) { config in session.changeSettingsTwoFactor(method: .email, config: config) }
            .then(on: bgq) { _ in session.loadTwoFactorConfig() }
            .asVoid()
    }
}
