import Foundation
import UIKit

import gdk

class AssetsManager {

    private let testnet: Bool
    private var infos = [String: AssetInfo?]()
    private var icons = [String: String?]()
    private var uncached = [String]()
    private var session: SessionManager?

    init(testnet: Bool) {
        self.testnet = testnet
        if testnet {
            infos = [AssetInfo.testId: AssetInfo.test,
                     AssetInfo.ltestId: AssetInfo.ltest]
        } else {
            infos = [AssetInfo.btcId: AssetInfo.btc,
                     AssetInfo.lbtcId: AssetInfo.lbtc]
        }
    }

    var all: [AssetInfo] {
        return infos.compactMap { $0.value }
    }

    func getAsset(for key: String) {
        let assets = session?.getAssets(params: GetAssetsParams(assetsId: [key]))
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

    func load(session: SessionManager?) async throws {
        self.session = session ?? self.session
        try await self.session?.connect()
        try await self.fetchFromCountly(session: self.session)
        _ = try await self.session?.refreshAssets(icons: true, assets: true, refresh: true)
        let notification = NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue)
        NotificationCenter.default.post(name: notification, object: nil, userInfo: nil)
    }

    func getAssetsFromCountly() async throws -> [EnrichedAsset] {
        let assets = AnalyticsManager.shared.getRemoteConfigValue(key: Constants.countlyRemoteConfigAssets) as? [[String: Any]]
        let json = try? JSONSerialization.data(withJSONObject: assets ?? [], options: [])
        let res = try? JSONDecoder().decode([EnrichedAsset].self, from: json ?? Data())
        return res ?? []
    }

    func fetchFromCountly(session: SessionManager?) async throws {
        let assets = try await getAssetsFromCountly()
        let res = session?.getAssets(params: GetAssetsParams(assetsId: assets.map { $0.id }))
        self.infos.merge(res?.assets ?? [:], uniquingKeysWith: {_, new in new})
        self.icons.merge(res?.icons ?? [:], uniquingKeysWith: {_, new in new})
        assets.forEach {
            self.infos[$0.id]??.amp = $0.amp ?? false
            self.infos[$0.id]??.weight = $0.weight ?? 0
        }
    }

    func sortAssets(lhs: String, rhs: String) -> Bool {
        if [AssetInfo.btcId, AssetInfo.testId].contains(lhs) { return true }
        if [AssetInfo.btcId, AssetInfo.testId].contains(rhs) { return false }
        if [AssetInfo.lbtcId, AssetInfo.ltestId].contains(lhs) { return true }
        if [AssetInfo.lbtcId, AssetInfo.ltestId].contains(rhs) { return false }
        let lhsImage = icons[lhs] != nil
        let rhsImage = icons[rhs] != nil
        if lhsImage && !rhsImage { return true }
        if !lhsImage && rhsImage { return false }
        let lhsInfo = infos[lhs]
        let rhsInfo = infos[rhs]
        if lhsInfo??.ticker != nil && rhsInfo??.ticker == nil { return true }
        if lhsInfo??.ticker == nil && rhsInfo??.ticker != nil { return false }
        let lhsw = lhsInfo??.weight ?? 0
        let rhsw = rhsInfo??.weight ?? 0
        if lhsw != rhsw {
            return lhsw > rhsw
        }
        return lhs < rhs
    }
}
