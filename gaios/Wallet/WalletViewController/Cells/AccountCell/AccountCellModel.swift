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
    }
}
