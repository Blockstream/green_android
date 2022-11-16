import Foundation
import UIKit
import PromiseKit

class AssetExpandableSelectViewModel {

    var selectedSection: Int = -1
    var allSections: [Int] = []
    var accounts: [WalletItem]
    var assets: [String] = []
    var accountSelectSubCellModels: [AccountSelectSubCellModel] = []

    var assetSelectCellModels: [AssetSelectCellModel] = []
    var assetSelectCellModelsFilter: [AssetSelectCellModel] = []

    init(accounts: [WalletItem]) {
        self.accounts = accounts
        loadAssets()
        self.accountSelectSubCellModels = accounts.map { AccountSelectSubCellModel(account: $0) }

        for i in 0..<assets.count {
            allSections.append(i)
        }
    }

    func search(_ txt: String?) {
        self.assetSelectCellModelsFilter = []
        assetSelectCellModels.forEach {
            if let txt = txt, txt.count > 0 {
                if ($0.asset?.name ?? "") .lowercased().contains(txt.lowercased()) {
                    self.assetSelectCellModelsFilter.append($0)
                }
            } else {
                self.assetSelectCellModelsFilter.append($0)
            }
        }
    }

    func loadAssets() {
        guard let registry = WalletManager.current?.registry else { return }
        self.assets = registry.allAssets
        if let account = accounts.first, accounts.count == 1 {
            if account.gdkNetwork.liquid {
                self.assets.removeAll(where: { $0 == "btc"})
            } else {
                self.assets.removeAll(where: { $0 != "btc"})
            }
        }
        self.assetSelectCellModels = self.assets.map { AssetSelectCellModel(assetId: $0, satoshi: 0) }
        self.assetSelectCellModelsFilter = self.assetSelectCellModels
    }
}
