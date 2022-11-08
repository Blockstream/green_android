import Foundation
import UIKit

class BalanceCellModel {

    var value: String
    var numAssets: Int
    var valueFiat: String
    var cachedBalance: [(String, Int64)]

    init(satoshi: Int64, numAssets: Int, cachedBalance: [(String, Int64)]) {
        self.cachedBalance = cachedBalance
        self.numAssets = numAssets
        if let balance = Balance.fromSatoshi(satoshi)?.toDenom() {
            value = "\(balance.0) \(balance.1)"
        } else {
            value = "--"
        }

        if let balance = Balance.fromSatoshi(satoshi)?.toFiat() {
            self.valueFiat = "\(balance.0) \(balance.1)"
        } else {
            valueFiat = "--"
        }
    }
}
