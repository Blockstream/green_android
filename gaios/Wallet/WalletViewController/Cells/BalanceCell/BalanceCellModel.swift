import Foundation
import UIKit

class BalanceCellModel {

    var value: String
    var numAssets: Int
    var valueFiat: String

    init(satoshi: Int64, numAssets: Int) {
        self.numAssets = numAssets
        if let balance = Balance.fromSatoshi(satoshi)?.toDenom() {
            value = "\(balance.0) \(balance.1)"
        } else {
            value = "--"
        }
        valueFiat = "-- USD"
    }

    var fiatValue: String {
        return "0.0000 todo USD"
    }
}
