import Foundation
import UIKit
import PromiseKit
import gdk
import hw

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

    func disconnect() {
        activeSessions.values.forEach { $0.disconnect() }
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
            .then { self.login(credentials: $0) }
            .map { AccountsRepository.shared.current = self.account }
    }

    func create(_ credentials: Credentials) -> Promise<Void> {
        let btcNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let btcSession = self.sessions[btcNetwork.rawValue]!
        return Promise()
            .then { btcSession.connect() }
            .then { btcSession.register(credentials: credentials) }
            .compactMap { btcSession.walletIdentifier(btcNetwork.network, credentials: credentials) }
            .then { _ in btcSession.loginUser(credentials) }
            .map { self.account.xpubHashId = $0.xpubHashId }
            .then { _ in btcSession.updateSubaccount(subaccount: 0, hidden: true) }
            .map { AccountsRepository.shared.current = self.account }
    }

    func login(credentials: Credentials? = nil, device: HWDevice? = nil, masterXpub: String? = nil) -> Promise<Void> {
        let walletId: ((_ session: SessionManager) -> WalletIdentifier?) = { session in
            if let credentials = credentials {
                return session.walletIdentifier(session.gdkNetwork.network, credentials: credentials)
            } else if device != nil, let masterXpub = masterXpub {
                return session.walletIdentifier(session.gdkNetwork.network, masterXpub: masterXpub)
            }
            return nil
        }
        let existDatadir:((_ session: SessionManager) -> Bool) = { session in
            !session.gdkNetwork.electrum || session.existDatadir(walletHashId: walletId(session)!.walletHashId)
        }
        
        guard let session = sessions[prominentNetwork.rawValue] else { fatalError() }
        let restore = account.xpubHashId == nil || !existDatadir(session)
        self.failureSessions = [:]
        let networks = self.sessions.values
            .filter { !$0.logged }
            .filter { restore || $0.gdkNetwork.network == prominentNetwork.gdkNetwork!.network || existDatadir(session) }
            .filter { !$0.gdkNetwork.liquid || device?.supportsLiquid ?? 1 == 1 }
            .map { $0.gdkNetwork.network }
        let concurrently = device != nil ? 1 : 2
        return Promise<String>.chain(networks, concurrently) { network -> Promise<Void> in
            guard let session = self.sessions[network] else { return Promise(error: LoginError.failed()) }
            let walletHashId = walletId(session)!.walletHashId
            let existDatadir = existDatadir(session)
            let removeDatadir = !existDatadir && session.gdkNetwork.network != self.prominentNetwork.network
            return session.loginUser(credentials: credentials, hw: device)
                .map { self.account.xpubHashId = $0.xpubHashId }
                .recover { self.failureSessions[session.gdkNetwork.network] = $0 }
                .then { session.logged && (restore || !existDatadir) ? session.discovery(credentials: credentials, hw: device, removeDatadir: removeDatadir, walletHashId: walletHashId) : Promise().asVoid() }
                .asVoid()
        }
        .map { _ in if
            self.activeSessions.count == 0 { throw LoginError.failed() } }
        .then { self.subaccounts(true) }.asVoid()
        .compactMap { self.loadRegistry() }
        .map { AccountsRepository.shared.current = self.account }
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
