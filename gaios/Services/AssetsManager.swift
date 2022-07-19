import Foundation
import UIKit
import PromiseKit

protocol AssetsManagerProtocol {
    func info(for key: String) -> AssetInfo
    func image(for key: String) -> UIImage
    func hasImage(for key: String?) -> Bool
    func cache(session: SessionManager)
    func refresh(session: SessionManager)
    func loadAsync(session: SessionManager)
}

class AssetsManager {
    static let liquid = AssetsManagerLiquid()
    static let elements = AssetsManagerLiquid()
    static let mainnet = AssetsManagerBitcoin()
    static let testnet = AssetsManagerTestnet()

    static func get(for network: GdkNetwork) -> AssetsManagerProtocol? {
        if network.liquid && network.mainnet {
            return AssetsManager.liquid
        } else if network.liquid {
            return AssetsManager.elements
        } else if network.mainnet {
            return AssetsManager.mainnet
        } else if !network.mainnet {
            return AssetsManager.testnet
        }
        return nil
    }
}

class AssetsManagerBitcoin: Codable, AssetsManagerProtocol {
    func info(for key: String) -> AssetInfo {
        let denomination = SessionsManager.current?.settings?.denomination
        let precision = UInt8(denomination?.digits ?? 8)
        let ticker = denomination?.string ?? "BTC"
        return AssetInfo(assetId: "btc", name: "Bitcoin", precision: precision, ticker: ticker)
    }
    func image(for key: String) -> UIImage {
        return UIImage(named: "ntw_btc") ?? UIImage()
    }
    func hasImage(for key: String?) -> Bool { true }
    func cache(session: SessionManager) { }
    func refresh(session: SessionManager) { }
    func loadAsync(session: SessionManager) { }
}

class AssetsManagerTestnet: Codable, AssetsManagerProtocol {
    func info(for key: String) -> AssetInfo {
        let denomination = SessionsManager.current?.settings?.denomination
        let precision = UInt8(denomination?.digits ?? 8)
        let ticker = denomination?.string ?? "TEST"
        return AssetInfo(assetId: "btc", name: "Testnet", precision: precision, ticker: ticker)
    }
    func image(for key: String) -> UIImage {
        return UIImage(named: "ntw_testnet") ?? UIImage()
    }
    func hasImage(for key: String?) -> Bool { true }
    func cache(session: SessionManager) { }
    func refresh(session: SessionManager) { }
    func loadAsync(session: SessionManager) { }
}

class AssetsManagerLiquid: Codable, AssetsManagerProtocol {

    var infos: [String: AssetInfo]
    var icons: [String: String]
    var iconsTask: Bool = false
    var assetsTask: Bool = false

    init(infos: [String: AssetInfo] = [:], icons: [String: String] = [:]) {
        self.infos = infos
        self.icons = icons
    }

    func info(for key: String) -> AssetInfo {
        if var info = infos[key] {
            if key == SessionsManager.current?.account?.gdkNetwork?.getFeeAsset() {
                info.name = "Liquid Bitcoin"
            }
            return info
        }
        return AssetInfo(assetId: key, name: nil, precision: 0, ticker: nil)
    }

    func image(for key: String) -> UIImage {
        UIImage(base64: icons[key]) ?? UIImage(named: "default_asset_icon") ?? UIImage()
    }

    func hasImage(for key: String?) -> Bool {
        return icons.filter({ $0.key == key }).first != nil
    }

    @discardableResult
    func fetchIcons(session: SessionManager, refresh: Bool) -> Bool {
        let data = try? session.refreshAssets(params: ["icons": true, "assets": false, "refresh": refresh])
        let iconsData = data?["icons"] as? [String: String]
        self.icons = iconsData ?? [:]
        return iconsData != nil
    }

    @discardableResult
    func fetchAssets(session: SessionManager, refresh: Bool) -> Bool {
        let data = try? session.refreshAssets(params: ["icons": false, "assets": true, "refresh": refresh])
        let infosData = data?["assets"] as? [String: Any]
        let infosSer = try? JSONSerialization.data(withJSONObject: infosData ?? [:])
        let infos = try? JSONDecoder().decode([String: AssetInfo].self, from: infosSer ?? Data())
        self.infos = infos ?? [:]
        return infos != nil
    }

    func cache(session: SessionManager) {
        fetchAssets(session: session, refresh: false)
        fetchIcons(session: session, refresh: false)
    }

    func refresh(session: SessionManager) {
        if !assetsTask {
            assetsTask = fetchAssets(session: session, refresh: true)
            iconsTask = fetchIcons(session: session, refresh: true)
        }
    }

    func loadAsync(session: SessionManager) {
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().compactMap(on: bgq) {
            self.refresh(session: session)
        }.done { _ in
            let notification = NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue)
            NotificationCenter.default.post(name: notification, object: nil, userInfo: nil)
        }.catch { _ in
            print("Asset registry loading failure")
        }
    }
}
