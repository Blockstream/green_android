import Foundation

struct WalletIdentifier: Codable {
    enum CodingKeys: String, CodingKey {
        case walletHashId = "wallet_hash_id"
        case xpubHashId = "xpub_hash_id"
    }
    let walletHashId: String
    let xpubHashId: String
}

struct SystemMessage: Codable {
    enum CodingKeys: String, CodingKey {
        case text
        case network
    }
    let text: String
    let network: String
}

struct TwoFactorResetMessage: Codable {
    enum CodingKeys: String, CodingKey {
        case twoFactorReset
        case network
    }
    let twoFactorReset: TwoFactorReset
    let network: String
}

struct DecryptWithPinParams: Codable {
    enum CodingKeys: String, CodingKey {
        case pin
        case pinData  = "pin_data"
    }
    let pin: String
    let pinData: PinData
}

struct EncryptWithPinParams: Codable {
    enum CodingKeys: String, CodingKey {
        case pin
        case plaintext
    }
    let pin: String
    let plaintext: [String: String]
}

struct EncryptWithPinResult: Codable {
    enum CodingKeys: String, CodingKey {
        case pinData = "pin_data"
    }
    let pinData: PinData
}

struct LoginUserResult: Codable {
    enum CodingKeys: String, CodingKey {
        case xpubHashId = "xpub_hash_id"
        case walletHashId = "wallet_hash_id"
    }
    let xpubHashId: String
    let walletHashId: String
}

struct GetSubaccountsParams: Codable {
    enum CodingKeys: String, CodingKey {
        case refresh
    }
    let refresh: Bool
}

struct GetSubaccountsResult: Codable {
    enum CodingKeys: String, CodingKey {
        case subaccounts
    }
    let subaccounts: [WalletItem]
}

struct GetSubaccountParams: Codable {
    enum CodingKeys: String, CodingKey {
        case pointer
    }
    let pointer: UInt32
}

struct GetAssetsParams: Codable {
    enum CodingKeys: String, CodingKey {
        case assetsId = "assets_id"
    }
    let assetsId: [String]
}

struct GetAssetsResult: Codable {
    let assets: [String: AssetInfo]
    let icons: [String: String]
}

struct GdkInit: Codable {
    enum CodingKeys: String, CodingKey {
        case datadir
        case tordir
        case registrydir
        case logLevel = "log_level"
    }
    let datadir: String?
    let tordir: String?
    let registrydir: String?
    let logLevel: String

    static func defaults() -> GdkInit {
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

    func run() {
        try? gdkInit(config: self.toDict() ?? [:])
    }
}
