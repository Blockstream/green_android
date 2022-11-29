import Foundation
import UIKit
import PromiseKit

class AssetExpandableSelectViewModel {

    var selectedSection: Int = -1
    var allSections: [Int] = []
    var enableAnyAsset = true
    var onlyFunded = true
    var assets: [AssetInfo] = [] {
        didSet {
            for i in 0..<assets.count {
                allSections.append(i)
            }
        }
    }
    var accountSelectSubCellModels: [AccountSelectSubCellModel] = []
    var assetSelectCellModels: [AssetSelectCellModel] = []
    var assetSelectCellModelsFilter: [AssetSelectCellModel] = []

    let wm = WalletManager.current!

    init(assets: [AssetInfo], enableAnyAsset: Bool, onlyFunded: Bool) {
        self.assets = assets
        self.enableAnyAsset = enableAnyAsset
        self.onlyFunded = onlyFunded
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

    func loadAccountsForAsset(_ assetId: String?) {
        var accounts = wm.subaccounts
        if [AssetInfo.btcId, AssetInfo.testId].contains(assetId) {
            // for btc / test btc only
            accounts.removeAll { $0.hidden ?? false || $0.gdkNetwork.liquid }
        } else {
            // for liquid
            accounts.removeAll { $0.hidden ?? false || !$0.gdkNetwork.liquid }
            if let assetId = assetId, wm.registry.info(for: assetId).amp ?? false {
                accounts.removeAll { $0.type != .amp }
            }
        }
        if onlyFunded {
            accounts.removeAll { account in
                account.satoshi?
                    .filter { $0.key == assetId }.values.first ?? 0 == 0
            }
        }
        accountSelectSubCellModels = accounts.map { AccountSelectSubCellModel(account: $0) }
    }

    func loadAssets() {
        assetSelectCellModels = assets.map { AssetSelectCellModel(assetId: $0.assetId, satoshi: 0) }
        assetSelectCellModelsFilter = assetSelectCellModels
    }
}
