import Foundation

struct TransactionEvent: Codable {
    enum CodingKeys: String, CodingKey {
        case txHash = "txhash"
        case type = "type"
        case subAccounts = "subaccounts"
        case satoshi = "satoshi"
    }
    let txHash: String
    let type: String
    let subAccounts: [Int]
    let satoshi: UInt64
}

struct SystemMessage: Codable {
    let text: String
}

protocol EventProtocol {
    func title() -> String
    func description(wallets: [WalletItem], twoFactorConfig: TwoFactorConfig?) -> String
}
