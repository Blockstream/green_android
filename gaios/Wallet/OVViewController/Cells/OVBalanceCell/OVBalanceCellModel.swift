import Foundation
import UIKit

class OVBalanceCellModel {

    var value: String
    var numAssets: Int

    init(satoshi: UInt64, numAssets: Int) {
        self.numAssets = numAssets
        if let balance = Balance.fromSatoshi(satoshi)?.toDenom() {
            value = "\(balance.0) \(balance.1)"
        } else {
            value = "--"
        }
    }

    init() {
        numAssets = 0
        value = "--"
    }

    var fiatValue: String {
        return "0.0000 todo USD"
    }
}
