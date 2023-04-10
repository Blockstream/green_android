import Foundation
import UIKit

enum NetworkSecurityCase: String, CaseIterable {
    case bitcoinMS = "mainnet"
    case bitcoinSS = "electrum-mainnet"
    case liquidMS = "liquid"
    case liquidSS = "electrum-liquid"
    case testnetMS = "testnet"
    case testnetSS = "electrum-testnet"
    case testnetLiquidMS = "testnet-liquid"
    case testnetLiquidSS = "electrum-testnet-liquid"

    var network: String {
        self.rawValue
    }

    var gdkNetwork: GdkNetwork? {
        getGdkNetwork(self.rawValue)
    }

    var chain: String {
        network.replacingOccurrences(of: "electrum-", with: "")
    }

    var singleSig: Bool {
        gdkNetwork?.electrum ?? true
    }

    func name() -> String {
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

    func icons() -> (UIImage, UIImage) {
        switch self {
        case .bitcoinMS:
            return (UIImage(named: "ic_keys_invert")!, UIImage(named: "ntw_btc")!)
        case .bitcoinSS:
            return (UIImage(named: "ic_key")!, UIImage(named: "ntw_btc")!)
        case .liquidMS:
            return (UIImage(named: "ic_keys_invert")!, UIImage(named: "ntw_liquid")!)
        case .liquidSS:
            return (UIImage(named: "ic_key")!, UIImage(named: "ntw_liquid")!)
        case .testnetMS:
            return (UIImage(named: "ic_keys_invert")!, UIImage(named: "ntw_testnet")!)
        case .testnetSS:
            return (UIImage(named: "ic_key")!, UIImage(named: "ntw_testnet")!)
        case .testnetLiquidMS:
            return (UIImage(named: "ic_keys_invert")!, UIImage(named: "ntw_testnet_liquid")!)
        case .testnetLiquidSS:
            return (UIImage(named: "ic_key")!, UIImage(named: "ntw_testnet_liquid")!)
        }
    }

    func color() -> UIColor {
        switch self {
        case .bitcoinMS, .bitcoinSS:
            return UIColor.accountOrange()
        case .liquidMS, .liquidSS:
            return UIColor.accountLightBlue()
        case .testnetMS, .testnetSS, .testnetLiquidMS, .testnetLiquidSS:
            return UIColor.accountGray()
        }
    }
}
