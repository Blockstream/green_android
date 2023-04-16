import Foundation
import greenaddress

public struct WalletIdentifier: Codable {
    enum CodingKeys: String, CodingKey {
        case walletHashId = "wallet_hash_id"
        case xpubHashId = "xpub_hash_id"
    }
    public let walletHashId: String
    public let xpubHashId: String
}

public struct SystemMessage: Codable {
    enum CodingKeys: String, CodingKey {
        case text
        case network
    }
    public let text: String
    public let network: String

    public init(text: String,
                network: String) {
        self.text = text
        self.network = network
    }
}

public struct TwoFactorResetMessage: Codable {
    public enum CodingKeys: String, CodingKey {
        case twoFactorReset
        case network
    }
    public let twoFactorReset: TwoFactorReset
    public let network: String
    public init(twoFactorReset: TwoFactorReset, network: String) {
        self.twoFactorReset = twoFactorReset
        self.network = network
    }
}

public struct DecryptWithPinParams: Codable {
    enum CodingKeys: String, CodingKey {
        case pin
        case pinData  = "pin_data"
    }
    public let pin: String
    public let pinData: PinData

    public init(pin: String, pinData: PinData) {
        self.pin = pin
        self.pinData = pinData
    }
}

public struct EncryptWithPinParams: Codable {
    enum CodingKeys: String, CodingKey {
        case pin
        case plaintext
    }
    public let pin: String
    public let plaintext: [String: String]

    public init(pin: String, plaintext: [String : String]) {
        self.pin = pin
        self.plaintext = plaintext
    }
}

public struct EncryptWithPinResult: Codable {
    enum CodingKeys: String, CodingKey {
        case pinData = "pin_data"
    }
    public let pinData: PinData
}

public struct LoginUserResult: Codable {
    enum CodingKeys: String, CodingKey {
        case xpubHashId = "xpub_hash_id"
        case walletHashId = "wallet_hash_id"
    }
    public let xpubHashId: String
    public let walletHashId: String
}

public struct GetSubaccountsParams: Codable {
    enum CodingKeys: String, CodingKey {
        case refresh
    }
    public let refresh: Bool

    public init(refresh: Bool) {
        self.refresh = refresh
    }
}

public struct GetSubaccountsResult: Codable {
    enum CodingKeys: String, CodingKey {
        case subaccounts
    }
    public let subaccounts: [WalletItem]
}

public struct GetSubaccountParams: Codable {
    enum CodingKeys: String, CodingKey {
        case pointer
    }
    public let pointer: UInt32
}

public struct GetAssetsParams: Codable {
    enum CodingKeys: String, CodingKey {
        case assetsId = "assets_id"
    }
    public let assetsId: [String]

    public init(assetsId: [String]) {
        self.assetsId = assetsId
    }
}

public struct GetAssetsResult: Codable {
    public let assets: [String: AssetInfo]
    public let icons: [String: String]
}

public struct CreateSubaccountParams: Codable {
    enum CodingKeys: String, CodingKey {
        case name = "name"
        case type = "type"
        case recoveryMnemonic = "recovery_mnemonic"
        case recoveryXpub = "recovery_xpub"
    }

    public let name: String
    public let type: AccountType
    public let recoveryMnemonic: String?
    public let recoveryXpub: String?

    public init(name: String, type: AccountType, recoveryMnemonic: String? = nil, recoveryXpub: String? = nil) {
        self.name = name
        self.type = type
        self.recoveryMnemonic = recoveryMnemonic
        self.recoveryXpub = recoveryXpub
    }
}

public struct GdkInit: Codable {
    enum CodingKeys: String, CodingKey {
        case datadir
        case tordir
        case registrydir
        case logLevel = "log_level"
    }
    public let datadir: String?
    public let tordir: String?
    public let registrydir: String?
    public let logLevel: String

    public static func defaults() -> GdkInit {
        let appSupportDir = try? FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        let cacheDir = try? FileManager.default.url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        var logLevel = "none"
#if DEBUG
        logLevel = "info"
#endif
        return GdkInit(datadir: appSupportDir?.path,
                       tordir: cacheDir?.path,
                       registrydir: cacheDir?.path,
                       logLevel: logLevel)
    }

    public func run() {
        try? gdkInit(config: self.toDict() ?? [:])
    }
}
