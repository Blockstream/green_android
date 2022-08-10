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

    var btc: AssetInfo {
        let denomination = WalletManager.current?.currentSession?.settings?.denomination
        let precision = UInt8(denomination?.digits ?? 8)
        let ticker = denomination?.string ?? "BTC"
        return AssetInfo(assetId: "btc", name: "Bitcoin", precision: precision, ticker: ticker)
    }

    var tbtc: AssetInfo {
        let denomination = WalletManager.current?.currentSession?.settings?.denomination
        let precision = UInt8(denomination?.digits ?? 8)
        let ticker = denomination?.string ?? "TEST"
        return AssetInfo(assetId: "btc", name: "Testnet", precision: precision, ticker: ticker)
    }

    func info(for key: String) -> AssetInfo {
        if key == "btc" {
            return testnet ? tbtc : btc
        }
        let liquid: NetworkSecurityCase = testnet ? .testnetLiquidSS : .liquidSS
        let session = WalletManager.current?.sessions[liquid.rawValue]
        if infos[key] == nil {
            let infos = fetchAssets(session: session!, assetsId: [key])
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

    func image(for key: String) -> UIImage {
        if key == "btc" {
            return UIImage(named: testnet ? "ntw_testnet" : "ntw_btc") ?? UIImage()
        }
        if icons[key] == nil {
            let liquid: NetworkSecurityCase = testnet ? .testnetLiquidSS : .liquidSS
            let session = WalletManager.current?.sessions[liquid.rawValue]
            let icons = fetchIcons(session: session!, assetsId: [key])
            self.icons.merge(icons, uniquingKeysWith: {_, new in new})
        }
        if let icon = icons[key] {
            return UIImage(base64: icon) ?? UIImage()
        }
        return UIImage(named: "default_asset_icon") ?? UIImage()
    }

    func hasImage(for key: String?) -> Bool {
        return icons.filter({ $0.key == key }).first != nil
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
        }.done { _ in
            let notification = NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue)
            NotificationCenter.default.post(name: notification, object: nil, userInfo: nil)
        }.catch { _ in
            print("Asset registry loading failure")
        }
    }
}
