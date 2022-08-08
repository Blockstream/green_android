import Foundation

class SessionsManager {

    static var shared = [String: SessionManager]()
    private static let reconnectionQueue = DispatchQueue(label: "reconnection_queue")

    static var current: SessionManager? {
        guard let account = AccountsManager.shared.current else {
            fatalError("no account selected or found")
        }
        return get(for: account)
    }

    static func get(for account: Account) -> SessionManager? {
        if shared.contains(where: { $0.0 == account.id }) {
            return shared[account.id]
        }
        return nil
    }

    static func new(for account: Account) -> SessionManager {
        if shared[account.id] != nil {
            remove(for: account)
        }
        let session = SessionManager(account.gdkNetwork!)
        shared[account.id] = session
        return session
    }

    static func remove(for account: Account) {
        shared.removeValue(forKey: account.id)
    }

    static func remove(for id: String) {
        shared.removeValue(forKey: id)
    }

    static func pause() {
        shared.forEach { (_, session) in
            if session.connected {
                reconnectionQueue.async {
                    try? session.session?.reconnectHint(hint: ["tor_hint": "disconnect", "hint": "disconnect"])
                }
            }
        }
        if useTor() {
            reconnectionQueue.async {
                TorSessionManager.shared.pause()
            }
        }
    }

    static func resume() {
        shared.forEach { (_, session) in
            if session.connected {
                reconnectionQueue.async {
                    try? session.session?.reconnectHint(hint: ["tor_hint": "connect", "hint": "connect"])
                }
            }
        }
        if useTor() {
            reconnectionQueue.async {
                TorSessionManager.shared.resume()
            }
        }
    }

    static func useTor() -> Bool {
        let networkSettings = getUserNetworkSettings()
        return networkSettings["tor"] as? Bool ?? false
    }
}
