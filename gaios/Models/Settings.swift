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
    case SwitchNetwork
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
    case Pgp
    case Sweep
    case Version
    case TermsOfUse
    case PrivacyPolicy
}

// Type of priority of a fee for transaction
public enum TransactionPriority: Int {
    case Low = 24
    case Medium = 12
    case High = 3
    case Custom = 0

    var text: String {
        let string = [TransactionPriority.Low: "id_slow", TransactionPriority.Medium: "id_medium", TransactionPriority.High: "id_fast", TransactionPriority.Custom: "id_custom"][self]
        return NSLocalizedString(string ?? "", comment: "")
    }

    var time: String {
        let isLiquid = getGdkNetwork(getNetwork()).liquid
        let blocksPerHour = isLiquid ? 60 : 6
        let blocks = self.rawValue
        let n = (blocks % blocksPerHour) == 0 ? blocks / blocksPerHour : blocks * (60 / blocksPerHour)
        let time = NSLocalizedString((blocks % blocksPerHour) == 0 ? (blocks == blocksPerHour ? "id_hour" : "id_hours") : "id_minutes", comment: "")
        return String(format: "%d %@", n, time)
    }

    var description: String {
        let confirmationInBlocks = String(format: NSLocalizedString("id_confirmation_in_d_blocks", comment: ""), self.rawValue)
        return confirmationInBlocks + ", " + time + " " + NSLocalizedString("id_on_average", comment: "")
    }
}

// Bitcoin denomination type
public enum DenominationType: String, CodingKey {
    case BTC = "btc"
    case MilliBTC = "mbtc"
    case MicroBTC = "ubtc"
    case Bits = "bits"

    static let denominationsBTC: [DenominationType: String] = [ .BTC: "BTC", .MilliBTC: "mBTC", .MicroBTC: "µBTC", .Bits: "bits"]
    static let denominationsLBTC: [DenominationType: String] = [ .BTC: "L-BTC", .MilliBTC: "mL-BTC", .MicroBTC: "µL-BTC", .Bits: "L-bits"]

    var string: String {
        let isLiquid = getGdkNetwork(getNetwork()).liquid
        let denominations = isLiquid ? DenominationType.denominationsLBTC : DenominationType.denominationsBTC
        let denom = denominations.filter { $0.key == self }.first
        return denom!.value
    }

    static func from(_ string: String) -> DenominationType {
        let isLiquid = getGdkNetwork(getNetwork()).liquid
        let denominations = isLiquid ? DenominationType.denominationsLBTC : DenominationType.denominationsBTC
        let denom = denominations.filter { $0.value == string }.first
        return denom?.key ?? .BTC
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
        get {
            let denom = DenominationType.denominationsBTC.filter { $0.value == self.unit }.first
            return denom?.key ?? DenominationType.BTC
        }
        set {
            self.unit = DenominationType.denominationsBTC[newValue] ?? "BTC"
        }
    }

    var transactionPriority: TransactionPriority {
        get { return TransactionPriority(rawValue: self.requiredNumBlock) ?? .Medium}
        set { self.requiredNumBlock = newValue.rawValue}
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
