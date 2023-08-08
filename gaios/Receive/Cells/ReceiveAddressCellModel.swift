import Foundation
import UIKit
import gdk
import BreezSDK
import lightning

struct ReceiveAddressCellModel {
    var text: String?
    var type: ReceiveType
    var swapInfo: SwapInfo?

    var satoshi: Int64?
    var maxLimit: UInt64?
    var inputDenomination: DenominationType
    var nodeState: NodeState?
    var lspInfo: LspInformation?

    var btc: String? {
        if let satoshi = satoshi {
            if let res = Balance.fromSatoshi(satoshi, assetId: AssetInfo.btcId)?.toDenom(inputDenomination) {
                return "\(res.0) \(res.1)"
            }
        }
        return ""
    }

    var maxSendable: String? {
        if let maxLimit = maxLimit {
            if let res = Balance.fromSatoshi(UInt64(maxLimit), assetId: AssetInfo.btcId)?.toDenom(inputDenomination) {
                return "\(res.0) \(res.1)"
            }
        }
        return nil
    }

    var channelFeePercent: String? {
        if let channelFeePercent = lspInfo?.channelFeePercent {
            return "\(channelFeePercent)"
        }
        return nil
    }
    
    var channelMinFee: String? {
        if let channelMinimumFeeSatoshi = lspInfo?.channelMinimumFeeSatoshi {
            return Balance.fromSatoshi(channelMinimumFeeSatoshi, assetId: AssetInfo.btcId)?.toText(inputDenomination)
        }
        return nil
    }
    
    var onChainMin: String? {
        if let minAllowedDeposit = swapInfo?.minAllowedDeposit {
            return Balance.fromSatoshi(minAllowedDeposit, assetId: AssetInfo.btcId)?.toText(inputDenomination)
        }
        return nil
    }
    var onChainMax: String? {
        if let maxAllowedDeposit = swapInfo?.maxAllowedDeposit {
            return Balance.fromSatoshi(maxAllowedDeposit, assetId: AssetInfo.btcId)?.toText(inputDenomination)
        }
        return nil
    }

    var onChaininfo: String? {
        return "Send more than \(onChainMin ?? "") and up to \(onChainMax ?? "") to this address. A setup fee of \(channelFeePercent ?? "")% with a minimum of \(channelMinFee ?? "") will be applied on the received amount.\n\nThis address can be used only once."
    }
}
