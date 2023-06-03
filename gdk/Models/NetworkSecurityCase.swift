import Foundation
import UIKit

public enum NetworkSecurityCase: String, CaseIterable {
    case bitcoinMS = "mainnet"
    case bitcoinSS = "electrum-mainnet"
    case liquidMS = "liquid"
    case liquidSS = "electrum-liquid"
    case testnetMS = "testnet"
    case testnetSS = "electrum-testnet"
    case testnetLiquidMS = "testnet-liquid"
    case testnetLiquidSS = "electrum-testnet-liquid"
    
    case lightning = "lightning-mainnet"
    case testnetLightning = "lightning-testnet"
    

    public var network: String {
        self.rawValue
    }

    public var gdkNetwork: GdkNetwork {
        return GdkNetworks.shared.get(networkType: self)
    }

    public var chain: String {
        network.replacingOccurrences(of: "electrum-", with: "")
            .replacingOccurrences(of: "lightning-", with: "")
    }

    public var singlesig: Bool { gdkNetwork.singlesig }
    public var multisig: Bool { gdkNetwork.multisig }
    public var lightning: Bool { gdkNetwork.lightning }
    public var testnet: Bool { !gdkNetwork.mainnet }
    public var liquid: Bool { gdkNetwork.liquid }

    public func name() -> String {
        switch self {
        case .bitcoinMS:
            return "Multisig Bitcoin"
        case .bitcoinSS:
            return "Singlesig Bitcoin"
        case .liquidMS:
            return "Multisig Liquid"
        case .liquidSS:
            return "Singlesig Liquid"
        case .testnetMS:
            return "Multisig Testnet"
        case .testnetSS:
            return "Singlesig Testnet"
        case .testnetLiquidMS:
            return "Multisig Liquid Testnet"
        case .testnetLiquidSS:
            return "Singlesig Liquid Testnet"
        case .lightning:
            return "Lightning"
        case .testnetLightning:
            return "Lightning Testnet"
        }
    }
}
