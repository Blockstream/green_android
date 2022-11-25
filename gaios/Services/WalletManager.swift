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

    // Cached subaccounts list
    var registry: AssetsManager

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

    // Get active session of the active subaccount
    var prominentSession: SessionManager? {
        return sessions[prominentNetwork.rawValue]
    }

    // Serial reconnect queue for network events
    static let reconnectionQueue = DispatchQueue(label: "reconnection_queue")

    init(prominentNetwork: NetworkSecurityCase?) {
        let mainnet = prominentNetwork?.gdkNetwork?.mainnet ?? true
        self.prominentNetwork = prominentNetwork ?? .bitcoinSS
        self.registry = AssetsManager(testnet: !mainnet)
        if mainnet {
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

    var testnet: Bool {
        return !(prominentNetwork.gdkNetwork?.mainnet ?? true)
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
            .then { mainSession.connect() }
            .then { _ in mainSession.decryptWithPin(pin: pin, pinData: pinData) }
            .then { self.login($0) }
    }

    func loginWatchOnly(_ username: String, _ password: String) -> Promise<Void> {
        guard let mainSession = sessions[prominentNetwork.rawValue] else {
            fatalError()
        }
        return Guarantee()
            .then { mainSession.loginWatchOnly(username, password).asVoid() }
            .compactMap { Credentials(username: username, password: password) }
            .then { self.login($0) }
    }

    func login(_ credentials: Credentials) -> Promise<Void> {
        return when(guarantees: self.sessions.values
                .filter { !$0.logged }
                .map { session in
                    if session.gdkNetwork.electrum && !session.existDatadir(credentials: credentials) {
                        return Guarantee().asVoid()
                    }
                    return session.loginWithCredentials(credentials)
                    .asVoid()
                    .recover { _ in return Guarantee().asVoid() }
                })
            .then { self.subaccounts() }.asVoid()
            .compactMap { self.loadRegistry() }
    }

    func create(_ credentials: Credentials) -> Promise<Void> {
        let btcNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let btcSession = self.sessions[btcNetwork.rawValue]!
        return Promise()
            .then { btcSession.connect() }
            .then { btcSession.registerSW(credentials) }
            .then { btcSession.loginWithCredentials(credentials) }
            .then { _ in btcSession.updateSubaccount(subaccount: 0, hidden: true) }
            .then { _ in btcSession.subaccounts() }
            .asVoid()
    }

    func restore(_ credentials: Credentials) -> Guarantee<Void> {
        let btcNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let btcSession = self.sessions[btcNetwork.rawValue]!
        let btcRestore = Guarantee()
            .then { btcSession.loginWithCredentials(credentials) }
            .then { _ in btcSession.subaccounts(true).recover { _ in Promise(error: LoginError.connectionFailed()) }}
            .then { $0.first?.bip44Discovered ?? false ? Promise().asVoid() :
                btcSession.updateSubaccount(subaccount: $0.first?.pointer ?? 0, hidden: true) }
            .then { _ in btcSession.subaccounts() }

        let liquidNetwork: NetworkSecurityCase = testnet ? .testnetLiquidSS : .liquidSS
        let liquidSession = self.sessions[liquidNetwork.rawValue]!
        let liquidRestore = Guarantee()
            .then { liquidSession.loginWithCredentials(credentials) }
            .then { _ in liquidSession.subaccounts(true).recover { _ in Promise(error: LoginError.connectionFailed()) }}
            .then { (accounts: [WalletItem]) -> Promise<Void> in
                if accounts.first?.bip44Discovered ?? false {
                    return liquidSession.updateSubaccount(subaccount: accounts.first?.pointer ?? 0, hidden: true)
                }
                if accounts.filter({ $0.bip44Discovered ?? false }).isEmpty {
                    liquidSession.removeDatadir(credentials: credentials)
                }
                return Promise().asVoid()
            }.then { _ in btcSession.subaccounts() }
        return when(resolved: [btcRestore, liquidRestore]).asVoid()
    }

    func loginWithHW(_ device: HWDevice) -> Promise<Void> {
        var iterator = self.sessions.values
            .filter { !$0.logged }
            .filter { $0.gdkNetwork.network != "electrum-liquid" }
            .makeIterator()
        let generator = AnyIterator<Promise<Void>> {
            guard let session = iterator.next() else {
                return nil
            }
            return session.loginWithHW(device).asVoid()
                .recover { _ in return Guarantee().asVoid() }
        }
        return when(fulfilled: generator, concurrently: 1)
            .then { _ in self.subaccounts() }.asVoid()
            .compactMap { self.loadRegistry() }
    }

    func loadRegistry() {
        self.registry.loadAsync()
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
            self.subaccounts = Array(txt.joined()).sorted()
            return self.subaccounts
        }
    }

    func balances(subaccounts: [WalletItem]) -> Promise<[String: Int64]> {
        let promises = subaccounts
            .map { sub in
                sessions[sub.network ?? ""]!
                    .getBalance(subaccount: sub.pointer, numConfs: 0)
                    .compactMap { sub.satoshi = $0 }
                    .asVoid()
            }
        return when(fulfilled: promises)
            .compactMap { _ in
                var balance = [String: Int64]()
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

    func transactions(subaccounts: [WalletItem], first: Int = 0) -> Promise<[Transaction]> {
        var txs = [Transaction]()
        var iterator = subaccounts.makeIterator()
        let generator = AnyIterator<Promise<Void>> {
            guard let sub = iterator.next(),
                  let network = sub.network,
                  let session = self.sessions[network],
                  session.logged else {
                return nil
            }
            return session.transactions(subaccount: sub.pointer, first: UInt32(first))
                .compactMap { $0.list.map { Transaction($0.details, subaccount: sub.hashValue) } }
                .compactMap {
                    txs += $0 }
                .asVoid()
        }
        return when(fulfilled: generator, concurrently: 1)
            .compactMap { _ in txs }
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
        let account = AccountsManager.shared.current
        return get(for: account?.id ?? "")
    }

    static func add(for account: Account, wm: WalletManager? = nil) {
        if let wm = wm {
            wallets[account.id] = wm
            return
        }
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
