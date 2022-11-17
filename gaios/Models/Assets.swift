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
        case amp
    }

    var assetId: String
    var name: String?
    var precision: UInt8?
    var ticker: String?
    var entity: AssentEntity?
    var amp: Bool?

    init(assetId: String, name: String?, precision: UInt8, ticker: String?) {
        self.assetId = assetId
        self.name = name
        self.precision = precision
        self.ticker = ticker
    }

    func encode() -> [String: Any]? {
        return try? JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any]
    }

    // Default asset id
    static var btcId = "btc"
    static var testId = "btc"
    static var lbtcId = getGdkNetwork("liquid").getFeeAsset()
    static var ltestId = getGdkNetwork("testnet-liquid").getFeeAsset()

    // Default asset info
    static var btc: AssetInfo {
        let denom = WalletManager.current?.prominentSession?.settings?.denomination ?? .BTC
        return AssetInfo(assetId: btcId,
                         name: "Bitcoin",
                         precision: denom.digits,
                         ticker: DenominationType.denominationsBTC[denom])
    }

    static var test: AssetInfo {
        let denom = WalletManager.current?.prominentSession?.settings?.denomination ?? .BTC
        return AssetInfo(assetId: testId,
                         name: "Testnet",
                         precision: denom.digits,
                         ticker: DenominationType.denominationsTEST[denom])
    }

    static var lbtc: AssetInfo {
        let denom = WalletManager.current?.prominentSession?.settings?.denomination ?? .BTC
        return AssetInfo(assetId: lbtcId,
                         name: "Liquid Bitcoin",
                         precision: denom.digits,
                         ticker: DenominationType.denominationsBTC[denom])
    }

    static var ltest: AssetInfo {
        let denom = WalletManager.current?.prominentSession?.settings?.denomination ?? .BTC
        return AssetInfo(assetId: ltestId,
                         name: "Liquid Testnet",
                         precision: denom.digits,
                         ticker: DenominationType.denominationsLTEST[denom])
    }

    // comparing functions
    static func < (lhs: AssetInfo, rhs: AssetInfo) -> Bool {
        if [btcId, testId].contains(lhs.assetId) { return true }
        if [btcId, testId].contains(rhs.assetId) { return false }
        if [lbtcId, ltestId].contains(lhs.assetId) { return true }
        if [lbtcId, ltestId].contains(rhs.assetId) { return false }
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
