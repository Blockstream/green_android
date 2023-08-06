import Foundation
import gdk

enum NodeCellType: CaseIterable {
    case id
    case channelsBalance
    case inboundLiquidity
    case maxPayble
    case maxSinglePaymentAmount
    case maxReceivable
    case connectedPeers
    case closeChannels
}

class DialogNodeViewModel {

    var lightningSession: LightningSessionManager
    
    

    var cells: [NodeCellType] {
        var list = NodeCellType.allCases
        if lightningSession.nodeState?.channelsBalanceSatoshi ?? 0 == 0 {
            list.removeAll { $0 == NodeCellType.closeChannels }
        }
        return list
    }


    var id: String {
        return lightningSession.nodeState?.id ?? ""
    }

    var channelsBalance: String {
        return asStr(satoshi: lightningSession.nodeState?.channelsBalanceSatoshi)
    }

    var inboundLiquidity: String {
        return asStr(satoshi: lightningSession.nodeState?.inboundLiquiditySatoshi)
    }

    var maxPayble: String {
        return asStr(satoshi: lightningSession.nodeState?.maxPaybleSatoshi)
    }

    var maxSinglePaymentAmount: String {
        return asStr(satoshi: lightningSession.nodeState?.maxSinglePaymentAmountSatoshi)
    }

    var maxReceivable: String {
        return asStr(satoshi: lightningSession.nodeState?.maxReceivableSatoshi)
    }

    var connectedPeers: String {
        return lightningSession.nodeState?.connectedPeers.joined(separator: ", ") ?? ""
    }

    var closeChannels: String {
        return "Tap to close channels"
    }

    init(lightningSession: LightningSessionManager) {
        self.lightningSession = lightningSession
    }

    func asStr(satoshi: UInt64?) -> String {
        let satoshi = satoshi ?? 0
        
        if let balance = Balance.fromSatoshi(satoshi, assetId: AssetInfo.btcId) {
            let (amount, denom) = balance.toDenom()
            return "\(amount) \(denom)"
        }
        return ""
    }
}
