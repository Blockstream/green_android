import Foundation
import UIKit
import gdk

class AccountCellModel {

    var name: String
    var type: String
    var value: String?
    var fiat: String?
    var lblType: String
    var balanceStr: String = ""
    var fiatStr: String = ""
    var account: WalletItem
    var hasTxs: Bool = false
    var satoshi: Int64?
    var networkType: NetworkSecurityCase

    init(subaccount: WalletItem, satoshi: Int64?) {
        self.satoshi = satoshi
        account = subaccount
        name = account.localizedName
        type = account.localizedType
        networkType = account.networkType
        lblType = subaccount.localizedType
        hasTxs = subaccount.hasTxs
        let assetId = account.gdkNetwork.getFeeAsset()
        if let satoshi = satoshi,let converted = Balance.fromSatoshi(satoshi, assetId: assetId) {
            let (amount, denom) = converted.toValue()
            balanceStr = "\(amount) \(denom)"
            let (fAmount, fDenom) = converted.toFiat()
            fiatStr = "\(fAmount) \(fDenom)"
        }
    }
}
