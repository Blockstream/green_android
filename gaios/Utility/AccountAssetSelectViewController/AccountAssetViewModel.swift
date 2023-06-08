import Foundation
import UIKit

import gdk

class AccountAssetViewModel {

    let accounts: [WalletItem]
    var accountAssetCellModels: [AccountAssetCellModel] = []
    let registry = WalletManager.current?.registry

    /// reload by section with animation
    var reloadSections: (([AccountAssetSection], Bool) -> Void)?

    init(accounts: [WalletItem]) {
        self.accounts = accounts
        load()
    }
    
    func load() {
        var models: [AccountAssetCellModel] = []
        for subaccount in accounts {
            let satoshi = subaccount.satoshi ?? [:]
            for sat in satoshi {
                var balance = [String: Int64]()
                balance[sat.0] = sat.1
                for id in balance.keys {
                    let asset = registry?.info(for: id)
                    let assetBalance = balance.filter { $0.key == asset!.assetId }
                    let satoshi = assetBalance.first?.value ?? 0
                    if satoshi > 0 {
                        models.append(AccountAssetCellModel(account: subaccount,
                                                            asset: asset!,
                                                            balance: assetBalance)
                                      )
                    }
                }
            }
        }
        self.accountAssetCellModels = models.sorted()
    }                                                                    
}
