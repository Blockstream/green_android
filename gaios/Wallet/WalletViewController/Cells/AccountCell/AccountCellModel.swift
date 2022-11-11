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
        name = subaccount.localizedName()
        type = subaccount.type.typeStringId.localized.uppercased()
        isSS = getGdkNetwork(subaccount.network ?? "mainnet").electrum ? true : false
        isLiquid = getGdkNetwork(subaccount.network ?? "mainnet").liquid
        isTest = (getGdkNetwork(subaccount.network ?? "mainnet").network).contains("testnet")
        security = (isSS ? "Singlesig" : "Multisig").uppercased()
        lblType = security + " / " + type
        account = subaccount

        let gdkNetwork = getGdkNetwork(account.network ?? "mainnet")
        let satoshi = account.satoshi?[gdkNetwork.getFeeAsset()] ?? 0
        if let converted = Balance.fromSatoshi(satoshi) {
            let (amount, denom) = converted.toValue()
            balanceStr = "\(amount) \(denom)"

            let (fAmount, fDenom) = converted.toFiat()
            fiatStr = "\(fAmount) \(fDenom)"
        }
    }
}
