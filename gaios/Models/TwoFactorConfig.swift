import Foundation
import UIKit
import PromiseKit

struct TwoFactorReset: Codable {
    enum CodingKeys: String, CodingKey {
        case isResetActive = "is_active"
        case isDisputeActive = "is_disputed"
        case daysRemaining = "days_remaining"
    }
    let isResetActive: Bool
    let isDisputeActive: Bool
    let daysRemaining: Int
}

struct TwoFactorConfigItem: Codable {
    enum CodingKeys: String, CodingKey {
        case enabled
        case confirmed
        case data
    }
    let enabled: Bool
    let confirmed: Bool
    let data: String
}

struct TwoFactorConfigLimits: Codable {
    enum CodingKeys: String, CodingKey {
        case isFiat = "is_fiat"
        case fiat = "fiat"
        case btc = "btc"
        case bits = "bits"
        case mbtc = "mbtc"
        case ubtc = "ubtc"
    }
    let isFiat: Bool
    let fiat: String
    let btc: String
    let bits: String
    let mbtc: String
    let ubtc: String

    func get<T>(_ key: CodingKeys) -> T? {
        let value = Mirror(reflecting: self).children.filter { $0.label == key.rawValue }.map { return $0.value as? T }
        return value.first ?? nil
    }
}

enum TwoFactorType: String {
    case email
    case phone
    case sms
    case gauth
}

struct TwoFactorConfig: Codable {
    enum CodingKeys: String, CodingKey {
        case email = "email"
        case phone = "phone"
        case sms = "sms"
        case gauth = "gauth"
        case anyEnabled = "any_enabled"
        case allMethods = "all_methods"
        case enableMethods = "enabled_methods"
        case limits = "limits"
    }
    let anyEnabled: Bool
    let allMethods: [String]
    let enableMethods: [String]
    let limits: TwoFactorConfigLimits
    let email: TwoFactorConfigItem
    let phone: TwoFactorConfigItem
    let sms: TwoFactorConfigItem
    let gauth: TwoFactorConfigItem

    func gauthSecret() -> String? {
        return URL(string: gauth.data)!.queryItems["secret"]
    }
}
