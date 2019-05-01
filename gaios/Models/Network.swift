import Foundation
import PromiseKit

struct GdkNetwork: Codable {

    enum CodingKeys: String, CodingKey {
        case name
        case network
        case development
        case txExplorerUrl = "tx_explorer_url"
        case icon
    }

    let name: String
    let network: String
    let development: Bool
    let txExplorerUrl: String
    var icon: String?
}

func getGdkNetwork(_ network: String, data: [String: Any]? = nil) -> GdkNetwork {
    let res = data ?? (try! getNetworks())
    let jsonData = try! JSONSerialization.data(withJSONObject: res![network]!)
    var network = try! JSONDecoder().decode(GdkNetwork.self, from: jsonData)
    network.icon = network.network.lowercased() == "mainnet" ? "btc" : "btc_testnet"
    return network
}

func getGdkNetworks() -> [GdkNetwork] {
    let data = try! getNetworks()
    let list = data!["all_networks"] as? [String] ?? []
    return list.filter {
        #if DEBUG
        return true
        #else
        let net = data![$0] as? [String: Any]
        let development = net!["development"] as? Bool
        return !development!
        #endif
    }.map { name in
        return getGdkNetwork(name, data: data)
    }
}
