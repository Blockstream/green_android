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
    var prominentSession: SessionManager? {
        return sessions[prominentNetwork.rawValue]
    }

    // For Countly
    var activeNetworks: [NetworkSecurityCase] {
        return activeSessions.keys.compactMap { NetworkSecurityCase(rawValue: $0) }
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

    var failureSessions = [String: Error]()

    var logged: Bool {
        activeSessions.count > 0
    }

    func loginWithPin(pin: String, pinData: PinData, bip39passphrase: String?) -> Promise<Void> {
        guard let mainSession = sessions[prominentNetwork.rawValue] else {
            fatalError()
        }
        return Guarantee()
            .then { mainSession.connect() }
            .compactMap { DecryptWithPinParams(pin: pin, pinData: pinData)}
            .then { mainSession.decryptWithPin($0) }
            .map { bip39passphrase.isNilOrEmpty ? $0 : Credentials(mnemonic: $0.mnemonic, bip39Passphrase: bip39passphrase) }
            .then { when(fulfilled: Guarantee.value($0), !bip39passphrase.isNilOrEmpty && !mainSession.existDatadir(credentials: $0) ? self.restore($0) : Guarantee().asVoid()) }
            .then { credentials, _ in self.login(credentials) }
    }

    func loginWatchOnly(_ credentials: Credentials) -> Promise<Void> {
        guard let mainSession = sessions[prominentNetwork.rawValue] else {
            fatalError()
        }
        return mainSession.loginUser(credentials).asVoid()
            .then { self.subaccounts() }.asVoid()
            .compactMap { self.loadRegistry() }
    }

    func loginWithHW(_ device: HWDevice, masterXpub: String) -> Promise<Void> {
        guard let mainSession = sessions[prominentNetwork.rawValue] else {
            fatalError()
        }
        return Guarantee()
            .compactMap { mainSession.existDatadir(masterXpub: masterXpub) }
            .then { !$0 ? self.restore(hw: device) : Guarantee().asVoid() }
            .then { self.loginHW(device) }
            .asVoid()
    }

    func login(_ credentials: Credentials) -> Promise<Void> {
        self.failureSessions = [:]
        return when(guarantees: self.sessions.values
                .filter { !$0.logged }
                .map { session in
                    if session.gdkNetwork.electrum && credentials.bip39Passphrase == nil && !session.existDatadir(credentials: credentials) {
                        return Guarantee().asVoid()
                    }
                    return session.loginUser(credentials)
                    .asVoid()
                    .recover { err in
                        switch err {
                        case TwoFactorCallError.failure(_):
                            break
                        default:
                            self.failureSessions[session.gdkNetwork.network] = err
                        }
                    }
                })
            .compactMap { if self.activeSessions.count == 0 { throw LoginError.failed() } }
            .then { self.subaccounts() }.asVoid()
            .compactMap { self.loadRegistry() }
    }

    func create(_ credentials: Credentials) -> Promise<Void> {
        let btcNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let btcSession = self.sessions[btcNetwork.rawValue]!
        return Promise()
            .then { btcSession.connect() }
            .then { btcSession.register(credentials: credentials) }
            .then { btcSession.loginUser(credentials) }
            .then { _ in btcSession.updateSubaccount(subaccount: 0, hidden: true) }
    }

    func restore(_ credentials: Credentials? = nil, hw: HWDevice? = nil) -> Promise<Void> {
        let btcNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let btcSession = self.sessions[btcNetwork.rawValue]!
        if btcSession.existDatadir(credentials: credentials) {
            return Promise() { seal in seal.reject(LoginError.walletsJustRestored()) }
        }
        let btcRestore = Guarantee()
            .then { btcSession.login(credentials: credentials, hw: hw) }
            .then { _ in btcSession.subaccounts(true).recover { _ in Promise(error: LoginError.connectionFailed()) }}
            .compactMap { $0.filter({ $0.pointer == 0 }).first }
            .then { credentials?.bip39Passphrase.isNilOrEmpty ?? true && !($0.bip44Discovered ?? false) ? btcSession.updateSubaccount(subaccount: 0, hidden: true) : Promise().asVoid() }
        guard let credentials = credentials else {
            // Liquid singlesig not yet available
            return btcRestore
        }
        let liquidNetwork: NetworkSecurityCase = testnet ? .testnetLiquidSS : .liquidSS
        guard let liquidSession = self.sessions[liquidNetwork.rawValue] else {
            return btcRestore
        }
        let liquidRestore = Guarantee()
            .then { liquidSession.login(credentials: credentials, hw: hw) }
            .then { _ in liquidSession.subaccounts(true).recover { _ in Promise(error: LoginError.connectionFailed()) }}
            .compactMap { $0.filter({ $0.pointer == 0 }).first }
            .then { !($0.bip44Discovered ?? false) ? liquidSession.updateSubaccount(subaccount: 0, hidden: true) : Promise().asVoid() }
            .then { _ in liquidSession.subaccounts() }
            .compactMap { subaccounts in
                if subaccounts.filter({ $0.bip44Discovered ?? false }).isEmpty {
                    liquidSession.removeDatadir(credentials: credentials)
                    liquidSession.session?.logged = false
                    return
                }
            }.asVoid()
        return when(fulfilled: [btcRestore, liquidRestore])
    }

    func loginHW(_ device: HWDevice) -> Promise<Void> {
        var iterator = self.sessions.values
            .filter { !$0.logged }
            .filter { $0.gdkNetwork.network != "electrum-liquid" }
            .makeIterator()
        let generator = AnyIterator<Promise<Void>> {
            guard let session = iterator.next() else {
                return nil
            }
            return session.loginUser(device).asVoid()
                .recover { _ in return Guarantee().asVoid() }
        }
        return when(fulfilled: generator, concurrently: 1)
            .then { _ in self.subaccounts() }.asVoid()
            .compactMap { self.loadRegistry() }
    }

    func loadSystemMessages() -> Promise<[SystemMessage]> {
        let promises: [Promise<SystemMessage>] = self.activeSessions.values
            .compactMap { session in
                let text = try? session.session?.getSystemMessage()
                return SystemMessage(text: text ?? "", network: session.gdkNetwork.network)
            }.compactMap { res in Promise() { seal in seal.fulfill(res)} }
        return when(fulfilled: promises)
    }

    func loadRegistry() {
        let liquidNetworks: [NetworkSecurityCase] = testnet ? [.testnetLiquidSS, .testnetLiquidMS ] : [.liquidSS, .liquidMS ]
        let liquidSessions = sessions.filter { liquidNetworks.map { $0.rawValue }.contains($0.key) }
        if let session = liquidSessions.filter({ $0.value.logged }).first?.value {
            return registry.loadAsync(session: session)
        } else if let session = liquidSessions.filter({ $0.value.connected }).first?.value {
            return registry.loadAsync(session: session)
        } else {
            return registry.loadAsync(session: nil)
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

    static func delete(for wm: WalletManager) {
        if let index = wallets.firstIndex(where: { $0.value === wm }) {
            wallets.remove(at: index)
        }
    }

    static func change(wm: WalletManager, for account: Account) {
        delete(for: wm)
        add(for: account, wm: wm)
    }
}
