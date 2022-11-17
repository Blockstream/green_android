import Foundation
import UIKit
import PromiseKit

struct AssentEntity: Codable {
    let domain: String
}

struct SortingAsset {
    let tag: String
    let info: AssetInfo?
    let hasImage: Bool
    let value: Int64
}

struct AssetInfo: Codable, Comparable {

    enum CodingKeys: String, CodingKey {
        case assetId = "asset_id"
        case name
        case precision
        case ticker
        case entity
    }

    var assetId: String
    var name: String?
    var precision: UInt8?
    var ticker: String?
    var entity: AssentEntity?

    init(assetId: String, name: String?, precision: UInt8, ticker: String?) {
        self.assetId = assetId
        self.name = name
        self.precision = precision
        self.ticker = ticker
    }

    func encode() -> [String: Any]? {
        return try? JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any]
    }

    static var btc = "btc"
    static var test = "btc"
    static var lbtc = getGdkNetwork("liquid").getFeeAsset()
    static var ltest = getGdkNetwork("testnet-liquid").getFeeAsset()

    static func < (lhs: AssetInfo, rhs: AssetInfo) -> Bool {
        if [btc, test].contains(lhs.assetId) { return true }
        if [btc, test].contains(rhs.assetId) { return false }
        if [lbtc, ltest].contains(lhs.assetId) { return true }
        if [lbtc, ltest].contains(rhs.assetId) { return false }
        let registry = WalletManager.current?.registry
        let lhsImage = registry?.hasImage(for: lhs.assetId) ?? false
        let rhsImage = registry?.hasImage(for: rhs.assetId) ?? false
        if lhsImage && !rhsImage { return true }
        if !lhsImage && rhsImage { return false }
        if lhs.ticker != nil && rhs.ticker == nil { return true }
        if lhs.ticker == nil && rhs.ticker != nil { return false }
        return lhs.assetId < rhs.assetId
    }

    static func == (lhs: AssetInfo, rhs: AssetInfo) -> Bool {
        return lhs.assetId == rhs.assetId
    }
}
