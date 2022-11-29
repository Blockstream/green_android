import Foundation
import PromiseKit

class AssetSelectViewModel {

    var assets = [AssetInfo]()
    var reload: (() -> Void)?
    var enableAnyAsset: Bool

    var assetSelectCellModels: [AssetSelectCellModel] = []
    var assetSelectCellModelsFilter: [AssetSelectCellModel] = []
    private var wm: WalletManager { WalletManager.current! }

    func search(_ txt: String?) {
        assetSelectCellModelsFilter = []
        assetSelectCellModels.forEach {
            if let txt = txt, txt.count > 0 {
                if ($0.asset?.name ?? "") .lowercased().contains(txt.lowercased()) {
                    assetSelectCellModelsFilter.append($0)
                }
            } else {
                assetSelectCellModelsFilter.append($0)
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

    init(assets: [AssetInfo], enableAnyAsset: Bool) {
        self.assets = assets
        self.enableAnyAsset = enableAnyAsset
        assetSelectCellModels = assets.map { AssetSelectCellModel(assetId: $0.assetId, satoshi: 0) }
        assetSelectCellModelsFilter = assetSelectCellModels
        reload?()
    }
}
