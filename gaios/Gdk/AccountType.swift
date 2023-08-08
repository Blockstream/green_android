import Foundation
import gdk

extension AccountType {
    var network: String {
        if lightning {
            return "Lightning"
        } else if singlesig {
            return "Singlesig"
        } else {
            return "Multisig"
        }
    }
    var shortText: String {
        if lightning {
            return "Fastest"
        } else {
            return "\(shortString)"
        }
    }
    var longText: String {
        if lightning {
            return "Fastest"
        } else {
            return "\(string)"
        }
    }
    var path: String {
        if lightning {
            return network
        } else {
            return "\(network) / \(longText)"
        }
    }
}
