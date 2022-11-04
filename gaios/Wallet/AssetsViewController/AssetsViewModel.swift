import Foundation
import PromiseKit

class AssetsViewModel {

    var assetCellModels: [WalletAssetCellModel] = []
    var assetCellModelsFilter: [WalletAssetCellModel] = []

    init(assetCellModels: [WalletAssetCellModel]) {
        self.assetCellModels = assetCellModels
        self.assetCellModelsFilter = assetCellModels
    }

    func search(_ txt: String?) {

        self.assetCellModelsFilter = []
        assetCellModels.forEach {
            if let txt = txt, txt.count > 0 {
                if ($0.asset?.name ?? "") .lowercased().contains(txt.lowercased()) {
                    self.assetCellModelsFilter.append($0)
                }
            } else {
                self.assetCellModelsFilter.append($0)
            }
        }
    }
}
