import Foundation
import UIKit
import PromiseKit

class AssetsManager {

    private let testnet: Bool
    private var infos = [String: AssetInfo]()
    private var icons = [String: String]()
    private var session: SessionManager?
    fileprivate let qos = DispatchQueue(label: "AssetsManagerDispatchQueue", qos: .userInteractive                                        )

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
            let infos = qos.sync() { fetchAssets(session: session, assetsId: [key]) }
            self.infos.merge(infos, uniquingKeysWith: {_, new in new})
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
            let icons = qos.sync() { fetchIcons(session: session, assetsId: [key]) }
            self.icons.merge(icons, uniquingKeysWith: {_, new in new})
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
    
    func getAsset(session: SessionManager, assetsId: [String]) {
        let assets = try? session.session?.getAssets(params: ["assets_id": assetsId])
        
    }
    
    

    func fetchAssets(session: SessionManager, assetsId: [String]) -> [String: AssetInfo] {
        let assets = try? session.session?.getAssets(params: ["assets_id": assetsId])
        let infosAsset = assets?["assets"] as? [String: Any]
        let infosData = try? JSONSerialization.data(withJSONObject: infosAsset ?? [:])
        let infos = try? JSONDecoder().decode([String: AssetInfo].self, from: infosData ?? Data())
        return infos ?? [:]
    }

    func fetchIcons(session: SessionManager, assetsId: [String]) -> [String: String] {
        let assets = try? session.session?.getAssets(params: ["assets_id": assetsId])
        let iconsAsset = assets?["icons"] as? [String: Any]
        let iconsData = try? JSONSerialization.data(withJSONObject: iconsAsset ?? [:])
        let icons = try? JSONDecoder().decode([String: String].self, from: iconsData ?? Data())
        return icons ?? [:]
    }

    func loadAsync(session: SessionManager?) {
        let liquidNetworks: [NetworkSecurityCase] = testnet ? [.testnetLiquidSS, .testnetLiquidMS ] : [.liquidSS, .liquidMS ]
        let session = session ?? SessionManager(getGdkNetwork(liquidNetworks.first!.rawValue))
        self.session = session
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee()
            .then(on: bgq) {
                session.connect()
            }.compactMap(on: bgq) {
                self.fetchFromCountly(session: session)
                _ = try session.refreshAssets(icons: false, assets: true, refresh: true)
                _ = try session.refreshAssets(icons: true, assets: false, refresh: true)
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
        let infos = qos.sync() { fetchAssets(session: session, assetsId: assets.map { $0.id }) }
        self.infos.merge(infos, uniquingKeysWith: {_, new in new})
        assets.forEach {
            self.infos[$0.id]?.amp = $0.amp ?? false
            self.infos[$0.id]?.weight = $0.weight ?? 0
        }
        let icons = qos.sync() { fetchIcons(session: session, assetsId: assets.map { $0.id }) }
        self.icons.merge(icons, uniquingKeysWith: {_, new in new})
    }
}
