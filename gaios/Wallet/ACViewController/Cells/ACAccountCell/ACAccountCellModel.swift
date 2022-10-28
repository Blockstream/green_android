import Foundation
import UIKit

class ACAccountCellModel {

    var name: String
    var type: String
    var security: String
    var value: String?
    var fiat: String?

    init(subaccount: WalletItem, assetId: String, satoshi: Int64) {
        name = subaccount.localizedName()
        type = subaccount.type.typeStringId
        security = getGdkNetwork(subaccount.network ?? "mainnet").electrum ? "Singlesig" : "Multisig"

        let asset = WalletManager.current?.registry.info(for: assetId)
        if let balance = Balance.fromSatoshi(satoshi, asset: asset)?.toValue() {
            self.value = "\(balance.0) \(balance.1)"
        }
        if let balance = Balance.fromSatoshi(satoshi, asset: asset)?.toFiat() {
            self.fiat = "\(balance.0) \(balance.1)"
        }
    }
}
