import Foundation
import gdk
import UIKit

class AssetAmountList {

    var amounts: [(String, Int64)]
    var assets: [String: AssetInfo]  = [:]
    var hasImages: [String: Bool]  = [:]
    var ids: [String] { amounts.map { $0.0} }

    init(_ amounts: [String: Int64]) {
        let registry = WalletManager.current?.registry
        for assetId in amounts.keys {
            let assetInfo = registry?.info(for: assetId)
            assets[assetId] = assetInfo
            let hasImage = registry?.hasImage(for: assetId)
            hasImages[assetId] = hasImage ?? false
        }
        self.amounts = amounts.map { ($0.key, $0.value) }
        self.amounts = sorted()
    }

    static func from(assetIds: [String]) -> AssetAmountList {
        let assetIds = assetIds.map { ($0, Int64(0)) }
        let dict = Dictionary(uniqueKeysWithValues: assetIds)
        return AssetAmountList(dict)
    }

    func satoshi() -> Int64 {
        let baseIds = [AssetInfo.btcId, AssetInfo.testId, AssetInfo.lbtcId, AssetInfo.ltestId]
        return amounts.filter { baseIds.contains($0.0) }.map { $0.1 }.reduce(0, { (res, partial) in res + partial })
    }

    func sortAssets(lhs: String, rhs: String) -> Bool {
        if [AssetInfo.btcId, AssetInfo.testId].contains(lhs) { return true }
        if [AssetInfo.btcId, AssetInfo.testId].contains(rhs) { return false }
        if [AssetInfo.lbtcId, AssetInfo.ltestId].contains(lhs) { return true }
        if [AssetInfo.lbtcId, AssetInfo.ltestId].contains(rhs) { return false }
        let lhsImage = hasImages[lhs] ?? false
        let rhsImage = hasImages[rhs] ?? false
        if lhsImage && !rhsImage { return true }
        if !lhsImage && rhsImage { return false }
        let lhsInfo = assets[lhs]
        let rhsInfo = assets[rhs]
        if lhsInfo?.ticker != nil && rhsInfo?.ticker == nil { return true }
        if lhsInfo?.ticker == nil && rhsInfo?.ticker != nil { return false }
        let lhsw = lhsInfo?.weight ?? 0
        let rhsw = rhsInfo?.weight ?? 0
        if lhsw != rhsw {
            return lhsw > rhsw
        }
        return lhs < rhs
    }

    func sorted() -> [(String, Int64)] {
        return amounts.sorted(by: { (lhs, rhs) in
            return sortAssets(lhs: lhs.0, rhs: rhs.0)
        })
    }

    func nonZeroAmounts() -> [(String, Int64)] {
        return amounts.filter { $0.1 != 0 }
    }
    
    func image(for id: String) -> UIImage {
        let registry = WalletManager.current?.registry
        return registry?.image(for: id) ?? UIImage()
    }
}
