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
    var breezSdk: LightningBridge?

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
    
    var channelFee: String? {
        let channelFeeSatoshi = try? breezSdk?.openChannelFee(satoshi: Long(satoshi ?? 0))?.feeMsat.satoshi
        if let channelFeeSatoshi = channelFeeSatoshi {
            return Balance.fromSatoshi(channelFeeSatoshi, assetId: AssetInfo.btcId)?.toText(inputDenomination)
        }
        return nil
    }

    var onChaininfo: String? {
        return String(format: "Send more than %@ and up to %@ to this address. A minimum setup fee of %@ will be applied on the received amount.\n\nThis address can be used only once.".localized, onChainMin ?? "", onChainMax ?? "", channelFee ?? "")
    }
}
