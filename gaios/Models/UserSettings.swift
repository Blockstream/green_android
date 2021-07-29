import Foundation
import PromiseKit

// Section of settings
enum UserSettingsSections: String, Codable, CaseIterable {
    case logout = ""
    case general = "General"
    case security = "Security"
    case recovery = "Recovery"
    case advanced = "Advanced"
    case about = "About"

    static func name(_ section: UserSettingsSections) -> String {
        switch section {
        case .logout:
            return ""
        case .general:
            return localize("id_general")
        case .security:
            return localize("id_security")
        case .recovery:
            return localize("id_recovery")
        case .advanced:
            return localize("id_advanced")
        case .about:
            return localize("id_about")
        }
    }

    private static func localize(_ name: String) -> String {
        return NSLocalizedString(name, comment: "").lowercased().capitalized
    }
}

// Setting type in all sections
enum UserSettingsType: String, Codable {

    case Logout

    case WatchOnly
    case BitcoinDenomination = "unit"
    case ReferenceExchangeRate = "pricing"
    case DefaultTransactionPriority
    case DefaultCustomFeeRate

    case ChangePin
    case LoginWithBiometrics
    case AutoLogout
    case TwoFactorAuthentication

    case BackUpRecoveryPhrase
    case RecoveryTransactions

    case Pgp
    case Sweep

    case Version
    case TermsOfUse
    case PrivacyPolicy
}

struct UserSettingsItem {
    var title: String
    var subtitle: String
    var section: UserSettingsSections
    var type: UserSettingsType
}
