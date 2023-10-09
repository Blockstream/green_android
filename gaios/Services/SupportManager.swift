import Foundation

class SupportManager {
    
    static let shared = SupportManager()
    
    func str() async -> String {
        let multiSigSessions = { WalletManager.current?.activeSessions.values.filter { !$0.gdkNetwork.electrum && !$0.gdkNetwork.lightning} }()
        let msMainSession = multiSigSessions?.filter{ !$0.gdkNetwork.liquid }.first
        let msLiquidSession = multiSigSessions?.filter{ $0.gdkNetwork.liquid }.first
        var strings: [String] = []
        
        if let item = try? await msMainSession?.subaccount(0) {
            strings.append("bitcoin:\(item.receivingId)")
        }
        if let item = try? await msLiquidSession?.subaccount(0) {
            strings.append("liquidnetwork:\(item.receivingId)")
        }
        if let nodeId = WalletManager.current?.lightningSession?.nodeState?.id {
            strings.append("lightning:\(nodeId)")
        }
        return strings.joined(separator: ",")
    }
}
