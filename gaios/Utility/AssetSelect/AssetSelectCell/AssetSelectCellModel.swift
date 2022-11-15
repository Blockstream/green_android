import Foundation
import UIKit

class AssetSelectCellModel {
    var asset: AssetInfo?
    var icon: UIImage?
    var ampWarn: String?

    init(assetId: String, satoshi: Int64) {
        asset = WalletManager.current?.registry.info(for: assetId)
        icon = WalletManager.current?.registry.image(for: assetId)

        /// for demo only, to fix
        if assetId == "18729918ab4bca843656f08d4dd877bed6641fbd596a0a963abbf199cfeb3cec" {
            let name = asset?.name ?? asset?.assetId
            ampWarn = "\(name ?? "") " + "is a Liquid asset. You can receive it directly on a Liquid account."
        }
    }
}
