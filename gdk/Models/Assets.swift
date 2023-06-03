import Foundation
import UIKit
import PromiseKit

public struct AssentEntity: Codable {
    public let domain: String
}

public struct SortingAsset {
    public let tag: String
    public let info: AssetInfo?
    public let hasImage: Bool
    public let value: Int64
}

public struct AssetInfo: Codable {

    enum CodingKeys: String, CodingKey {
        case assetId = "asset_id"
        case name
        case precision
        case ticker
        case entity
        case amp
        case weight
    }

    public var assetId: String
    public var name: String?
    public var precision: UInt8?
    public var ticker: String?
    public var entity: AssentEntity?
    public var amp: Bool?
    public var weight: Int?

    public init(assetId: String, name: String?, precision: UInt8, ticker: String?) {
        self.assetId = assetId
        self.name = name
        self.precision = precision
        self.ticker = ticker
    }

    public func encode() -> [String: Any]? {
        return try? JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any]
    }

    // Default asset id
    public static var btcId = "btc"
    public static var testId = "btc"
    public static var lbtcId = GdkNetworks.shared.liquidSS.getFeeAsset()
    public static var ltestId = GdkNetworks.shared.testnetLiquidSS.getFeeAsset()
    public static var baseIds = [btcId, testId, lbtcId, ltestId]
}
