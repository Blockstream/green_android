import Foundation
import UIKit
import gdk

enum BalanceDisplayMode {
    case denom
    case fiat

    func next(_ isBTC: Bool) -> BalanceDisplayMode {
        switch self {
        case .denom:
            return .fiat
        case .fiat:
            return .denom
        }
    }
}

class BalanceCellModel {

    var value: String
    var valueChange: String
    var assetId: String
    var cachedBalance: AssetAmountList

    init(satoshi: Int64,
         cachedBalance: AssetAmountList,
         mode: BalanceDisplayMode,
         assetId: String) {
        self.cachedBalance = cachedBalance
        self.value = ""
        self.valueChange = ""
        self.assetId = assetId

        value = getValues(satoshi: satoshi, mode: mode, assetId: assetId).0
        valueChange = getValues(satoshi: satoshi, mode: mode, assetId: assetId).1
    }

    func getValues(satoshi: Int64, mode: BalanceDisplayMode, assetId: String) -> (String, String) {

        var valueToDenom = "--"
        var valueToFiat = "--"

        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId)?.toDenom() {
            valueToDenom = "\(balance.0) \(balance.1)"
        }

        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId)?.toFiat() {
            valueToFiat = "\(balance.0) \(balance.1)"
        }

        switch mode {
        case .denom:
            return (valueToDenom, valueToFiat)
        case .fiat:
            return (valueToFiat, valueToDenom)
        }
    }
}
