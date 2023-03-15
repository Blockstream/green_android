import Foundation
import UIKit
import PromiseKit

class WalletManager {

    // Return current WalletManager used for the active user session
    static var current: WalletManager? {
        let account = AccountsRepository.shared.current
        return WalletsRepository.shared.get(for: account?.id ?? "")
    }

    // Hashmap of available networks with open session
    var sessions = [String: SessionManager]()

    // Prominent network used for login with stored credentials
    var prominentNetwork = NetworkSecurityCase.bitcoinSS

    // Cached subaccounts list
    var subaccounts = [WalletItem]()

    // Cached subaccounts list
    var registry: AssetsManager

    var account: Account {
        didSet {
            if AccountsRepository.shared.get(for: account.id) != nil {
                AccountsRepository.shared.upsert(account)
            }
        }
    }

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

    init(account: Account, prominentNetwork: NetworkSecurityCase?) {
        let mainnet = prominentNetwork?.gdkNetwork?.mainnet ?? true
        self.prominentNetwork = prominentNetwork ?? .bitcoinSS
        self.registry = AssetsManager(testnet: !mainnet)
        self.account = account
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

    var hasMultisig: Bool {
        let multisigNetworks: [NetworkSecurityCase] =  [.bitcoinMS, .testnetMS, .liquidMS, .testnetLiquidMS]
        return self.activeNetworks.filter { multisigNetworks.contains($0) }.count > 0
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
            .map { (self.account.xpubHashId == nil || ( mainSession.gdkNetwork.electrum && !mainSession.existDatadir(credentials: $0)), $0) }
            .then { (discovery, cred) in discovery ? self.restore(cred).map { cred } : Promise.value(cred) }
            .then { self.login($0) }
            .map { AccountsRepository.shared.current = self.account }
    }

    func loginWithHW(_ device: HWDevice, masterXpub: String) -> Promise<Void> {
        guard let mainSession = sessions[prominentNetwork.rawValue] else {
            fatalError()
        }
        return Guarantee()
            .compactMap { mainSession.existDatadir(masterXpub: masterXpub) }
            .then { !$0 ? self.restore(hw: device) : Guarantee().asVoid() }
            .then { self.loginHW(device) }
            .map { AccountsRepository.shared.current = self.account }
            .asVoid()
    }

    func login(_ credentials: Credentials) -> Promise<Void> {
        self.failureSessions = [:]
        return when(guarantees: self.sessions.values
            .filter { !$0.logged && ($0 === prominentSession || !($0.gdkNetwork.electrum && !$0.existDatadir(credentials: credentials))) }
            .map { session in
                session.loginUser(credentials)
                    .map { self.account.xpubHashId = $0.xpubHashId }
                    .recover { err in
                        switch err {
                        case TwoFactorCallError.failure(_):
                            break
                        default:
                            self.failureSessions[session.gdkNetwork.network] = err
                        }
                    }.asVoid()
            })
        .map { if self.activeSessions.count == 0 { throw LoginError.failed() } }
        .then { self.subaccounts() }.asVoid()
        .compactMap { self.loadRegistry() }
        .map { AccountsRepository.shared.current = self.account }
    }

    func create(_ credentials: Credentials) -> Promise<Void> {
        let btcNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let btcSession = self.sessions[btcNetwork.rawValue]!
        return Promise()
            .then { btcSession.connect() }
            .then { btcSession.register(credentials: credentials) }
            .then { btcSession.loginUser(credentials) }
            .map { self.account.xpubHashId = $0.xpubHashId }
            .then { _ in btcSession.updateSubaccount(subaccount: 0, hidden: true) }
            .map { AccountsRepository.shared.current = self.account }
    }

    func restore(_ credentials: Credentials? = nil, hw: HWDevice? = nil, forceJustRestored: Bool = false) -> Promise<Void> {
        let btcNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let btcSession = self.sessions[btcNetwork.rawValue]
        let btcPromise = btcSession?.restore(credentials: credentials, hw: hw, forceJustRestored: forceJustRestored)
            .map { self.account.xpubHashId = $0.xpubHashId }.asVoid()
        let liquidNetwork: NetworkSecurityCase = testnet ? .testnetLiquidSS : .liquidSS
        let liquidSession = self.sessions[liquidNetwork.rawValue]
        let liquidPromise = hw == nil ? liquidSession?.restore(credentials: credentials, hw: hw, forceJustRestored: forceJustRestored).asVoid() : nil
        return when(fulfilled: [btcPromise ?? Promise().asVoid(), liquidPromise ?? Promise().asVoid()])
            .map { AccountsRepository.shared.current = self.account }
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
            let liquidNetworks: [NetworkSecurityCase] = testnet ? [.testnetLiquidSS, .testnetLiquidMS ] : [.liquidSS, .liquidMS ]
            let session = SessionManager(getGdkNetwork(liquidNetworks.first!.rawValue))
            return registry.loadAsync(session: session)
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

    func subaccount(account: WalletItem) -> Promise<WalletItem> {
        return Guarantee()
            .compactMap { account.session }
            .then { $0.subaccount(account.pointer) }
            .get { if let row = self.subaccounts.firstIndex(where: {$0.pointer == account.pointer && $0.gdkNetwork == account.gdkNetwork}) { self.subaccounts[row] = $0 } }
    }

    func balances(subaccounts: [WalletItem]) -> Promise<[String: Int64]> {
        let promises = subaccounts
            .map { sub in
                sessions[sub.network ?? ""]!
                    .getBalance(subaccount: sub.pointer, numConfs: 0)
                    .compactMap { sub.satoshi = $0 }
                    .compactMap { sub.hasTxs = (sub.satoshi ?? [:]).count > 1 ? true : sub.hasTxs }
                    .compactMap { sub.hasTxs = (sub.satoshi?.first?.value ?? 0) > 0 ? true : sub.hasTxs }
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
                .get { txs += $0 }
                .compactMap { sub.hasTxs = !$0.isEmpty }
                .asVoid()
        }
        return when(fulfilled: generator, concurrently: 1)
            .compactMap { _ in txs }
    }

    func pause() {
        activeSessions.forEach { (_, session) in
            if session.connected {
                session.networkDisconnect()
            }
        }
    }

    func resume() {
        activeSessions.forEach { (_, session) in
            if session.connected {
               session.networkConnect()
            }
        }
    }
}
