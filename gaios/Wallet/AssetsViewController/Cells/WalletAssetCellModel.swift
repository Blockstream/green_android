import Foundation
import UIKit
import gdk

class WalletAssetCellModel {
    var assetId: String
    var asset: AssetInfo?
    var icon: UIImage?
    var value: String?
    var fiat: String?
    var satoshi: Int64

    init(assetId: String, satoshi: Int64) {
        self.assetId = assetId
        self.satoshi = satoshi
        load()
    }

    func load() {
        asset = WalletManager.current?.registry.info(for: assetId)
        icon = WalletManager.current?.registry.image(for: assetId)

        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId)?.toValue() {
            self.value = "\(balance.0) \(balance.1)"
        }
        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId)?.toFiat() {
            self.fiat = "\(balance.0) \(balance.1)"
        }
    }
}
