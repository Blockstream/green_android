import Foundation

enum EventType: String {
    case Block = "block"
    case Transaction = "transaction"
    case TwoFactorReset = "twofactor_reset"
    case Settings = "settings"
    case AddressChanged = "address_changed"
    case Network = "network"
    case SystemMessage = "system_message"
    case Tor = "tor"
    case AssetsUpdated = "assets_updated"
    case Session = "session"
}
