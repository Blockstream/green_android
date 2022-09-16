import Foundation
import UIKit

class OVAssetCellModel {
    var asset: AssetInfo?
    var icon: UIImage?
    var value: String?
    var fiat: String?

    init(assetId: String, satoshi: UInt64) {
        let session = WalletManager.current?.sessions[assetId == "btc" ? "electrum-mainnet" : "electrum-liquid"]
        asset = session?.registry?.info(for: assetId)
        icon = session?.registry?.image(for: assetId)

        if let balance = Balance.fromSatoshi(satoshi, asset: asset)?.toAssetValue() {
            self.value = "\(balance.0) \(balance.1)"
        }
        if session?.gdkNetwork.getFeeAsset() == assetId,
            let balance = Balance.fromSatoshi(satoshi, asset: asset)?.toFiat() {
            self.fiat = "\(balance.0) \(balance.1)"
        }
    }
}
