import Foundation
import UIKit
import gdk

class AccountAssetCellModel: Comparable {
    
    var account: WalletItem
    var asset: AssetInfo
    var balance: [String: Int64]

    init(account: WalletItem, asset: AssetInfo, balance: [String: Int64] ) {
        self.account = account
        self.asset = asset
        self.balance = balance
    }

    var icon: UIImage? {
        if account.gdkNetwork.lightning {
            return UIImage(named: "ic_lightning_btc")
        } else {
            let registry = WalletManager.current?.registry
            return registry?.image(for: asset.assetId)
        }
    }

    var ticker: String {
        asset.ticker ?? ""
    }
    
    static func == (lhs: AccountAssetCellModel, rhs: AccountAssetCellModel) -> Bool {
        lhs.account == rhs.account && lhs.asset == rhs.asset && lhs.balance == rhs.balance
    }

    static func < (lhs: AccountAssetCellModel, rhs: AccountAssetCellModel) -> Bool {
        if lhs.asset.assetId != rhs.asset.assetId {
            return WalletManager.current?.registry.sortAssets(lhs: lhs.asset.assetId, rhs: rhs.asset.assetId) ?? false
        }
        if lhs.account != rhs.account {
            return lhs.account < rhs.account
        }
        return lhs.balance[lhs.asset.assetId] ?? 0 < rhs.balance[rhs.asset.assetId] ?? 0
    }
}
