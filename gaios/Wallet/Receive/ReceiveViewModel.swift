import Foundation
import UIKit
import PromiseKit

class ReceiveViewModel {

    var accounts: [WalletItem]
    var cachedBalance: [(String, Int64)]
    var cAccount: WalletItem
    var cAsset: (String, Int64)

    init(accounts: [WalletItem], cachedBalance: [(String, Int64)]) {
        self.accounts = accounts
        self.cachedBalance = cachedBalance
        /// remove unwrapping here and change logic
        /// only for demo
        self.cAccount = accounts.first!
        self.cAsset = cachedBalance.first!
    }

    func assetIcon() -> UIImage {
        return WalletManager.current?.registry.image(for: cAsset.0) ?? UIImage()
    }
    func assetName() -> String {
        return WalletManager.current?.registry.info(for: cAsset.0).name ?? "--"
    }
    func accountType() -> String {
        return cAccount.type.typeStringId.localized.uppercased()
    }

    var assetSelectCellModels: [AssetSelectCellModel] {
        return cachedBalance.map { AssetSelectCellModel(assetId: $0.0, satoshi: $0.1) }
    }
}
