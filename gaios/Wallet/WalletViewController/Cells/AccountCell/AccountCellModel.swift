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

    init(subaccount: WalletItem) {
        account = subaccount
        name = account.localizedName()
        type = account.type.typeStringId.localized.uppercased()
        isSS = account.gdkNetwork.electrum ? true : false
        isLiquid = account.gdkNetwork.liquid
        isTest = !account.gdkNetwork.mainnet
        security = (isSS ? "Singlesig" : "Multisig").uppercased()
        lblType = security + " / " + type

        let satoshi = account.satoshi?[account.gdkNetwork.getFeeAsset()] ?? 0
        if let converted = Balance.fromSatoshi(satoshi) {
            let (amount, denom) = converted.toValue()
            balanceStr = "\(amount) \(denom)"

            let (fAmount, fDenom) = converted.toFiat()
            fiatStr = "\(fAmount) \(fDenom)"
        }
    }
}
