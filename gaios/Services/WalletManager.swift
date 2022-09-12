import Foundation
import UIKit
import PromiseKit

class WalletManager {

    // Store all the Wallet available for each account id
    static var wallets = [String: WalletManager]()

    // Hashmap of available networks with open session
    var sessions = [String: SessionManager]()

    // Prominent network used for login with stored credentials
    var prominentNetwork = NetworkSecurityCase.bitcoinSS

    // Cached subaccounts list
    var subaccounts = [WalletItem]()

    // Store active subaccount
    private var activeWalletHash: Int?
    var currentSubaccount: WalletItem? {
        get {
            if activeWalletHash == nil {
                return subaccounts.first { $0.hidden == false }
            }
            return subaccounts.first { $0.hashValue == activeWalletHash}
        }
        set {
            if let newValue = newValue {
                activeWalletHash = newValue.hashValue
                if let index = subaccounts.firstIndex(where: { $0.pointer == newValue.pointer && $0.network == newValue.network}) {
                    subaccounts[index] = newValue
                }
            }
        }
    }

    // Get active session of the active subaccount
    var currentSession: SessionManager? {
        let network = currentSubaccount?.network
        return sessions[network ?? ""]
    }

    // Serial reconnect queue for network events
    static let reconnectionQueue = DispatchQueue(label: "reconnection_queue")

    init(prominentNetwork: NetworkSecurityCase?) {
        self.prominentNetwork = prominentNetwork ?? .bitcoinSS
        if prominentNetwork?.gdkNetwork?.mainnet ?? true {
            addSession(for: .bitcoinSS)
            addSession(for: .liquidSS)
            addSession(for: .bitcoinMS)
            addSession(for: .liquidMS)
        } else {
            addSession(for: .testnetSS)
            addSession(for: .testnetLiquidSS)
            addSession(for: .testnetMS)
            addSession(for: .testnetLiquidMS)
        }
    }

    func addSession(for network: NetworkSecurityCase) {
        let networkName = network.network
        let gdknetwork = getGdkNetwork(networkName)
        sessions[networkName] = SessionManager(gdknetwork)
    }

    var activeSessions: [String: SessionManager] {
        self.sessions.filter { $0.1.logged }
    }

    var logged: Bool {
        activeSessions.count > 0
    }

    func loginWithPin(pin: String, pinData: PinData, bip39passphrase: String?) -> Promise<Void> {
        guard let mainSession = sessions[prominentNetwork.rawValue] else {
            fatalError()
        }
        return Guarantee()
            //.then { mainSession.connect() }
            //.then { _ in mainSession.decryptWithPin(pin: pin, pinData: pinData) }
            .then { _ in mainSession.loginWithPin(pin, pinData: pinData) }
            .then { _ in mainSession.getCredentials(password: "") }
            .then { self.login($0) }
            .compactMap { self.loadRegistry() }
    }

    func loginWatchOnly(_ username: String, _ password: String) -> Promise<Void> {
        guard let mainSession = sessions[prominentNetwork.rawValue] else {
            fatalError()
        }
        return Guarantee()
            .then { mainSession.loginWatchOnly(username, password).asVoid() }
            .compactMap { Credentials(username: username, password: password) }
            .then { self.login($0) }
            .compactMap { self.loadRegistry() }
    }

    func login(_ credentials: Credentials) -> Guarantee<Void> {
        return when(guarantees: self.sessions.values
                .filter { !$0.logged }
                .map { session in
                    session.loginWithCredentials(credentials)
                    .asVoid()
                    .recover { _ in return Guarantee().asVoid() }
                }
        )
    }

    func loadRegistry() {
        self.sessions.values
            .filter { $0.logged }
            .forEach {
                $0.registry?.cache(session: $0)
                $0.registry?.loadAsync(session: $0)
            }
    }

    func subaccounts(_ refresh: Bool = false) -> Promise<[WalletItem]> {
        let promises: [Promise<[WalletItem]>] = self.activeSessions.values
            .compactMap { session in
                session
                    .subaccounts(refresh)
                    .get { $0.forEach { $0.network = session.gdkNetwork.network }}
            }
        return when(resolved: promises).compactMap { (subaccounts: [Result<[WalletItem]>]) -> [WalletItem] in
            let txt: [[WalletItem]] = subaccounts.compactMap { res in
                switch res {
                case .fulfilled(let sub):
                    return sub
                case .rejected(_):
                    return nil
                }
            }
            self.subaccounts = Array(txt.joined())
            return self.subaccounts
        }
    }

    func balances(subaccounts: [WalletItem]) -> Promise<[String: UInt64]> {
        let promises = subaccounts
            .map { sub in
                sessions[sub.network ?? ""]!
                    .getBalance(subaccount: sub.pointer, numConfs: 0)
                    .compactMap { sub.satoshi = $0 }
                    .asVoid()
            }
        return when(fulfilled: promises)
            .compactMap { _ in
                var balance = [String: UInt64]()
                subaccounts.forEach { subaccount in
                    let satoshi = subaccount.satoshi ?? [:]
                    satoshi.forEach {
                        if let amount = balance[$0.0] {
                            balance[$0.0] = amount + $0.1
                        } else {
                            balance[$0.0] = $0.1
                        }
                    }
                }
                return balance
            }
    }

    func subaccountsFilteredByAsset(subaccounts: [WalletItem], asset: String) -> [WalletItem] {
        return subaccounts.filter { $0.satoshi?.keys.contains(asset) ?? false }
    }

    func pause() {
        activeSessions.forEach { (_, session) in
            if session.connected {
                WalletManager.reconnectionQueue.async {
                    try? session.session?.reconnectHint(hint: ["tor_hint": "disconnect", "hint": "disconnect"])
                }
            }
        }
    }

    func resume() {
        activeSessions.forEach { (_, session) in
            if session.connected {
                WalletManager.reconnectionQueue.async {
                    try? session.session?.reconnectHint(hint: ["tor_hint": "connect", "hint": "connect"])
                }
            }
        }
    }
}

extension WalletManager {

    // Return current WalletManager used for the active user session
    static var current: WalletManager? {
        let account = AccountDao.shared.current
        return get(for: account?.id ?? "")
    }

    static func add(for account: Account) {
        let network = NetworkSecurityCase(rawValue: account.networkName)
        let wm = WalletManager(prominentNetwork: network)
        wallets[account.id] = wm
    }

    static func get(for accountId: String) -> WalletManager? {
        return wallets[accountId]
    }

    static func get(for account: Account) -> WalletManager? {
        get(for: account.id)
    }

    static func getOrAdd(for account: Account) -> WalletManager {
        if !wallets.keys.contains(account.id) {
            add(for: account)
        }
        return get(for: account)!
    }

    static func delete(for accountId: String) {
        wallets.removeValue(forKey: accountId)
    }

    static func delete(for account: Account?) {
        if let account = account {
            delete(for: account.id)
        }
    }
}
