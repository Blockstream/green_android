import Foundation
import UIKit
import BreezSDK
import gdk

struct LTAmountCellModel {
    var satoshi: Int64?
    var maxLimit: UInt64?
    var isFiat: Bool
    var inputDenomination: gdk.DenominationType
    var gdkNetwork: gdk.GdkNetwork?
    var nodeState: NodeState?
    var lspInfo: LspInformation?

    var amountText: String? { isFiat ? fiat : btc }
    var denomText: String? {
        if isFiat {
            return currency == nil ? defaultCurrency : currency
        } else {
            if let gdkNetwork = gdkNetwork {
                return inputDenomination.string(for: gdkNetwork)
            } else {
                return defaultDenomination
            }
        }
    }
    var denomUnderlineText: NSAttributedString {
        return NSAttributedString(string: denomText ?? "", attributes:
            [.underlineStyle: NSUnderlineStyle.single.rawValue])
    }
    
    var btc: String? {
        if let satoshi = satoshi {
            return Balance.fromSatoshi(satoshi, assetId: AssetInfo.btcId)?.toDenom(inputDenomination).0
        }
        return nil
    }
    var fiat: String? {
        if let satoshi = satoshi {
            return Balance.fromSatoshi(satoshi, assetId: AssetInfo.btcId)?.toFiat().0
        }
        return nil
    }
    var currency: String? {
        if let satoshi = satoshi {
            return Balance.fromSatoshi(satoshi, assetId: AssetInfo.btcId)?.toFiat().1
        }
        return nil
    }
    
    var maxLimitAmount: String? {
        if let maxLimit = maxLimit {
            return Balance.fromSatoshi(UInt64(maxLimit), assetId: AssetInfo.btcId)?.toDenom(inputDenomination).0
        }
        return nil
    }
    var state: LTAmountCellState {
        guard let satoshi = satoshi else {
            return .disabled
        }
        guard let lspInfo = lspInfo, let nodeState = nodeState else {
            return .disconnected
        }
        if satoshi >= nodeState.maxReceivableSatoshi {
            return .tooHigh
        } else if satoshi <= nodeState.inboundLiquiditySatoshi || satoshi >= lspInfo.channelMinimumFeeSatoshi {
            if nodeState.inboundLiquiditySatoshi == 0 || satoshi > nodeState.inboundLiquiditySatoshi {
                return .validFunding
            } else {
                return .valid
            }
        } else if satoshi <= lspInfo.channelMinimumFeeSatoshi {
            return .tooLow
        } else {
            return .disabled
        }
    }
    var defaultCurrency: String? = {
        return Balance.fromSatoshi(0, assetId: AssetInfo.btcId)?.toFiat().1
    }()
    var defaultDenomination: String? = {
        return Balance.fromSatoshi(0, assetId: AssetInfo.btcId)?.toDenom().1
    }()

    var channelFee: Int64? {
        let feeVariable = (Float((lspInfo?.channelFeePercent ?? 0)) / 100) * Float(satoshi ?? 0)
        let fee = Int64(feeVariable)
        return fee > lspInfo?.channelMinimumFeeSatoshi ?? 0 ? fee : lspInfo?.channelMinimumFeeSatoshi
    }
    
    func toFiatText(_ amount: Int64?) -> String? {
        if let amount = amount {
            return Balance.fromSatoshi(amount, assetId: AssetInfo.btcId)?.toFiatText()
        }
        return nil
    }
    func toBtcText(_ amount: Int64?) -> String? {
        if let amount = amount {
            return Balance.fromSatoshi(amount, assetId: AssetInfo.btcId)?.toText(inputDenomination)
        }
        return nil
    }
}
