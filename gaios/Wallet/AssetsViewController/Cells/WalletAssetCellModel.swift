import Foundation
import UIKit

class WalletAssetCellModel {
    var asset: AssetInfo?
    var icon: UIImage?
    var value: String?
    var fiat: String?
    var satoshi: Int64?

    init(assetId: String, satoshi: Int64) {
        asset = WalletManager.current?.registry.info(for: assetId)
        icon = WalletManager.current?.registry.image(for: assetId)
        self.satoshi = satoshi

        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId)?.toValue() {
            self.value = "\(balance.0) \(balance.1)"
        }
        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId)?.toFiat() {
            self.fiat = "\(balance.0) \(balance.1)"
        }
    }
}
