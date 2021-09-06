import Foundation
import PromiseKit

class NotificationManager {

    var twoFactorReset: TwoFactorReset?
    var events = [Event]()
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
                events.append(Event(value: data))
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
            do {
                let json = try JSONSerialization.data(withJSONObject: data, options: [])
                self.twoFactorReset = try JSONDecoder().decode(TwoFactorReset.self, from: json)
                events.removeAll(where: { $0.kindOf(TwoFactorReset.self)})
                if self.twoFactorReset!.isResetActive {
                    events.append(Event(value: data))
                }
            } catch { break }
            post(event: .TwoFactorReset, data: data)
        case .Settings:
            reloadSystemMessage()
            Settings.shared = Settings.from(data)
            if let acc = AccountsManager.shared.current, !acc.isWatchonly {
                reloadTwoFactor()
            }
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
            // Restore connection through hidden login
            reconnect().done { _ in
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

    func reloadTwoFactor() {
        events.removeAll(where: { $0.kindOf(Settings.self)})
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map(on: bgq) {
            try SessionManager.shared.getTwoFactorConfig()
        }.done { dataTwoFactorConfig in
            if dataTwoFactorConfig != nil {
                let twoFactorConfig = try JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig!, options: []))
                let data = try JSONSerialization.jsonObject(with: JSONEncoder().encode(Settings.shared), options: .allowFragments) as? [String: Any]
                if twoFactorConfig.enableMethods.count <= 1 {
                    self.events.append(Event(value: data!))
                }
            }
        }.catch { _ in
            print("Error on get settings")
        }
    }

    func reloadSystemMessage() {
        events.removeAll(where: { $0.kindOf(SystemMessage.self)})
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map(on: bgq) {
            try SessionManager.shared.getSystemMessage()
        }.done { text in
            if !text.isEmpty {
                self.events.append(Event(value: ["text": text]))
            }
        }.catch { _ in
            print("Error on get system message")
        }
    }

    func reconnect() -> Promise<[String: Any]> {
        let bgq = DispatchQueue.global(qos: .background)
        let session = SessionManager.shared
        let account = AccountsManager.shared.current
        let isHwLogin = account?.isJade ?? false || account?.isLedger ?? false
        return Guarantee().then(on: bgq) { _ in
            return try session.loginUser(details: [:], hw_device: [:]).resolve()
        }
    }
}
