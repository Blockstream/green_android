import Foundation
import PromiseKit

class NotificationManager {

    var blockHeight: UInt32 = 0

    public func newNotification(notification: [String: Any]?) {
        guard let notificationEvent = notification?["event"] as? String,
                let event = EventType(rawValue: notificationEvent),
                let data = notification?[event.rawValue] as? [String: Any] else {
            return
        }
        #if DEBUG
        print("notification \(event): \(data)")
        #endif
        switch event {
        case .Block:
            guard let height = data["block_height"] as? UInt32 else { break }
            blockHeight = height
            post(event: .Block, data: data)
        case .Transaction:
            post(event: .Transaction, data: data)
            do {
                let json = try JSONSerialization.data(withJSONObject: data, options: [])
                let txEvent = try JSONDecoder().decode(TransactionEvent.self, from: json)
                if txEvent.type == "incoming" {
                    txEvent.subAccounts.forEach { pointer in
                        post(event: .AddressChanged, data: ["pointer": UInt32(pointer)])
                    }
                    DispatchQueue.main.async {
                        DropAlert().success(message: NSLocalizedString("id_new_transaction", comment: ""))
                    }
                }
            } catch { break }
        case .TwoFactorReset:
            SessionManager.shared.loadTwoFactorConfig().done { _ in
                self.post(event: .TwoFactorReset, data: data)
            }
        case .Settings:
            SessionManager.shared.settings = Settings.from(data)
            post(event: .Settings, data: data)
        case .Session:
            post(event: EventType.Network, data: data)
        case .Network:
            guard let json = try? JSONSerialization.data(withJSONObject: data, options: []),
                  let connection = try? JSONDecoder().decode(Connection.self, from: json) else {
                return
            }
            if !connection.connected || !(connection.loginRequired ?? false) {
                post(event: EventType.Network, data: data)
                return
            }
            guard SessionManager.shared.connected && SessionManager.shared.logged else {
                return
            }
            // Restore connection through hidden login
            SessionManager.shared.login(details: [:], hwDevice: nil).done { _ in
                self.post(event: EventType.Network, data: data)
            }.catch { err in
                print("Error on reconnected with hw: \(err.localizedDescription)")
                let appDelegate = UIApplication.shared.delegate as? AppDelegate
                appDelegate?.logout(with: false)
            }
        case .Tor:
            post(event: .Tor, data: data)
        case .Ticker:
            post(event: .Ticker, data: data)
        default:
            break
        }
    }

    func post(event: EventType, data: [String: Any]) {
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: event.rawValue),
                                        object: nil, userInfo: data)
    }

}
