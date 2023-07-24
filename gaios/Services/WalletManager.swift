import Foundation
import UIKit
import PromiseKit
import gdk
import hw
import lightning

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

    // Get active session of the active subaccount
    var prominentSession: SessionManager? {
        return sessions[prominentNetwork.rawValue]
    }

    // For Countly
    var activeNetworks: [NetworkSecurityCase] {
        return activeSessions.keys.compactMap { NetworkSecurityCase(rawValue: $0) }
    }

    init(account: Account, prominentNetwork: NetworkSecurityCase?) {
        let mainnet = prominentNetwork?.gdkNetwork.mainnet ?? true
        self.prominentNetwork = prominentNetwork ?? .bitcoinSS
        self.registry = AssetsManager(testnet: !mainnet)
        self.account = account
        if mainnet {
            addSession(for: .bitcoinSS)
            addSession(for: .liquidSS)
            addSession(for: .bitcoinMS)
            addSession(for: .liquidMS)
            addLightningSession(for: .lightning)
        } else {
            addSession(for: .testnetSS)
            addSession(for: .testnetLiquidSS)
            addSession(for: .testnetMS)
            addSession(for: .testnetLiquidMS)
            //breez not enabled on testnet
        }
    }

    func disconnect() {
        activeSessions.values.forEach { $0.disconnect() }
    }

    func addSession(for network: NetworkSecurityCase) {
        let networkName = network.network
        sessions[networkName] = SessionManager(network.gdkNetwork)
    }

    func addLightningSession(for network: NetworkSecurityCase) {
        let session = LightningSessionManager(network.gdkNetwork)
        session.accountId = account.id
        sessions[network.rawValue] = session
    }

    var lightningSession: LightningSessionManager? {
        let network: NetworkSecurityCase = testnet ? .testnetLightning : .lightning
        return sessions[network.rawValue] as? LightningSessionManager
    }

    var lightningSubaccount: WalletItem? {
        return subaccounts.filter {$0.gdkNetwork.lightning }.first
    }

    var testnet: Bool {
        return !prominentNetwork.gdkNetwork.mainnet
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
            .map { credentials in
                // for bip39passphrase login, singlesig is the prominent network
                if !bip39passphrase.isNilOrEmpty {
                    self.prominentNetwork = self.testnet ? .testnetSS : .bitcoinSS
                    return Credentials(mnemonic: credentials.mnemonic, bip39Passphrase: bip39passphrase)
                }
                return credentials
            }
            .then { self.login(credentials: $0) }
            .map { AccountsRepository.shared.current = self.account }
    }

    func create(_ credentials: Credentials) -> Promise<Void> {
        let btcNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let btcSession = self.sessions[btcNetwork.rawValue]!
        return Promise()
            .then { btcSession.connect() }
            .then { btcSession.register(credentials: credentials) }
            .compactMap { btcSession.walletIdentifier(credentials: credentials) }
            .then { _ in btcSession.loginUser(credentials, restore: false) }
            .map { self.account.xpubHashId = $0.xpubHashId }
            .then { _ in btcSession.updateSubaccount(subaccount: 0, hidden: true) }
            .map { AccountsRepository.shared.current = self.account }
            .then { self.subaccounts() }
            .compactMap { _ in self.loadRegistry() }
            .asVoid()
    }

    func loginWatchonly(credentials: Credentials) -> Promise<Void> {
        guard let session = prominentSession else { fatalError() }
        return session.loginUser(credentials: credentials, restore: false)
            .map { self.account.xpubHashId = $0.xpubHashId }
            .then { self.subaccounts() }.asVoid()
            .compactMap { self.loadRegistry() }
            .map { AccountsRepository.shared.current = self.account }
    }

    func login(credentials: Credentials? = nil, device: HWDevice? = nil, masterXpub: String? = nil) -> Promise<Void> {
        let walletId: ((_ session: SessionManager) -> WalletIdentifier?) = { session in
            if let credentials = credentials {
                return session.walletIdentifier(credentials: credentials)
            } else if device != nil, let masterXpub = masterXpub {
                return session.walletIdentifier(masterXpub: masterXpub)
            }
            return nil
        }
        let existDatadir: ((_ session: SessionManager) -> Bool) = { session in
            session.existDatadir(walletHashId: walletId(session)!.walletHashId)
        }
        guard let prominentSession = sessions[prominentNetwork.rawValue] else { fatalError() }
        let fullRestore = account.xpubHashId == nil || !existDatadir(prominentSession)
        let doLogin: ((_ session: SessionManager) -> Bool) = {
            if $0.gdkNetwork.liquid && device?.supportsLiquid ?? 1 == 0 {
                // disable liquid if is unsupported on hw
                return false
            }
            if fullRestore && $0.enabled() {
                return true
            }
            if $0.gdkNetwork.network == prominentSession.gdkNetwork.network {
                return true
            }
            if existDatadir($0) {
                return true
            }
            return false
        }
        failureSessions = [:]
        let networks = self.sessions.values
            .filter { !$0.logged }
            .filter { doLogin($0) }
            .map { $0.gdkNetwork.network }
        let concurrently = device != nil ? 1 : networks.count
        let bgq = DispatchQueue.global(qos: .background)
        return Promise<String>.chain(networks, concurrently, on: bgq) { network -> Promise<Void> in
            return Promise { seal in
                guard let session = self.sessions[network] else { return seal.fulfill(()) }
                let walletHashId = walletId(session)!.walletHashId
                let removeDatadir = !existDatadir(session) && session.gdkNetwork.network != self.prominentNetwork.network
                let restore = fullRestore || !existDatadir(session)
                do {
                    let res = try session.loginUser(credentials: credentials, hw: device, restore: restore).wait()
                    self.account.xpubHashId = res.xpubHashId
                    if session.gdkNetwork.network == self.prominentNetwork.network {
                        self.account.walletHashId = res.walletHashId
                    }
                    if session.logged && (fullRestore || !existDatadir(session)) {
                        let isFunded = try session.discovery().wait()
                        if !isFunded && removeDatadir {
                            if let lightningSession = session as? LightningSessionManager {
                                if !(lightningSession.isRestoredNode ?? false) {
                                    lightningSession.disconnect()
                                    lightningSession.removeDatadir(walletHashId: walletHashId)
                                    LightningRepository.shared.remove(for: walletHashId)
                                }
                            } else {
                                session.disconnect()
                                session.removeDatadir(walletHashId: walletHashId)
                            }
                        }
                    }
                } catch {
                    self.failureSessions[session.gdkNetwork.network] = error
                }
                seal.fulfill(())
            }
        }
        .map { _ in if self.activeSessions.count == 0 { throw LoginError.failed() } }
        .then { self.subaccounts() }.asVoid()
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
            let session = SessionManager(liquidNetworks.first!.gdkNetwork)
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
