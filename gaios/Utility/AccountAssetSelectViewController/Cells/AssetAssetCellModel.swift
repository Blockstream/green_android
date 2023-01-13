import Foundation
import UIKit

class AccountAssetCellModel {
    var account: WalletItem
    var asset: AssetInfo

    init(account: WalletItem, asset: AssetInfo ) {
        self.account = account
        self.asset = asset
    }
}
