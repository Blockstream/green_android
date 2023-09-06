import Foundation
import UIKit

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

    var hwDevice: BLEDevice? {
        didSet {
            sessions.forEach { $0.value.hw = hwDevice }
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
    
    func disconnect() async {
        for session in sessions.values {
            try? await session.disconnect()
        }
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
    
    func loginWithPin(pin: String, pinData: PinData, bip39passphrase: String?) async throws {
        print("--- loginWithPin")
        guard let mainSession = sessions[prominentNetwork.rawValue] else {
            fatalError()
        }
        try await mainSession.connect()
        let decryptData = DecryptWithPinParams(pin: pin, pinData: pinData)
        print("--- decryptWithPin")
        var credentials = try await mainSession.decryptWithPin(decryptData)
        // for bip39passphrase login, singlesig is the prominent network
        if !bip39passphrase.isNilOrEmpty {
            self.prominentNetwork = self.testnet ? .testnetSS : .bitcoinSS
            credentials = Credentials(mnemonic: credentials.mnemonic, bip39Passphrase: bip39passphrase)
        }
        print("--- login")
        try await self.login(credentials: credentials)
        AccountsRepository.shared.current = self.account
    }
    
    func create(_ credentials: Credentials) async throws {
        let btcNetwork: NetworkSecurityCase = testnet ? .testnetSS : .bitcoinSS
        let btcSession = self.sessions[btcNetwork.rawValue]!
        try await btcSession.connect()
        try await btcSession.register(credentials: credentials)
        let loginData = try await btcSession.loginUser(credentials, restore: false)
        account.xpubHashId = loginData.xpubHashId
        AccountsRepository.shared.current = self.account
        try await btcSession.updateSubaccount(subaccount: 0, hidden: true)
        _ = try await self.subaccounts()
        try? await self.loadRegistry()
    }
    
    func loginWatchonly(credentials: Credentials) async throws {
        guard let session = prominentSession else { fatalError() }
        let loginData = try await session.loginUser(credentials: credentials, restore: false)
        self.account.xpubHashId = loginData.xpubHashId
        AccountsRepository.shared.current = self.account
        _ = try await self.subaccounts()
        try? await self.loadRegistry()
}

    func login(credentials: Credentials? = nil, device: HWDevice? = nil, masterXpub: String? = nil, fullRestore: Bool = false) async throws {
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
        let fullRestore = fullRestore || account.xpubHashId == nil || !existDatadir(prominentSession)
        let ifLogin: ((_ session: SessionManager) -> Bool) = {
            if $0.gdkNetwork.lightning && !AppSettings.shared.experimental && fullRestore {
                return false
            }
            if $0.gdkNetwork.lightning && device != nil {
                return false
            }
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
        let loginTask: ((_ session: SessionManager) async throws -> ()) = { session in
            let walletHashId = walletId(session)!.walletHashId
            let removeDatadir = !existDatadir(session) && session.gdkNetwork.network != self.prominentNetwork.network
            let restore = fullRestore || !existDatadir(session)
            do {
                let res = try await session.loginUser(credentials: credentials, hw: device, restore: restore)
                self.account.xpubHashId = res.xpubHashId
                if session.gdkNetwork.network == self.prominentNetwork.network {
                    self.account.walletHashId = res.walletHashId
                }
                if session.logged && (fullRestore || !existDatadir(session)) {
                    let isFunded = try await session.discovery()
                    if !isFunded && removeDatadir {
                        if let lightningSession = session as? LightningSessionManager {
                            if !(lightningSession.isRestoredNode ?? false) {
                                try? await lightningSession.disconnect()
                                lightningSession.removeDatadir(walletHashId: walletHashId)
                                LightningRepository.shared.remove(for: walletHashId)
                            }
                        } else {
                            try? await session.disconnect()
                            session.removeDatadir(walletHashId: walletHashId)
                        }
                    }
                }
            } catch {
                try? await session.disconnect()
                switch error {
                case TwoFactorCallError.failure(let txt):
                    if txt.contains("HWW must enable host unblinding for singlesig wallets") {
                        self.failureSessions[session.gdkNetwork.network] = LoginError.hostUnblindingDisabled(txt)
                    } else if txt != "id_login_failed" {
                        self.failureSessions[session.gdkNetwork.network] = error
                    }
                default:
                    self.failureSessions[session.gdkNetwork.network] = error
                }
            }
        }
        failureSessions = [:]
        let sessions = self.sessions.values
            .filter { !$0.logged }
            .filter { ifLogin($0) }
        
        print("--- login start sessions \(sessions.count)")
        await withTaskGroup(of: Void.self) { group -> () in
            for session in sessions {
                group.addTask { try? await loginTask(session) }
            }
            for await _ in group {
            }
        }
        print("--- login end")
        if self.activeSessions.count == 0 {
            throw LoginError.failed()
        }
        _ = try await self.subaccounts()
        print("--- subaccounts end")
        try? await self.loadRegistry()
        //AccountsRepository.shared.current = self.account
    }

    func loadSystemMessages() async throws -> [SystemMessage] {
        return try await withThrowingTaskGroup(of: SystemMessage.self, returning: [SystemMessage].self) { [weak self] group in
            for session in (self?.activeSessions ?? [String: SessionManager]()).values {
                group.addTask {
                    let text = try? await session.loadSystemMessage()
                    return SystemMessage(text: text ?? "", network: session.gdkNetwork.network)
                }
            }
            return try await group.reduce(into: [SystemMessage]()) { partial, res in
                partial += [res]
            }
        }
    }

    func loadRegistry() async throws {
        let liquidNetworks: [NetworkSecurityCase] = testnet ? [.testnetLiquidSS, .testnetLiquidMS ] : [.liquidSS, .liquidMS ]
        let liquidSessions = sessions.filter { liquidNetworks.map { $0.rawValue }.contains($0.key) }
        var session = liquidSessions.filter({ $0.value.logged }).first?.value
        session = session ?? liquidSessions.filter({ $0.value.connected }).first?.value
        session = session ?? SessionManager(liquidNetworks.first!.gdkNetwork)
        if let session = session {
            try await registry.cache(session: session)
            Task { try await registry.refresh(session: session) }
        }
    }

    func subaccounts(_ refresh: Bool = false) async throws -> [WalletItem] {
        self.subaccounts = try await withThrowingTaskGroup(of: [WalletItem].self, returning: [WalletItem].self) { [weak self] group in
            for session in (self?.activeSessions ?? [String: SessionManager]()).values {
                group.addTask { try await session.subaccounts(refresh) }
            }
            return try await group.reduce(into: [WalletItem]()) { partial, result in
                partial += result
            }.sorted()
        }
        return self.subaccounts
    }

    func subaccount(account: WalletItem) async throws -> WalletItem? {
        let res = try? await account.session?.subaccount(account.pointer)
        if let res = res, let row = self.subaccounts.firstIndex(where: {$0.pointer == account.pointer && $0.gdkNetwork == account.gdkNetwork}) {
            self.subaccounts[row] = res
        }
        return res
    }

    func balances(subaccounts: [WalletItem]) async throws -> [String: Int64] {
        let balances = await withTaskGroup(of: [String: Int64].self, returning: [[String: Int64]].self) { group in
            for account in subaccounts.enumerated() {
                group.addTask {
                    let acc = account.element
                    let satoshi = try? await acc.session?.getBalance(subaccount: acc.pointer, numConfs: 0)
                    if let index = self.subaccounts.firstIndex(where: { $0.hashValue == acc.hashValue }), let satoshi = satoshi {
                        self.subaccounts[index].satoshi = satoshi
                        self.subaccounts[index].hasTxs = satoshi.count > 1 ? true : account.element.hasTxs
                        self.subaccounts[index].hasTxs = (satoshi.first?.value ?? 0) > 0 ? true : account.element.hasTxs
                    }
                    return satoshi ?? [:]
                }
            }
            return await group.reduce(into: [[String: Int64]]()) { partial, result in
                partial += [result]
            }
        }
        return balances
            .flatMap { $0 }
            .reduce([String:Int64]()) { (dict, tuple) in
                var nextDict = dict
                let prevValue = dict[tuple.key] ?? 0
                nextDict.updateValue(prevValue + tuple.value, forKey: tuple.key)
                return nextDict
            }
    }

    func transactions(subaccounts: [WalletItem], first: Int = 0) async throws -> [Transaction] {
        return try await withThrowingTaskGroup(of: [Transaction].self, returning: [Transaction].self) { group in
            for subaccount in subaccounts {
                group.addTask {
                    let txs = try await subaccount.session?.transactions(subaccount: subaccount.pointer, first: UInt32(first))
                    let page = txs?.list.map { Transaction($0.details, subaccount: subaccount.hashValue) }
                    return page ?? []
                }
            }
            return try await group.reduce(into: [Transaction]()) { partial, res in
                partial += res
            }.sorted()
        }
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
