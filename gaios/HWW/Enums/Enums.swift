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

    func name() -> String {
        switch self {
        case .bitcoin:
            return "Bitcoin"
        case .liquid:
            return "Liquid"
        case .testnet:
            return "Testnet"
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
        }
    }
}
