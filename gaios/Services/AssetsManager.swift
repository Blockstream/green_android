import Foundation
import UIKit
import PromiseKit

class AssetsManager: Codable {

    static let liquid = AssetsManager()
    static let elements = AssetsManager()

    var infos: [String: AssetInfo]
    var icons: [String: String]

    var iconsTask: Bool = false
    var assetsTask: Bool = false

    init(infos: [String: AssetInfo] = [:], icons: [String: String] = [:]) {
        self.infos = infos
        self.icons = icons
    }

    func image(for key: String?) -> UIImage? {
        if let icon = icons.filter({ $0.key == key }).first {
            // read icon from memory
            return UIImage(base64: icon.value)
        }
        return UIImage(named: "default_asset_icon")
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
