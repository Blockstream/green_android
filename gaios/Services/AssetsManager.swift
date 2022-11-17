import Foundation
import UIKit
import PromiseKit

class AssetsManager {

    let testnet: Bool
    var infos = [String: AssetInfo]()
    var icons = [String: String]()

    init(testnet: Bool) {
        self.testnet = testnet
    }

    static var btc: AssetInfo {
        let denomination = WalletManager.current?.prominentSession?.settings?.denomination
        let precision = UInt8(denomination?.digits ?? 8)
        let ticker = denomination?.string ?? "BTC"
        return AssetInfo(assetId: "btc", name: "Bitcoin", precision: precision, ticker: ticker)
    }

    static var tbtc: AssetInfo {
        let denomination = WalletManager.current?.prominentSession?.settings?.denomination
        let precision = UInt8(denomination?.digits ?? 8)
        let ticker = denomination?.string ?? "TEST"
        return AssetInfo(assetId: "btc", name: "Testnet", precision: precision, ticker: ticker)
    }
    static var lbtc: String { getGdkNetwork("liquid").getFeeAsset() }
    static var ltest: String { getGdkNetwork("testnet-liquid").getFeeAsset() }

    var session: SessionManager? {
        let liquid: NetworkSecurityCase = testnet ? .testnetLiquidSS : .liquidSS
        return WalletManager.current?.sessions[liquid.rawValue]
    }

    var allAssets: [String] {
        var assets = self.infos.map { $0.key }
        if testnet {
            assets += ["btc", AssetsManager.ltest]
        } else {
            assets += ["btc", AssetsManager.lbtc]
        }
        return assets
    }

    func info(for key: String) -> AssetInfo {
        if key == "btc" {
            return testnet ? AssetsManager.tbtc : AssetsManager.btc
        }
        if let session = session, infos[key] == nil {
            let infos = fetchAssets(session: session, assetsId: [key])
            self.infos.merge(infos, uniquingKeysWith: {_, new in new})
        }
        if var asset = infos[key] {
            if key == session?.gdkNetwork.getFeeAsset() ?? "" {
                asset.name = session?.gdkNetwork.mainnet ?? true ? "Liquid Bitcoin" : "Liquid Testnet Bitcoin"
            }
            return asset
        }
        return AssetInfo(assetId: key, name: nil, precision: 0, ticker: nil)
    }

    func getImage(for key: String) -> UIImage? {
        if key == "btc" {
            return UIImage(named: testnet ? "ntw_testnet" : "ntw_btc")
        }
        if let session = session, icons[key] == nil {
            let icons = fetchIcons(session: session, assetsId: [key])
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

    func loadAsync(session: SessionManager) {
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().compactMap(on: bgq) {
            _ = try session.refreshAssets(icons: false, assets: true, refresh: true)
            _ = try session.refreshAssets(icons: true, assets: false, refresh: true)
            self.fetchFromCountly()
        }.done { _ in
            let notification = NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue)
            NotificationCenter.default.post(name: notification, object: nil, userInfo: nil)
        }.catch { _ in
            print("Asset registry loading failure")
        }
    }

    func getAssetsFromCountly() -> [EnrichedAsset] {
        if let assets: [Any] = AnalyticsManager.shared.getRemoteConfigValue(key: Constants.countlyRemoteConfigAssets) as? [Any] {
            let json = try? JSONSerialization.data(withJSONObject: assets, options: [])
            let assets = try? JSONDecoder().decode([EnrichedAsset].self, from: json ?? Data())
            return assets ?? []
        }
        return []
    }

    func fetchFromCountly() {
        guard let session = session else { return }
        let assets = getAssetsFromCountly().map { $0.id }
        let infos = fetchAssets(session: session, assetsId: assets)
        self.infos.merge(infos, uniquingKeysWith: {_, new in new})
        let icons = fetchIcons(session: session, assetsId: assets)
        self.icons.merge(icons, uniquingKeysWith: {_, new in new})
    }
}
