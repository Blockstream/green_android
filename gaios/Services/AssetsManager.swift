import Foundation
import UIKit
import PromiseKit

class AssetsManager {

    private let testnet: Bool
    private var infos = [String: AssetInfo]()
    private var icons = [String: String]()
    private var session: SessionManager?
    private let qos = DispatchQueue(label: "AssetsManagerDispatchQueue", qos: .userInteractive)

    init(testnet: Bool) {
        self.testnet = testnet
        if testnet {
            self.infos = [AssetInfo.testId: AssetInfo.test,
                          AssetInfo.ltestId: AssetInfo.ltest]
        } else {
            self.infos = [AssetInfo.btcId: AssetInfo.btc,
                          AssetInfo.lbtcId: AssetInfo.lbtc]
        }
    }

    var all: [AssetInfo] {
        return infos.map { $0.value }.sorted()
    }

    func info(for key: String) -> AssetInfo {
        if infos[key] == nil, let session = session {
            let assets = qos.sync() { session.getAssets(params: GetAssetsParams(assetsId: [key])) }
            self.infos.merge(assets?.assets ?? [:], uniquingKeysWith: {_, new in new})
        }
        if let asset = infos[key] {
            return asset
        }
        return AssetInfo(assetId: key, name: nil, precision: 0, ticker: nil)
    }

    func getImage(for key: String) -> UIImage? {
        if key == "btc" {
            return UIImage(named: testnet ? "ntw_testnet" : "ntw_btc")
        }
        if icons[key] == nil, let session = session {
            let assets = qos.sync() { session.getAssets(params: GetAssetsParams(assetsId: [key])) }
            self.icons.merge(assets?.icons ?? [:], uniquingKeysWith: {_, new in new})
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

    func loadAsync(session: SessionManager) {
        self.session = session
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee()
            .then(on: bgq) {
                session.connect()
            }.compactMap(on: bgq) {
                self.fetchFromCountly(session: session)
            }.compactMap(on: qos) {
                _ = try session.refreshAssets(icons: true, assets: true, refresh: true)
            }.done { _ in
                let notification = NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue)
                NotificationCenter.default.post(name: notification, object: nil, userInfo: nil)
            }.catch { _ in
                print("Asset registry loading failure")
            }
    }

    func getAssetsFromCountly() -> [EnrichedAsset] {
        let assets = AnalyticsManager.shared.getRemoteConfigValue(key: Constants.countlyRemoteConfigAssets) as? [[String: Any]]
        let json = try? JSONSerialization.data(withJSONObject: assets ?? [], options: [])
        let res = try? JSONDecoder().decode([EnrichedAsset].self, from: json ?? Data())
        return res ?? []
    }

    func fetchFromCountly(session: SessionManager) {
        let assets = getAssetsFromCountly()
        let res = qos.sync() { session.getAssets(params: GetAssetsParams(assetsId: assets.map { $0.id })) }
        self.infos.merge(res?.assets ?? [:], uniquingKeysWith: {_, new in new})
        self.icons.merge(res?.icons ?? [:], uniquingKeysWith: {_, new in new})
        assets.forEach {
            self.infos[$0.id]?.amp = $0.amp ?? false
            self.infos[$0.id]?.weight = $0.weight ?? 0
        }
    }
}
