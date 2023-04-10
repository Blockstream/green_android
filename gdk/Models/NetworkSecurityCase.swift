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

    public var network: String {
        self.rawValue
    }

    public var gdkNetwork: GdkNetwork? {
        getGdkNetwork(self.rawValue)
    }

    public var chain: String {
        network.replacingOccurrences(of: "electrum-", with: "")
    }

    public var singleSig: Bool {
        gdkNetwork?.electrum ?? true
    }

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
        }
    }
}
