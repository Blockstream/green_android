import Foundation
import UIKit
import PromiseKit

struct AssentEntity: Codable {
    let domain: String
}

struct AssetInfo: Codable {

    enum CodingKeys: String, CodingKey {
        case assetId = "asset_id"
        case name
        case precision
        case ticker
        case entity
    }

    var assetId: String
    var name: String
    var precision: UInt8?
    var ticker: String?
    var entity: AssentEntity?

    init(assetId: String, name: String, precision: UInt8, ticker: String) {
        self.assetId = assetId
        self.name = name
        self.precision = precision
        self.ticker = ticker
    }

    func encode() -> [String: Any]? {
        return try? JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any]
    }
}

class Registry: Codable {
    static let shared = Registry(infos: [:], icons: [:])
    var infos: [String: AssetInfo]
    var icons: [String: String]

    init(infos: [String: AssetInfo], icons: [String: String]) {
        self.infos = infos
        self.icons = icons
    }

    func image(for key: String?) -> UIImage? {
        let id = "btc" == key ? getGdkNetwork(getNetwork()).policyAsset! : key
        let icon = icons.filter { $0.key == id }.first
        if icon != nil {
            return UIImage(base64: icon!.value)
        }
        return UIImage(named: "default_asset_icon")
    }

    func cache() -> Promise<Void> {
        return refresh(refresh: false)
    }

    func refresh(refresh: Bool = true) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        if !getGdkNetwork(getNetwork()).liquid {
            return Promise<Void>()
        }
        return Promise().compactMap(on: bgq) { _ in
            try getSession().refreshAssets(params: ["icons": true, "assets": true, "refresh": refresh])
        }.map { data in
            var infosData = data["assets"] as? [String: Any]
            var iconsData = data["icons"] as? [String: String]
            if let modIndex = infosData?.keys.firstIndex(of: "last_modified") {
                infosData?.remove(at: modIndex)
            }
            if let modIndex = iconsData?.keys.firstIndex(of: "last_modified") {
                iconsData?.remove(at: modIndex)
            }
            let infosSer = try? JSONSerialization.data(withJSONObject: infosData ?? [:])
            let infos = try? JSONDecoder().decode([String: AssetInfo].self, from: infosSer ?? Data())
            self.infos = infos ?? [:]
            self.icons = iconsData ?? [:]
        }
    }
}
