import Foundation
import PromiseKit

// Section of settings
enum SettingsSections: String, Codable {
    case network = "Network"
    case account = "Account"
    case twoFactor = "Two Factor"
    case security = "Security"
    case advanced = "Advanced"
    case about = "About"
}

// Setting type in all sections
enum SettingsType: String, Codable {
    case SetupPin
    case WatchOnly
    case Logout
    case BitcoinDenomination = "unit"
    case ReferenceExchangeRate = "pricing"
    case DefaultTransactionPriority = "required_num_blocks"
    case DefaultCustomFeeRate = "customFeeRate"
    case SetupTwoFactor
    case ThresholdTwoFactor
    case LockTimeRecovery
    case LockTimeRequest
    case ResetTwoFactor
    case DisputeTwoFactor
    case CancelTwoFactor
    case Mnemonic
    case Autolock
    case Version
    case TermsOfUse
    case PrivacyPolicy
}

// Type of priority of a fee for transaction
public enum TransactionPriority: String {
    case Low = "Low"
    case Medium = "Medium"
    case High = "High"
    case Custom = "Custom"

    init(_ numBlock: Int) {
        switch numBlock {
        case 24:
            self = .Low
        case 12:
            self = .Medium
        case 3:
            self = .High
        default:
            self = .Custom
        }
    }

    func toNumBlock() -> Int {
        switch self {
        case .Low:
            return 24
        case .Medium:
            return 12
        case .High:
            return 3
        default :
            return 0
        }
    }
}

// Bitcoin denomination type
public enum DenominationType: String, CodingKey {
    case BTC = "btc"
    case MilliBTC = "mbtc"
    case MicroBTC = "ubtc"
    case Bits = "bits"

    func toString() -> String {
        switch self {
        case .BTC:
            return "BTC"
        case .MilliBTC:
            return "mBTC"
        case .MicroBTC:
            return "µBTC"
        case .Bits:
            return "bits"
        }
    }

    static func fromString(_ value: String) -> DenominationType {
        switch value.lowercased() {
        case "btc":
            return .BTC
        case "mbtc":
            return .MilliBTC
        case "µbtc", "ubtc":
            return .MicroBTC
        default:
            return .Bits
        }
    }
}

// Autolock type in time unit
public enum AutoLockType: Int {
    case minute = 1
    case twoMinutes = 2
    case fiveMinutes = 5
    case tenMinutes = 10

    func toString() -> String {
        let number = String(format: "%d", self.rawValue)
        let localized = NSLocalizedString(self == .minute ? "id_minute" : "id_minutes", comment: "")
        return "\(number) \(localized)"
    }

    static func fromString(_ value: String) -> AutoLockType {
        switch value {
        case AutoLockType.minute.toString():
            return .minute
        case AutoLockType.twoMinutes.toString():
            return .twoMinutes
        case AutoLockType.fiveMinutes.toString():
            return .fiveMinutes
        default:
            return tenMinutes
        }
    }
}

// Screenlock time type
enum ScreenLockType: Int {
    case None = 0
    case Pin = 1
    case TouchID = 2
    case FaceID = 3
    case All = 4

    func toString() -> String? {
        switch self {
        case .None:
            return "None"
        case .Pin:
            return "Pin"
        case .TouchID:
            return "Touch ID"
        case .FaceID:
            return "Face ID"
        default:
            return ""
        }
    }
}

// Setting item
struct SettingsItem {
    var title: String
    var subtitle: String
    var section: SettingsSections
    var type: SettingsType
}

struct SettingsNotifications: Codable {
    enum CodingKeys: String, CodingKey {
        case emailIncoming = "email_incoming"
        case emailOutgoing = "email_outgoing"
    }
    var emailIncoming: Bool
    var emailOutgoing: Bool
}

// Main setting
class Settings: Codable {
    enum CodingKeys: String, CodingKey {
        case requiredNumBlock = "required_num_blocks"
        case altimeout = "altimeout"
        case unit = "unit"
        case pricing = "pricing"
        case customFeeRate = "custom_fee_rate"
        case pgp = "pgp"
        case sound = "sound"
        case notifications = "notifications"
    }

    var requiredNumBlock: Int
    var altimeout: Int
    var unit: String
    var pricing: [String: String]
    var customFeeRate: UInt64?
    var pgp: String?
    var sound: Bool
    var notifications: SettingsNotifications?

    var denomination: DenominationType {
        get { return DenominationType.fromString(self.unit)}
        set { self.unit = newValue.toString()}
    }

    var transactionPriority: TransactionPriority {
        get { return TransactionPriority(self.requiredNumBlock)}
        set { self.requiredNumBlock = newValue.toNumBlock()}
    }

    var autolock: AutoLockType {
        get { return AutoLockType.init(rawValue: self.altimeout) ?? .fiveMinutes}
        set { self.altimeout = newValue.rawValue}
    }

    func getCurrency() -> String {
        return self.pricing["currency"]!
    }

    func getScreenLock() -> ScreenLockType {
        let network = getNetwork()
        let bioData = AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
        let pinData = AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: network)
        if pinData && bioData {
            return .All
        } else if bioData {
            let biometryType = AuthenticationTypeHandler.biometryType
            return biometryType == .faceID ? .FaceID : .TouchID
        } else if pinData {
            return .Pin
        } else {
            return .None
        }
    }
}
