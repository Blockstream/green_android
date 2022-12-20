import Foundation
import UIKit

enum BalanceDisplayMode {
    case denom
    case fiat
    case btc

    func next(_ isBTC: Bool) -> BalanceDisplayMode {
        switch self {
        case .denom:
            return .fiat
        case .fiat:
            return .btc
        case .btc:
            if isBTC {
                return .fiat
            }
            return .denom
        }
    }
}

class BalanceCellModel {

    var value: String
    var valueChange: String
    var cachedBalance: [(String, Int64)]

    init(satoshi: Int64,
         cachedBalance: [(String, Int64)],
         mode: BalanceDisplayMode) {
        self.cachedBalance = cachedBalance
        self.value = ""
        self.valueChange = ""

        value = getValues(satoshi: satoshi, mode: mode).0
        valueChange = getValues(satoshi: satoshi, mode: mode).1
    }

    func getValues(satoshi: Int64, mode: BalanceDisplayMode) -> (String, String) {

        var valueToBTC = "--"
        var valueToDenom = "--"
        var valueToFiat = "--"

        if let balance = Balance.fromSatoshi(satoshi)?.toDenom() {
            valueToDenom = "\(balance.0) \(balance.1)"
        }

        if let balance = Balance.fromSatoshi(satoshi)?.toFiat() {
            valueToFiat = "\(balance.0) \(balance.1)"
        }

        if let balance = Balance.fromSatoshi(satoshi)?.toBTC() {
            valueToBTC = "\(balance.0) \(balance.1)"
        }

        print(mode)
        print(valueToDenom)
        print(valueToFiat)
        print(valueToBTC)

        switch mode {
        case .btc:
            return (valueToBTC, valueToFiat)
        case .denom:
            return (valueToDenom, valueToFiat)
        case .fiat:
            return (valueToFiat, valueToDenom)

        }
    }
}
