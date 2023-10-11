import Foundation

import greenaddress

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
        case electrumUrl = "electrum_url"
        case electrumOnionUrl = "electrum_onion_url"
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
    public var electrumUrl: String?
    public var electrumOnionUrl: String?
    
    /// Get the asset used to pay transaction fees
    public func getFeeAsset() -> String {
        return self.liquid ? self.policyAsset ?? "" : "btc"
    }
    
    public var electrum: Bool {
        "electrum" == serverType
    }
    
    
    public var lightning: Bool {
        "breez" == serverType
    }
    
    public var singlesig: Bool {
        electrum
    }
    
    public var multisig: Bool {
        !electrum && !lightning
    }
    
    public var chain: String {
        network.replacingOccurrences(of: "electrum-", with: "")
            .replacingOccurrences(of: "lightning-", with: "")
    }
    
    public var defaultFee: UInt64 {
        liquid ? 100 : 1000
    }

    public static func < (lhs: GdkNetwork, rhs: GdkNetwork) -> Bool {
        let rules: [NetworkSecurityCase] = [.bitcoinSS, .testnetSS, .bitcoinMS, .testnetMS, .lightning, .liquidSS, .testnetLiquidSS, .liquidMS, .testnetLiquidMS]
        let lnet = NetworkSecurityCase(rawValue: lhs.network) ?? .bitcoinSS
        let rnet = NetworkSecurityCase(rawValue: rhs.network) ?? .bitcoinSS
        return rules.firstIndex(of: lnet) ?? 0 < rules.firstIndex(of: rnet) ?? 0
    }
}


public struct GdkNetworks {
    public static var shared = GdkNetworks()

    public lazy var bitcoinSS = getGdkNetwork(NetworkSecurityCase.bitcoinSS.network)
    public lazy var bitcoinMS = getGdkNetwork(NetworkSecurityCase.bitcoinMS.network)
    public lazy var testnetSS = getGdkNetwork(NetworkSecurityCase.testnetSS.network)
    public lazy var testnetMS = getGdkNetwork(NetworkSecurityCase.testnetMS.network)
    public lazy var liquidSS = getGdkNetwork(NetworkSecurityCase.liquidSS.network)
    public lazy var liquidMS = getGdkNetwork(NetworkSecurityCase.liquidMS.network)
    public lazy var testnetLiquidSS = getGdkNetwork(NetworkSecurityCase.testnetLiquidSS.network)
    public lazy var testnetLiquidMS = getGdkNetwork(NetworkSecurityCase.testnetLiquidMS.network)

    public lazy var lightning = GdkNetwork(name: NetworkSecurityCase.lightning.name(),
                                           network: NetworkSecurityCase.lightning.network,
                                           liquid: false,
                                           mainnet: true,
                                           development: false,
                                           txExplorerUrl: nil,
                                           serverType: "breez")
    public lazy var testnetLightning = GdkNetwork(name: NetworkSecurityCase.testnetLightning.name(),
                                                  network: NetworkSecurityCase.testnetLightning.network,
                                                  liquid: false,
                                                  mainnet: false,
                                                  development: false,
                                                  txExplorerUrl: nil,
                                                  serverType: "breez")

    public mutating func get(networkType: NetworkSecurityCase) -> GdkNetwork {
        switch networkType {
        case .bitcoinSS: return bitcoinSS
        case .bitcoinMS: return bitcoinMS
        case .testnetSS: return testnetSS
        case .testnetMS: return testnetMS
        case .liquidSS: return liquidSS
        case .liquidMS: return liquidMS
        case .testnetLiquidSS: return testnetLiquidSS
        case .testnetLiquidMS: return testnetLiquidMS
        case .lightning: return lightning
        case .testnetLightning: return testnetLightning
        }
    }

    public func get(network: String) -> GdkNetwork {
        if network == NetworkSecurityCase.lightning.network {
            return GdkNetworks.shared.lightning
        } else if network == NetworkSecurityCase.testnetLightning.network {
            return GdkNetworks.shared.testnetLightning
        } else {
            return GdkNetworks.shared.getGdkNetwork(network)
        }
    }

    private static var cachedNetworks: [String: Any]?
    private func getGdkNetwork(_ network: String, data: [String: Any]? = nil) -> GdkNetwork {
        if data ?? GdkNetworks.cachedNetworks == nil {
            GdkNetworks.cachedNetworks = try? getNetworks()
        }
        guard let res = data ?? GdkNetworks.cachedNetworks,
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
}
