import Foundation

public enum EventType: String, CaseIterable, Codable {
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
    case Ticker = "ticker"
    case InvoicePaid = "invoice_paid"
    case PaymentSucceed = "payment_succeed"
    case PaymentFailed = "payment_failed"
}
