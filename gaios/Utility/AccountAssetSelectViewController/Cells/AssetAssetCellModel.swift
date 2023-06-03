import Foundation
import UIKit
import gdk

class AccountAssetCellModel {
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
}
