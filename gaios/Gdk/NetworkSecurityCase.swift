import Foundation
import UIKit
import gdk

extension gdk.NetworkSecurityCase {

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
        case .lightning, .testnetLightning:
            return (UIImage(named: "ic_key")!, UIImage(named: "ntw_btc")!)
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
        case .lightning, .testnetLightning:
            return UIColor.yellow
        }
    }
}
