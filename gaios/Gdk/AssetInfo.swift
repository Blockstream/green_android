import Foundation
import gdk

extension AssetInfo {
    
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
                         ticker: DenominationType.denominationsLBTC[denom])
    }

    static var ltest: AssetInfo {
        let denom = WalletManager.current?.prominentSession?.settings?.denomination ?? .BTC
        return AssetInfo(assetId: ltestId,
                         name: "Liquid Testnet",
                         precision: denom.digits,
                         ticker: DenominationType.denominationsLTEST[denom])
        }
}

extension AssetInfo: Comparable {

    // comparing functions
    public static func < (lhs: AssetInfo, rhs: AssetInfo) -> Bool {
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
        let lhsw = lhs.weight ?? 0
        let rhsw = rhs.weight ?? 0
        if lhsw > rhsw {
            return true
        } else {
            return false
        }
    }

    public static func == (lhs: AssetInfo, rhs: AssetInfo) -> Bool {
        return lhs.assetId == rhs.assetId
    }
}
