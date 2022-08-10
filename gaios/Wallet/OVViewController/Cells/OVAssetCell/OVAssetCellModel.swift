import Foundation
import UIKit

class OVAssetCellModel {
    var asset: AssetInfo?
    var icon: UIImage?
    var value: String?
    var fiat: String?

    init(assetId: String, satoshi: UInt64) {
        asset = WalletManager.current?.registry.info(for: assetId)
        icon = WalletManager.current?.registry.image(for: assetId)

        if let balance = Balance.fromSatoshi(satoshi, asset: asset)?.toValue() {
            self.value = "\(balance.0) \(balance.1)"
        }
        if let balance = Balance.fromSatoshi(satoshi, asset: asset)?.toFiat() {
            self.fiat = "\(balance.0) \(balance.1)"
        }
    }
}
