import Foundation
import gdk

extension WalletItem {
    
    var session: SessionManager? { WalletManager.current?.sessions[network ?? ""] }
    var lightningSession: LightningSessionManager? { WalletManager.current?.lightningSession }
    
    var localizedName: String {
        if !name.isEmpty {
            return name
        }
        let subaccounts = WalletManager.current?.subaccounts ?? []
        let subaccountsSameType = subaccounts.filter { $0.type == self.type && $0.network == self.network }
        let network = gdkNetwork.liquid ? " Liquid " : " "
        if subaccountsSameType.count > 1 {
            let index = subaccountsSameType.filter { $0.pointer < self.pointer }.count
            if index > 0 {
                return "\(type.string.localized)\(network)\(index+1)"
            }
        }
        return "\(type.string.localized)\(network)"
    }
}
