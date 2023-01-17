import Foundation
import PromiseKit

// Section of settings
enum WOSection: String, Codable, CaseIterable {
    case Multisig = "Multisig"
    case Singlesig = "Singlesig"
}

struct WOSettingsItem {
    var title: String
    var subtitle: String
    var network: GdkNetwork
}
