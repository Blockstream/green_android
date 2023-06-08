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
