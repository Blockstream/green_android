import Foundation
import UIKit
import PromiseKit

class AccountAssetViewModel {

    var accountAssetCellModels: [AccountAssetCellModel] = []
    let registry = WalletManager.current?.registry

    /// reload by section with animation
    var reloadSections: (([AccountAssetSection], Bool) -> Void)?

    init(accounts: [WalletItem]) {
        var models: [AccountAssetCellModel] = []

        accounts.forEach { subaccount in
            let satoshi = subaccount.satoshi ?? [:]
            satoshi.forEach {
                var balance = [String: Int64]()
                balance[$0.0] = $0.1
                let assets: [AssetInfo] = balance.keys.compactMap { registry?.info(for: $0) }

                for asset in assets {
                    models.append(AccountAssetCellModel(account: subaccount,
                                                        asset: asset,
                                                        balance: (balance.filter { $0.key == asset.assetId }))
                    )
                }
            }
        }
        self.accountAssetCellModels = sort(models)
    }

    func sort(_ models: [AccountAssetCellModel]) -> [AccountAssetCellModel] {
        var oModels = models
        guard let registry = registry else { return oModels }
        oModels = models.sorted { (lhs, rhs) in
            if registry.info(for: lhs.asset.assetId) == registry.info(for: rhs.asset.assetId) {
                return lhs.account < rhs.account
            }
            return registry.info(for: lhs.asset.assetId) < registry.info(for: rhs.asset.assetId)
        }
        return oModels
    }
}
