import Foundation
import UIKit

class AssetSelectCellModel {
    var asset: AssetInfo?
    var icon: UIImage?
    var ampWarn: String?

    init(assetId: String, satoshi: Int64) {
        asset = WalletManager.current?.registry.info(for: assetId)
        icon = WalletManager.current?.registry.image(for: assetId)
        /// probably we can remove
        ampWarn = nil
    }
}
