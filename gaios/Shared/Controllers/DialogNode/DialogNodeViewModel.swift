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
}

class DialogNodeViewModel {

    var lightningSession: LightningSessionManager
    
    

    var cells: [NodeCellType] {
        return NodeCellType.allCases
    }

    var hideBtnClose: Bool {
        return (lightningSession.nodeState?.channelsBalanceSatoshi ?? 0 == 0)
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
