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
