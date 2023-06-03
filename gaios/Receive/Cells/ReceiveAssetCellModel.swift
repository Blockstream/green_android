import Foundation
import UIKit
import gdk

struct ReceiveAssetCellModel {
    let assetId: String
    let account: WalletItem

    var icon: UIImage? {
        if account.gdkNetwork.lightning {
            return UIImage(named: "ic_lightning_btc")
        } else {
            let registry = WalletManager.current?.registry
            return registry?.image(for: assetId)
        }
    }

    var assetName: String? {
        let registry = WalletManager.current?.registry
        return registry?.info(for: assetId).name
    }

    var ticker: String? {
        let registry = WalletManager.current?.registry
        return registry?.info(for: assetId).ticker
    }
}
