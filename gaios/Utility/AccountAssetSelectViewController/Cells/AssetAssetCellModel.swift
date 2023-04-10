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
}
