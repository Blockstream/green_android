import Foundation
import UIKit
import gdk

struct LTAmountCellModel {
    var satoshi: Int64? {
        didSet {
            if let satoshi = satoshi, let balance = Balance.fromSatoshi(satoshi, assetId: AssetInfo.btcId) {
                (fiat, currency) = balance.toFiat()
            }
            if let satoshi = satoshi, let balance = Balance.fromSatoshi(satoshi, assetId: AssetInfo.btcId) {
                (amount, denom) = balance.toDenom()
            }
        }
    }
    var maxLimit: UInt64? {
        didSet {
            if let maxLimit = maxLimit, let balance = Balance.fromSatoshi(UInt64(maxLimit), assetId: AssetInfo.btcId) {
                (maxLimitAmount, denom) = balance.toDenom()
            }
        }
    }
    var isFiat: Bool = false
    var amountText: String? { isFiat ? fiat : amount }
    var denomText: NSAttributedString {
        var txt: String?
        if isFiat {
            txt = currency == nil ? defaultCurrency : currency
        } else {
            if let inputDenomination = inputDenomination {
                txt = inputDenomination.rawValue
            } else {
                txt = denom
            }
        }
        return NSAttributedString(string: txt ?? "", attributes:
            [.underlineStyle: NSUnderlineStyle.single.rawValue])
    }
    var fiat: String? = nil
    var currency: String? = nil
    var maxLimitAmount: String? = nil
    var denom: String? = nil
    var amount: String? = nil
    var channelFeePercent: Float? = nil
    var channelMinFee: Int64? = nil
    var inboundLiquidity: UInt64? = nil
    var state: LTAmountCellState {
        if satoshi ?? 0 > maxLimit ?? 0 {
            return .invalid
        } else if satoshi ?? 0 > inboundLiquidity ?? 0 {
            return .valid
        } else {
            return .disabled
        }
    }
    var defaultCurrency: String {
        var currency = ""
        if let balance = Balance.fromSatoshi(0, assetId: AssetInfo.btcId) {
            (_, currency) = balance.toFiat()
        }
        return currency
    }
    var inputDenomination: gdk.DenominationType?
}
