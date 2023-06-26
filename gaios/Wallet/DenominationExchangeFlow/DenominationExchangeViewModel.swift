import Foundation
import gdk

class DenominationExchangeViewModel {

    var wm: WalletManager { WalletManager.current! }
    var session: SessionManager? { wm.prominentSession }
    var settings: Settings? { session?.settings }
    var editingDenomination: DenominationType?
    var editingExchange: CurrencyItem?

    func currentSymbol() -> DenominationType {
        guard let settings = settings else { return .BTC }
        return editingDenomination != nil ? editingDenomination! : settings.denomination
    }

    func currentSymbolStr() -> String {
        let denominations = DenominationType.denominationsBTC
        if let denom = denominations.filter({ $0.key == currentSymbol() }).first?.value {
            return denom
        }
        return denominations[.BTC]!
    }

    func currentExchange() -> String {
        if let editingExchange = editingExchange {
            return "\(editingExchange.currency) \("id_from".localized.lowercased()) \(editingExchange.exchange.uppercased())"
        }
        guard let settings = settings else { return "" }
        return "\(settings.pricing["currency"]!) \("id_from".localized.lowercased()) \(settings.pricing["exchange"]!.uppercased())"
    }

    func pricing() -> [String: String]? {
        if let editingExchange = editingExchange {
            var pricing: [String: String] = [:]
            pricing["currency"] = editingExchange.currency
            pricing["exchange"] = editingExchange.exchange
            return pricing
        }
        return nil
    }

    func dialogDenominationViewModel() -> DialogDenominationViewModel? {
        guard let session = session, let settings = session.settings else { return nil }

        let list: [DenominationType] = [ .BTC, .MilliBTC, .MicroBTC, .Bits, .Sats]
        let selected = settings.denomination
        let network: NetworkSecurityCase = session.gdkNetwork.mainnet ? .bitcoinSS : .testnetSS
        return DialogDenominationViewModel(denomination: selected,
                                           denominations: list,
                                           network: network)
    }
}
