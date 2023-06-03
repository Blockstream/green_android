import Foundation
import gdk

extension AccountType {
    var subtitle: String {
        if lightning {
            return "Lightning"
        } else if singlesig {
            return "Singlesig / \(shortString)"
        } else {
            return "Multisig / \(shortString)"
        }
    }
}
