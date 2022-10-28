import Foundation
import UIKit

class AccountCellModel {

    var name: String
    var type: String
    var security: String
    var isSS: Bool
    var isLiquid: Bool
    var value: String?
    var fiat: String?
    var lblType: String

    init(subaccount: WalletItem /* , assetId: String, satoshi: Int64 */ ) {
        name = subaccount.localizedName()
        type = subaccount.type.typeStringId.localized.uppercased()
        isSS = getGdkNetwork(subaccount.network ?? "mainnet").electrum ? true : false
        isLiquid = getGdkNetwork(subaccount.network ?? "mainnet").liquid
        security = (isSS ? "Singlesig" : "Multisig").uppercased()
        lblType = security + " / " + type
//        let asset = WalletManager.current?.registry.info(for: assetId)
//        if let balance = Balance.fromSatoshi(satoshi, asset: asset)?.toValue() {
//            self.value = "\(balance.0) \(balance.1)"
//        }
//        if let balance = Balance.fromSatoshi(satoshi, asset: asset)?.toFiat() {
//            self.fiat = "\(balance.0) \(balance.1)"
//        }
    }
}
