import Foundation

public struct Balance: Codable {

    enum CodingKeys: String, CodingKey {
        case bits
        case btc
        case fiat
        case fiatCurrency = "fiat_currency"
        case fiatRate = "fiat_rate"
        case mbtc
        case satoshi
        case ubtc
        case sats
        case assetInfo = "asset_info"
        case asset
        case assetId = "asset_id"
    }

    public let bits: String
    public let btc: String
    public let fiat: String?
    public let fiatCurrency: String
    public let fiatRate: String?
    public let mbtc: String
    public let satoshi: Int64
    public let ubtc: String
    public let sats: String
    public let assetInfo: AssetInfo?
    public var asset: [String: String]?
    public var assetId: String?
}
