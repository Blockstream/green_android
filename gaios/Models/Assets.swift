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

func refreshAssets() -> Promise<[String: AssetInfo]?> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) { _ in
        try getSession().refreshAssets()
    }.compactMap(on: bgq) { data in
        let jsonData = try JSONSerialization.data(withJSONObject: data)
        return try! JSONDecoder().decode([String: AssetInfo].self, from: jsonData)
    }
}
