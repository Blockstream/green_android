import Foundation

public struct TransactionEvent: Codable {
    enum CodingKeys: String, CodingKey {
        case txHash = "txhash"
        case type = "type"
        case subAccounts = "subaccounts"
        case satoshi = "satoshi"
    }
    public let txHash: String
    public let type: String
    public let subAccounts: [Int]
    public let satoshi: UInt64
}

public protocol EventProtocol {
    func title() -> String
    func description(wallets: [WalletItem], twoFactorConfig: TwoFactorConfig?) -> String
}
