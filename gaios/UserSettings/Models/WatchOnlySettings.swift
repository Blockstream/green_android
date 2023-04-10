import Foundation
import PromiseKit
import UIKit
import gdk

// Section of settings
enum WOSection: String, Codable, CaseIterable {
    case Multisig
    case Singlesig

    var icon: UIImage {
        switch self {
        case .Multisig:
            return UIImage(named: "ic_keys_invert")!
        case .Singlesig:
            return UIImage(named: "ic_key")!
        }
    }
}

struct WOSettingsItem {
    var title: String
    var subtitle: String
    var network: GdkNetwork
}
