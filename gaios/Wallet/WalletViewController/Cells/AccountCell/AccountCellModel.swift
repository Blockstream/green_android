import Foundation
import UIKit
import gdk

class AccountCellModel {

    var name: String
    var type: String
    var security: String
    var isSS: Bool
    var isLiquid: Bool
    var isTest: Bool
    var value: String?
    var fiat: String?
    var lblType: String
    var balanceStr: String = ""
    var fiatStr: String = ""
    var account: WalletItem
    var hasTxs: Bool = false

    init(subaccount: WalletItem, satoshi: Int64?) {
        account = subaccount
        name = account.localizedName
        type = account.localizedType
        isSS = account.gdkNetwork.electrum ? true : false
        isLiquid = account.gdkNetwork.liquid
        isTest = !account.gdkNetwork.mainnet
        security = (isSS ? "Singlesig" : "Multisig").uppercased()
        lblType = security + " / " + type
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
