import Foundation
import PromiseKit

public struct TwoFactorReset: Codable {
    enum CodingKeys: String, CodingKey {
        case isResetActive = "is_active"
        case isDisputeActive = "is_disputed"
        case daysRemaining = "days_remaining"
    }
    public let isResetActive: Bool
    public let isDisputeActive: Bool
    public let daysRemaining: Int
}

public struct TwoFactorConfigItem: Codable {
    public enum CodingKeys: String, CodingKey {
        case enabled
        case confirmed
        case data
    }
    public let enabled: Bool
    public let confirmed: Bool
    public let data: String

    public init(enabled: Bool, confirmed: Bool, data: String) {
        self.enabled = enabled
        self.confirmed = confirmed
        self.data = data
    }
}

public struct TwoFactorConfigLimits: Codable {
    public enum CodingKeys: String, CodingKey {
        case isFiat = "is_fiat"
        case fiat = "fiat"
        case fiatCurrency = "fiat_currency"
        case btc = "btc"
        case bits = "bits"
        case mbtc = "mbtc"
        case ubtc = "ubtc"
        case sats = "sats"
    }
    public let isFiat: Bool
    public let fiat: String?
    public let fiatCurrency: String?
    public let btc: String?
    public let bits: String?
    public let mbtc: String?
    public let ubtc: String?
    public let sats: String?

    public func get<T>(_ key: CodingKeys) -> T? {
        let value = Mirror(reflecting: self).children.filter { $0.label == key.rawValue }.map { return $0.value as? T }
        return value.first ?? nil
    }
}

public enum TwoFactorType: String, Codable {
    case email
    case phone
    case sms
    case gauth
}

public struct TwoFactorConfig: Codable {
    enum CodingKeys: String, CodingKey {
        case email = "email"
        case phone = "phone"
        case sms = "sms"
        case gauth = "gauth"
        case anyEnabled = "any_enabled"
        case allMethods = "all_methods"
        case enableMethods = "enabled_methods"
        case limits = "limits"
        case twofactorReset = "twofactor_reset"
    }
    public let anyEnabled: Bool
    public let allMethods: [String]
    public let enableMethods: [String]
    public let limits: TwoFactorConfigLimits
    public let email: TwoFactorConfigItem
    public let phone: TwoFactorConfigItem
    public let sms: TwoFactorConfigItem
    public let gauth: TwoFactorConfigItem
    public let twofactorReset: TwoFactorReset

    public func gauthSecret() -> String? {
        return URL(string: gauth.data)!.queryItems["secret"]
    }
}

public struct TwoFactorItem: Codable {
    public init(name: String, enabled: Bool, maskedData: String? = nil, type: TwoFactorType) {
        self.name = name
        self.enabled = enabled
        self.maskedData = maskedData
        self.type = type
    }
    
    enum CodingKeys: String, CodingKey {
        case name = "name"
        case enabled
        case maskedData
        case type
    }
    public var name: String
    public var enabled: Bool
    public var maskedData: String?
    public var type: TwoFactorType
}
