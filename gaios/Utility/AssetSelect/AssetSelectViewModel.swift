import Foundation
import PromiseKit

class AssetSelectViewModel {

    var assetSelectCellModels: [AssetSelectCellModel] = []
    var assetSelectCellModelsFilter: [AssetSelectCellModel] = []

    init(assetSelectCellModels: [AssetSelectCellModel]) {
        self.assetSelectCellModels = assetSelectCellModels
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
