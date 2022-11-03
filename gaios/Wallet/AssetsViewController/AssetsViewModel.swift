import Foundation
import PromiseKit

class AssetsViewModel {

    var assetCellModels: [WalletAssetCellModel] = []
    var assetCellModelsFilter: [WalletAssetCellModel] = []

    init(assetCellModels: [WalletAssetCellModel]) {
        self.assetCellModels = assetCellModels
        self.assetCellModelsFilter = assetCellModels
    }

    
//    var reloadTableView: (() -> Void)?
//    var session = SessionsManager.current!

//    var assetCellModels = [SwapAssetCellModel]() {
//        didSet {
//            reloadTableView?()
//        }
//    }

//    func assetData(for assetId: String) -> (info: AssetInfo?, image: UIImage?) {
//        let registry = SessionsManager.current?.registry
//        return (info: registry?.info(for: assetId),
//                image: registry?.image(for: assetId))
//    }

//    func getAssetCellModels(at indexPath: IndexPath) -> SwapAssetCellModel {
//        return assetCellModels[indexPath.row]
//    }

//    func getAsset(at indexPath: IndexPath) -> String? {
//        return assetCellModels[indexPath.row].asset?.assetId
//    }

//    func getAssets() {
//        if let registry = session.registry as? AssetsManagerLiquid {
//            assets = Array(registry.infos.keys)
//            assets += remoteAssets.filter({ !($0.amp ?? false) }).map { $0.id }
//            assets = Array(Set(assets))
//            search(nil)
//        }
//    }
//
//    var remoteAssets: [EnrichedAsset] {
//        if let assets: [Any] = AnalyticsManager.shared.getRemoteConfigValue(key: Constants.countlyRemoteConfigAssets) as? [Any] {
//            let json = try? JSONSerialization.data(withJSONObject: assets, options: [])
//            let assets = try? JSONDecoder().decode([EnrichedAsset].self, from: json ?? Data())
//            return assets ?? []
//        }
//        return []
//    }

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
//
//        assetCellModels.sort(by: { (lha, rha) in
//            let btc = SessionsManager.current?.gdkNetwork.getFeeAsset()
//            if lha.asset?.assetId == btc {
//                return true
//            } else if rha.asset?.assetId == btc {
//                return false
//            } else if lha.hasImage && !rha.hasImage {
//                return true
//            } else if !lha.hasImage && rha.hasImage {
//                return false
//            } else if (lha.asset?.name ?? "").isEmpty {
//                return false
//            } else if (rha.asset?.name ?? "").isEmpty {
//                return true
//            } else {
//                return lha.asset?.name ?? "" < rha.asset?.name ?? ""
//            }
//        })
    }
}
