import Foundation
import gdk

class DialogExchangeViewModel {

    var wm: WalletManager { WalletManager.current! }
    var session: SessionManager? { wm.prominentSession }
    var settings: Settings? { session?.settings }

    var exchangeList: [CurrencyItem] = []
    var currentExchange: CurrencyItem?
    var onReady: (() -> Void)?

    init(onReady: (() -> Void)?) {
        self.onReady = onReady
        guard let session = session, let settings = session.settings else { return }
        self.currentExchange = CurrencyItem(exchange: settings.pricing["exchange"] ?? "",
                                            currency: settings.pricing["currency"] ?? "")
        getExchanges()
    }

    func getExchanges() {
        Task {
            do {
                let perExchange = try await session?.getAvailableCurrencies()
                await MainActor.run {
                    self.exchangeList.removeAll()
                    var list: [CurrencyItem] = []
                    for (exchange, array) in perExchange ?? [:]{
                        for currency in array {
                            list.append(CurrencyItem(exchange: exchange, currency: currency))
                        }
                    }
                    self.exchangeList = list.sorted(by: { $0.currency < $1.currency })
                    self.onReady?()
                }
            } catch { print (error) }
        }
    }

}
