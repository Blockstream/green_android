import Foundation
import PromiseKit

enum MSItem: String, Codable, CaseIterable {
    case WatchOnly = "id_watchonly_login"
    case TwoFactorAuthentication = "id_twofactor_authentication"
    case RecoveryTransactions = "id_recovery_transactions"
    case Pgp = "id_pgp_key"

    var string: String { NSLocalizedString(self.rawValue, comment: "") }
}

struct MultisigSettingsItem {
    var title: String
    var subtitle: String
    var type: MSItem
}
