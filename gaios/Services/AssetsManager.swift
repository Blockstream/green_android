import Foundation
import UIKit
import PromiseKit
import gdk

class AssetsManager {

    private let testnet: Bool
    private var infos = [String: AssetInfo?]()
    private var icons = [String: String?]()
    private var uncached = [String]()
    private var session: SessionManager?
    private let qos = DispatchQueue(label: "AssetsManagerDispatchQueue", qos: .userInteractive)

    init(testnet: Bool) {
        self.testnet = testnet
    }

    var all: [AssetInfo] {
        return infos.compactMap { $0.value }.sorted()
    }

    func getAsset(for key: String) {
        let assets = qos.sync() { session?.getAssets(params: GetAssetsParams(assetsId: [key])) }
        if let assets = assets {
            infos.merge(assets.assets.isEmpty ? [key: nil] : assets.assets, uniquingKeysWith: {_, new in new})
            icons.merge(assets.icons.isEmpty ? [key: nil] : assets.icons, uniquingKeysWith: {_, new in new})
        }
    }

    func info(for key: String) -> AssetInfo {
        let main = [AssetInfo.btc, AssetInfo.lbtc, AssetInfo.test, AssetInfo.ltest].filter { $0.assetId == key }.first
        if let main = main {
            return main
        }
        if infos[key] == nil {
            getAsset(for: key)
        }
        if let asset = infos[key], let asset = asset {
            return asset
        }
        return AssetInfo(assetId: key, name: nil, precision: 0, ticker: nil)
    }

    func getImage(for key: String) -> UIImage? {
        if [AssetInfo.btcId, AssetInfo.testId].contains(key) {
            return UIImage(named: testnet ? "ntw_testnet" : "ntw_btc")
        }
        if icons[key] == nil {
            getAsset(for: key)
        }
        if let icon = icons[key] {
            return UIImage(base64: icon)
        }
        return nil
    }

    func image(for key: String) -> UIImage {
        return getImage(for: key) ?? UIImage(named: "default_asset_icon") ?? UIImage()
    }

    func hasImage(for key: String?) -> Bool {
        return getImage(for: key ?? "") != nil
    }

    func loadAsync(session: SessionManager?) {
        if testnet {
            infos = [AssetInfo.testId: AssetInfo.test,
                          AssetInfo.ltestId: AssetInfo.ltest]
        } else {
            infos = [AssetInfo.btcId: AssetInfo.btc,
                          AssetInfo.lbtcId: AssetInfo.lbtc]
        }
        self.session = session ?? self.session
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee()
            .compactMap { self.session }
            .then(on: bgq) { $0.connect() }
            .compactMap(on: bgq) { self.fetchFromCountly(session: self.session) }
            .compactMap(on: qos) { _ = try self.session?.refreshAssets(icons: true, assets: true, refresh: true) }
            .done { _ in
                let notification = NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue)
                NotificationCenter.default.post(name: notification, object: nil, userInfo: nil)
            }.catch { _ in print("Asset registry loading failure") }
    }

    func getAssetsFromCountly() -> [EnrichedAsset] {
        let assets = AnalyticsManager.shared.getRemoteConfigValue(key: Constants.countlyRemoteConfigAssets) as? [[String: Any]]
        let json = try? JSONSerialization.data(withJSONObject: assets ?? [], options: [])
        let res = try? JSONDecoder().decode([EnrichedAsset].self, from: json ?? Data())
        return res ?? []
    }

    func fetchFromCountly(session: SessionManager?) {
        let assets = getAssetsFromCountly()
        let res = qos.sync() { session?.getAssets(params: GetAssetsParams(assetsId: assets.map { $0.id })) }
        self.infos.merge(res?.assets ?? [:], uniquingKeysWith: {_, new in new})
        self.icons.merge(res?.icons ?? [:], uniquingKeysWith: {_, new in new})
        assets.forEach {
            self.infos[$0.id]??.amp = $0.amp ?? false
            self.infos[$0.id]??.weight = $0.weight ?? 0
        }
    }
}
