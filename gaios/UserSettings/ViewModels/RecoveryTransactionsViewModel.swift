import Foundation
import UIKit
import gdk

class RecoveryTransactionsViewModel {

    var session: SessionManager

    init(session: SessionManager) {
        self.session = session
    }

    func getTwoFactorItemEmail() async throws -> TwoFactorItem? {
        if !session.logged {
            return nil
        }
        if let twoFactorConfig = try await session.loadTwoFactorConfig() {
            return TwoFactorItem(name: NSLocalizedString("id_email", comment: ""),
                                 enabled: twoFactorConfig.email.enabled,
                                 confirmed: twoFactorConfig.email.confirmed,
                                 maskedData: twoFactorConfig.email.data,
                                 type: TwoFactorType.email)
        }
        return nil
    }

    func setEmail(session: SessionManager, email: String, isSetRecovery: Bool) async throws {
        let config = TwoFactorConfigItem(enabled: isSetRecovery ? false : true, confirmed: true, data: email)
        try await session.changeSettingsTwoFactor(method: .email, config: config)
        _ = try await session.loadTwoFactorConfig()
    }
}
