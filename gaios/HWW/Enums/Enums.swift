import UIKit

enum HWWState {
    case connecting
    case connected
    case connectFailed
    case selectNetwork
    case followDevice
    case upgradingFirmware
    case upgradedFirmware
    case initialized
}

enum AvailableNetworks: String, CaseIterable {
    case bitcoin = "mainnet"
    case liquid = "liquid"
    case testnet = "testnet"
    case testnetLiquid = "testnet-liquid"

    func name() -> String {
        switch self {
        case .bitcoin:
            return "Bitcoin"
        case .liquid:
            return "Liquid"
        case .testnet:
            return "Testnet"
        case .testnetLiquid:
            return "Liquid Testnet"
        }
    }

    func icon() -> UIImage {
        switch self {
        case .bitcoin:
            return UIImage(named: "ntw_btc")!
        case .liquid:
            return UIImage(named: "ntw_liquid")!
        case .testnet:
            return UIImage(named: "ntw_testnet")!
        case .testnetLiquid:
            return UIImage(named: "ntw_testnet_liquid")!
        }
    }

    func color() -> UIColor {
        switch self {
        case .bitcoin:
            return UIColor.accountOrange()
        case .liquid:
            return UIColor.accountLightBlue()
        case .testnet:
            return UIColor.accountGray()
        case .testnetLiquid:
            return UIColor.accountGray()
        }
    }
}

enum NetworkSecurityCase: String, CaseIterable {
    case bitcoinMS
    case bitcoinSS
    case liquidMS
    case liquidSS
    case testnetMS
    case testnetSS
    case testnetLiquidMS
    case testnetLiquidSS

    func network() -> String {
        switch self {
        case .bitcoinMS:
            return "mainnet"
        case .bitcoinSS:
            return "electrum-mainnet"
        case .liquidMS:
            return "liquid"
        case .liquidSS:
            return "electrum-liquid"
        case .testnetMS:
            return "testnet"
        case .testnetSS:
            return "electrum-testnet"
        case .testnetLiquidMS:
            return "testnet-liquid"
        case .testnetLiquidSS:
            return "electrum-testnet-liquid"
        }
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
