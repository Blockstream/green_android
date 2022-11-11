import Foundation
import PromiseKit

class AssetSelectViewModel {

    var accounts: [WalletItem]
    var cachedBalance: [(String, Int64)]

    var assetSelectCellModels: [AssetSelectCellModel] = []
    var assetSelectCellModelsFilter: [AssetSelectCellModel] = []

    init(accounts: [WalletItem], cachedBalance: [(String, Int64)]) {
        self.accounts = accounts
        self.cachedBalance = cachedBalance
        self.assetSelectCellModels = cachedBalance.map { AssetSelectCellModel(assetId: $0.0, satoshi: $0.1) }
        self.assetSelectCellModelsFilter = assetSelectCellModels
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

    func getAssetIndex(_ index: Int) -> Int? {
        let assetAtFilterIndex = assetSelectCellModelsFilter[safe: index]
        for (idx, asset) in assetSelectCellModels.enumerated() where asset.asset?.assetId == assetAtFilterIndex?.asset?.assetId {
            return idx
        }
        return nil
    }
}
