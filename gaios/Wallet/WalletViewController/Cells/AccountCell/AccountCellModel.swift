import Foundation
import UIKit

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

    init(subaccount: WalletItem, satoshi: Int64) {
        account = subaccount
        name = account.localizedName
        type = account.localizedType
        isSS = account.gdkNetwork.electrum ? true : false
        isLiquid = account.gdkNetwork.liquid
        isTest = !account.gdkNetwork.mainnet
        security = (isSS ? "Singlesig" : "Multisig").uppercased()
        lblType = security + " / " + type

        if let converted = Balance.fromSatoshi(satoshi, assetId: account.gdkNetwork.getFeeAsset()) {
            let (amount, denom) = converted.toValue()
            balanceStr = "\(amount) \(denom)"

            let (fAmount, fDenom) = converted.toFiat()
            fiatStr = "\(fAmount) \(fDenom)"
        }
        hasTxs = subaccount.hasTxs
    }
}
