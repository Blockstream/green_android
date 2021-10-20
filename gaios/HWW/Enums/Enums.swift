import UIKit

enum HWWState {
    case connecting
    case connected
    case connectFailed
    case selectNetwork
    case followDevice
    case upgradingFirmware
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
