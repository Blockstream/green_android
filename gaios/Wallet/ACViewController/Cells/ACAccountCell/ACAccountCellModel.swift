import Foundation
import UIKit

class ACAccountCellModel {

    var name: String
    var type: String
    var security: String
    var value: String?
    var fiat: String?

    init(subaccount: WalletItem, assetId: String, satoshi: UInt64) {
        name = subaccount.localizedName()
        type = subaccount.type.typeStringId
        security = getGdkNetwork(subaccount.network ?? "mainnet").electrum ? "Singlesig" : "Multisig"

        let session = WalletManager.current?.sessions[assetId == "btc" ? "electrum-mainnet" : "electrum-liquid"]
        let asset = session?.registry?.info(for: assetId)

        if let balance = Balance.fromSatoshi(satoshi, asset: asset)?.toAssetValue() {
            self.value = "\(balance.0) \(balance.1)"
        }
        if session?.gdkNetwork.getFeeAsset() == assetId,
            let balance = Balance.fromSatoshi(satoshi, asset: asset)?.toFiat() {
            self.fiat = "\(balance.0) \(balance.1)"
        }
    }
}
