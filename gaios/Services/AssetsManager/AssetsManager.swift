import Foundation
import UIKit
import PromiseKit

class AssetsManager {
    static let liquid = AssetsManagerLiquid()
    static let elements = AssetsManagerLiquid()
    static let mainnet = AssetsManagerBitcoin()
    static let testnet = AssetsManagerTestnet()

    static func get(for network: GdkNetwork) -> AssetsManagerProtocol? {
        if network.liquid && network.mainnet {
            return AssetsManager.liquid
        } else if network.liquid {
            return AssetsManager.elements
        } else if network.mainnet {
            return AssetsManager.mainnet
        } else if !network.mainnet {
            return AssetsManager.testnet
        }
        return nil
    }
}
