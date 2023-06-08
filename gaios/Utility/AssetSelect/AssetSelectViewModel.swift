import Foundation


class AssetSelectViewModel {

    var assets: AssetAmountList?
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

    init(assets: AssetAmountList, enableAnyAsset: Bool) {
        self.assets = assets
        self.enableAnyAsset = enableAnyAsset
        assetSelectCellModels = self.assets?.amounts.map { AssetSelectCellModel(assetId: $0.0, satoshi: $0.1) } ?? []
        assetSelectCellModelsFilter = assetSelectCellModels
        reload?()
    }
}
