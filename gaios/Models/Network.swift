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

    /// Get the asset used to pay transaction fees
    func getFeeAsset() -> String {
        return self.liquid ? self.policyAsset ?? "" : "btc"
    }

    var electrum: Bool {
        "electrum" == serverType
    }

    var multisig: Bool {
        !electrum
    }
}

var cachedNetworks: [String: Any]?
func getGdkNetwork(_ network: String, data: [String: Any]? = nil) -> GdkNetwork {
    if data ?? cachedNetworks == nil {
        cachedNetworks = try? getNetworks()
    }
    guard let res = data ?? cachedNetworks,
          let net = res[network] as? [String: Any],
          let jsonData = try? JSONSerialization.data(withJSONObject: net),
          var network = try? JSONDecoder().decode(GdkNetwork.self, from: jsonData)
    else {
        fatalError("invalid network")
    }
    network.icon = network.network.lowercased() == "mainnet" ? "ntw_btc" : "ntw_testnet"
    network.icon = network.liquid ? "ntw_liquid" : network.icon
    return network
}
