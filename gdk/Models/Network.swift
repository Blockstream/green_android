import Foundation
import PromiseKit

public struct GdkNetwork: Codable, Equatable, Comparable {

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
        case bip21Prefix = "bip21_prefix"
    }

    public let name: String
    public let network: String
    public let liquid: Bool
    public let mainnet: Bool
    public let development: Bool
    public let txExplorerUrl: String?
    public var icon: String?
    public var policyAsset: String?
    public var serverType: String?
    public var csvBuckets: [Int]?
    public var bip21Prefix: String?

    /// Get the asset used to pay transaction fees
    public func getFeeAsset() -> String {
        return self.liquid ? self.policyAsset ?? "" : "btc"
    }

    public var electrum: Bool {
        "electrum" == serverType
    }

    public var multisig: Bool {
        !electrum
    }

    public var chain: String {
        network.replacingOccurrences(of: "electrum-", with: "")
    }

    public static func < (lhs: GdkNetwork, rhs: GdkNetwork) -> Bool {
        if lhs.liquid && !rhs.liquid { return false }
        if !lhs.liquid && rhs.liquid { return true }
        return lhs.electrum
    }
}

var cachedNetworks: [String: Any]?
public func getGdkNetwork(_ network: String, data: [String: Any]? = nil) -> GdkNetwork {
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
