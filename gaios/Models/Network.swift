import Foundation
import PromiseKit

struct GdkNetwork: Codable, Equatable {

    enum CodingKeys: String, CodingKey {
        case name
        case network
        case liquid
        case development
        case txExplorerUrl = "tx_explorer_url"
        case icon
        case mainnet
        case policyAsset = "policy_asset"
        case serverType = "server_type"
        case csvBuckets = "csv_buckets"
    }

    let name: String
    let network: String
    let liquid: Bool
    let mainnet: Bool
    let development: Bool
    let txExplorerUrl: String?
    var icon: String?
    var policyAsset: String?
    var serverType: String?
    var csvBuckets: [Int]?
}

func getGdkNetwork(_ network: String, data: [String: Any]? = nil) -> GdkNetwork {
    let res = data ?? (try! getNetworks())
    let jsonData = try! JSONSerialization.data(withJSONObject: res![network]!)
    var network = try! JSONDecoder().decode(GdkNetwork.self, from: jsonData)
    network.icon = network.network.lowercased() == "mainnet" ? "btc" : "btc_testnet"
    network.icon = network.liquid ? "btc_liquid" : network.icon
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
        let serverType = net!["server_type"] as? String ?? ""
        let isElectrum = serverType == "electrum"
        return !development! && !isElectrum
        #endif
    }.map { name in
        return getGdkNetwork(name, data: data)
    }
}
