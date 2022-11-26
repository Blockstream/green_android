import Foundation
import PromiseKit

// Section of settings
enum USSection: String, Codable, CaseIterable {
    case Logout = "id_logout"
    case General = "id_general"
    case Security = "id_security"
    case Recovery = "id_recovery"
    case Multisig = "Multisig"
    case About = "id_about"
}

enum USItem: String, Codable, CaseIterable {
    case Logout = "id_logout"

    case BitcoinDenomination = "id_bitcoin_denomination"
    case ReferenceExchangeRate = "id_reference_exchange_rate"
    case ArchievedAccounts = "Archieved accounts"

    case ChangePin = "id_change_pin"
    case LoginWithBiometrics = "id_login_with_biometrics"
    case AutoLogout = "id_auto_logout_timeout"

    case BackUpRecoveryPhrase = "id_back_up_recovery_phrase"

    case Bitcoin = "Bitcoin"
    case Liquid = "Liquid"

    case Version = "id_version"
    case SupportID = "id_support"

    var string: String { NSLocalizedString(self.rawValue, comment: "") }
}

struct UserSettingsItem {
    var title: String
    var subtitle: String
    var section: USSection
    var type: USItem
    var switcher: Bool?
}
