import Foundation
import PromiseKit
import gdk
import greenaddress
import hw

public enum LoginError: Error, Equatable {
    case walletsJustRestored(_ localizedDescription: String? = nil)
    case walletNotFound(_ localizedDescription: String? = nil)
    case invalidMnemonic(_ localizedDescription: String? = nil)
    case connectionFailed(_ localizedDescription: String? = nil)
    case failed(_ localizedDescription: String? = nil)
    case walletMismatch(_ localizedDescription: String? = nil)
}

class SessionManager {

    var notificationManager: NotificationManager?
    var twoFactorConfig: TwoFactorConfig?
    var settings: Settings?
    var session: GDKSession?
    var gdkNetwork: GdkNetwork
    var registry: AssetsManager?

    // Serial reconnect queue for network events
    static let reconnectionQueue = DispatchQueue(label: "reconnection_queue")

    var isResetActive: Bool? {
        get { twoFactorConfig?.twofactorReset.isResetActive }
    }

    var connected: Bool {
        self.session?.connected ?? false
    }

    var logged: Bool {
        self.session?.logged ?? false
    }

    init(_ gdkNetwork: GdkNetwork) {
        self.gdkNetwork = gdkNetwork
        session = GDKSession()
        registry = AssetsManager(testnet: !gdkNetwork.mainnet)
    }

    deinit {
        session?.logged = false
        session?.connected = false
    }

    public func connect() -> Promise<Void> {
        if session?.connected ?? false {
            return Promise().asVoid()
        }
        return Guarantee()
            .compactMap(on: SessionManager.reconnectionQueue) { self.networkConnect() }
            .compactMap(on: SessionManager.reconnectionQueue) { try self.connect(network: self.gdkNetwork.network) }
            .compactMap { AnalyticsManager.shared.setupSession(session: self.session) } // Update analytics endpoint with session tor/proxy
    }

    public func disconnect() {
        session?.logged = false
        session?.connected = false
        SessionManager.reconnectionQueue.async {
            self.session = GDKSession()
        }
    }

    private func connect(network: String, params: [String: Any]? = nil) throws {
        do {
            if notificationManager == nil {
                self.notificationManager = NotificationManager(session: self)
            }
            if let notificationManager = notificationManager {
                session?.setNotificationHandler(notificationCompletionHandler: notificationManager.newNotification)
            }
            try session?.connect(netParams: networkParams(network).toDict() ?? [:])
        } catch {
            switch error {
            case GaError.GenericError(let txt), GaError.SessionLost(let txt), GaError.TimeoutError(let txt):
                throw LoginError.connectionFailed(txt ?? "")
            default:
                throw LoginError.connectionFailed()
            }
        }
    }

    func networkParams(_ network: String) -> NetworkSettings {
        let appSettings = AppSettings.read()
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? CVarArg ?? ""
        let proxyURI = String(format: "socks5://%@:%@/", appSettings?.socks5Hostname ?? "", appSettings?.socks5Port ?? "")
        let gdkNetwork = getGdkNetwork(network)
    
        let electrumUrl: String? = {
            if let srv = appSettings?.btcElectrumSrv, gdkNetwork.mainnet && !gdkNetwork.liquid && !srv.isEmpty {
                return srv
            } else if let srv = appSettings?.testnetElectrumSrv, !gdkNetwork.mainnet && !gdkNetwork.liquid && !srv.isEmpty {
                return srv
            } else if let srv = appSettings?.liquidElectrumSrv, gdkNetwork.mainnet && gdkNetwork.liquid && !srv.isEmpty {
                return srv
            } else if let srv = appSettings?.liquidTestnetElectrumSrv, !gdkNetwork.mainnet && gdkNetwork.liquid && !srv.isEmpty {
                return srv
            } else {
                return nil
            }
        }()
        
        let networkSettings = NetworkSettings(
            name: network,
            useTor: appSettings?.tor ?? false,
            proxy: (appSettings?.proxy ?? false) ? proxyURI : nil,
            userAgent: String(format: "green_ios_%@", version),
            spvEnabled: (appSettings?.spvEnabled ?? false) && !gdkNetwork.liquid,
            electrumUrl: (appSettings?.personalNodeEnabled ?? false) ? electrumUrl : nil)
        return networkSettings
    }

    func walletIdentifier(_ network: String, credentials: Credentials) -> WalletIdentifier? {
        let res = try? self.session?.getWalletIdentifier(
            net_params: networkParams(network).toDict() ?? [:],
            details: credentials.toDict() ?? [:])
        return WalletIdentifier.from(res ?? [:]) as? WalletIdentifier
    }

    func walletIdentifier(_ network: String, masterXpub: String) -> WalletIdentifier? {
        let details = ["master_xpub": masterXpub]
        let res = try? self.session?.getWalletIdentifier(
            net_params: networkParams(network).toDict() ?? [:],
            details: details)
        return WalletIdentifier.from(res ?? [:]) as? WalletIdentifier
    }

    func existDatadir(credentials: Credentials? = nil, masterXpub: String? = nil) -> Bool {
        if let path = GdkInit.defaults().datadir {
            let hashes: WalletIdentifier? = {
                if let masterXpub = masterXpub {
                    return walletIdentifier(gdkNetwork.network, masterXpub: masterXpub)
                } else if let credentials = credentials {
                    return walletIdentifier(gdkNetwork.network, credentials: credentials)
                }
                return nil
            }()
            let dir = "\(path)/state/\(hashes?.walletHashId ?? "")"
            return FileManager.default.fileExists(atPath: dir)
        }
        return false
    }

    func removeDatadir(credentials: Credentials? = nil, masterXpub: String? = nil) {
        if let path = GdkInit.defaults().datadir {
            let hashes: WalletIdentifier? = {
                if let masterXpub = masterXpub {
                    return walletIdentifier(gdkNetwork.network, masterXpub: masterXpub)
                } else if let credentials = credentials {
                    return walletIdentifier(gdkNetwork.network, credentials: credentials)
                }
                return nil
            }()
            let dir = "\(path)/state/\(hashes?.walletHashId ?? "")"
            try? FileManager.default.removeItem(atPath: dir)
        }
    }

    func transactions(subaccount: UInt32, first: UInt32 = 0) -> Promise<Transactions> {
        return Guarantee()
            .compactMap { _ in try self.session?.getTransactions(details: ["subaccount": subaccount,
                                                        "first": first,
                                                        "count": Constants.trxPerPage]) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .compactMap { data in
                let result = data["result"] as? [String: Any]
                let dict = result?["transactions"] as? [[String: Any]]
                let list = dict?.map { Transaction($0) }
                return Transactions(list: list ?? [])
            }.tapLogger()
    }

    func subaccount(_ pointer: UInt32) -> Promise<WalletItem> {
        return Guarantee()
            .compactMap { try self.session?.getSubaccount(subaccount: pointer) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .compactMap { data in
                let result = data["result"] as? [String: Any]
                let wallet = WalletItem.from(result ?? [:]) as? WalletItem
                wallet?.network = self.gdkNetwork.network
                return wallet
            }.tapLogger()
    }

    func subaccounts(_ refresh: Bool = false) -> Promise<[WalletItem]> {
        let params = GetSubaccountsParams(refresh: refresh)
        return Guarantee()
            .then { self.wrapper(fun: self.session?.getSubaccounts, params: params) }
            .compactMap { $0 }
            .compactMap { (res: GetSubaccountsResult) in
                let wallets = res.subaccounts
                wallets.forEach { $0.network = self.gdkNetwork.network }
                return wallets.sorted()
            }
    }

    func loadTwoFactorConfig() -> Promise<TwoFactorConfig> {
        return Guarantee()
            .compactMap { try self.session?.getTwoFactorConfig() }
            .compactMap { dataTwoFactorConfig in
                let res = TwoFactorConfig.from(dataTwoFactorConfig) as? TwoFactorConfig
                self.twoFactorConfig = res
                return res
            }.tapLogger()
    }

    func loadSettings() -> Promise<Settings?> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.getSettings() }
            .compactMap { data in
                self.settings = Settings.from(data)
                return self.settings
            }.tapLogger()
    }

    // create a default segwit account if doesn't exist on singlesig
    func createDefaultSubaccount(wallets: [WalletItem]) -> Promise<Void> {
        let notFound = !wallets.contains(where: {$0.type == AccountType.segWit })
        if gdkNetwork.electrum && notFound {
            return Guarantee()
                .compactMap { try self.session?.createSubaccount(details: ["name": "",
                                                             "type": AccountType.segWit.rawValue]) }
                .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
                .tapLogger()
                .asVoid()
        }
        return Promise<Void>().asVoid()
    }

    func reconnect() -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.loginUserSW(details: [:]) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .tapLogger()
            .asVoid()
    }

    func loginUser(_ params: Credentials) -> Promise<LoginUserResult> {
        let discovery = gdkNetwork.electrum && !existDatadir(credentials: params)
        return connect()
            .then { self.wrapper(fun: self.session?.loginUserSW, params: params) }
            .compactMap { $0 }
            .then { res in self.onLogin(res).compactMap { res } }
            .then { res in discovery ? self.discovery(credentials: params, removeDatadir: false).compactMap { res } : Promise.value(res) }
    }

    func loginUser(_ params: HWDevice) -> Promise<LoginUserResult> {
        return connect()
            .then { self.wrapper(fun: self.session?.loginUserHW, params: params) }
            .compactMap { $0 }
            .then { res in self.onLogin(res).compactMap { res } }
    }

    func login(credentials: Credentials? = nil, hw: HWDevice? = nil) -> Promise<LoginUserResult> {
        if let credentials = credentials {
            return loginUser(credentials)
        } else if let hw = hw {
            return loginUser(hw)
        } else {
            return Promise<LoginUserResult>() { seal in seal.reject(GaError.GenericError("No login method specified")) }
        }
    }

    private func onLogin(_ data: LoginUserResult) -> Promise<Void> {
        self.session?.logged = true
        if !self.gdkNetwork.electrum {
            return self.loadTwoFactorConfig().asVoid().recover {_ in }
        }
        return Promise().asVoid()
    }

    typealias GdkFunc = ([String: Any]) throws -> TwoFactorCall

    func wrapper<T: Codable, K: Codable>(fun: GdkFunc?, params: T) -> Promise<K?> {
        let bgq = DispatchQueue.global(qos: .background)
        let dict = params.toDict()
        return Guarantee()
            .compactMap(on: bgq) { try fun?(dict ?? [:]) }
            .then(on: bgq) { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .compactMap { res in
                let result = res["result"] as? [String: Any]
                return K.from(result ?? [:]) as? K
            }.tapLogger()
    }

    func decryptWithPin(_ params: DecryptWithPinParams) -> Promise<Credentials> {
        return wrapper(fun: self.session?.decryptWithPin, params: params)
            .compactMap { $0 }
    }

    func load(refreshSubaccounts: Bool = true) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { _ -> Promise<Void> in
                if refreshSubaccounts {
                    return self.subaccounts(true)
                        .recover { _ in self.subaccounts(false) }
                        .then(on: bgq) { self.createDefaultSubaccount(wallets: $0) }
                }
                return Promise<Void>().asVoid()
            }.tapLogger()
    }

    func getCredentials(password: String) -> Promise<Credentials> {
        let cred = Credentials(password: password)
        return wrapper(fun: self.session?.getCredentials, params: cred)
            .compactMap { $0 }
    }

    func register(credentials: Credentials? = nil, hw: HWDevice? = nil) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { self.connect() }
            .compactMap(on: bgq) { try self.session?.registerUser(details: credentials?.toDict() ?? [:], hw_device: ["device": hw?.toDict() ?? [:]]) }
            .then(on: bgq) { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .tapLogger()
            .asVoid()
    }

    func encryptWithPin(_ params: EncryptWithPinParams) -> Promise<EncryptWithPinResult> {
        return wrapper(fun: self.session?.encryptWithPin, params: params)
            .compactMap { $0 }
    }

    func resetTwoFactor(email: String, isDispute: Bool) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.resetTwoFactor(email: email, isDispute: isDispute) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .asVoid()
            .tapLogger()
    }

    func cancelTwoFactorReset() -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.cancelTwoFactorReset() }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .asVoid()
            .tapLogger()
    }

    func undoTwoFactorReset(email: String) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.undoTwoFactorReset(email: email) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .asVoid()
            .tapLogger()
    }

    func setWatchOnly(username: String, password: String) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.setWatchOnly(username: username, password: password) }
            .tapLogger()
    }
    func getWatchOnlyUsername() -> Promise<String> {
        return Guarantee()
            .compactMap { try self.session?.getWatchOnlyUsername() }
            .tapLogger()
    }

    func setCSVTime(value: Int) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.setCSVTime(details: ["value": value]) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .asVoid()
    }

    func setTwoFactorLimit(details: [String: Any]) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.setTwoFactorLimit(details: details) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .asVoid()
            .tapLogger()
    }

    func convertAmount(input: [String: Any]) throws -> [String: Any] {
        try self.session?.convertAmount(input: input) ?? [:]
    }

    func refreshAssets(icons: Bool, assets: Bool, refresh: Bool) throws {
        try self.session?.refreshAssets(params: ["icons": icons, "assets": assets, "refresh": refresh])
    }

    func getReceiveAddress(subaccount: UInt32) -> Promise<Address> {
        let params = Address(address: nil, pointer: nil, branch: nil, subtype: nil, userPath: nil, subaccount: subaccount, scriptType: nil, addressType: nil, script: nil)
        return wrapper(fun: self.session?.getReceiveAddress, params: params)
            .compactMap { $0 }
    }

    func getBalance(subaccount: UInt32, numConfs: Int) -> Promise<[String: Int64]> {
        return Guarantee()
            .compactMap { try self.session?.getBalance(details: ["subaccount": subaccount, "num_confs": numConfs]) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .compactMap { $0["result"] as? [String: Int64] }
            .tapLogger()
    }

    func changeSettingsTwoFactor(method: TwoFactorType, config: TwoFactorConfigItem) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.changeSettingsTwoFactor(method: method.rawValue, details: config.toDict() ?? [:]) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .asVoid()
            .tapLogger()
    }

    func updateSubaccount(subaccount: UInt32, hidden: Bool) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.updateSubaccount(details: ["subaccount": subaccount, "hidden": hidden]) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .asVoid()
            .tapLogger()
    }

    func createSubaccount(_ details: CreateSubaccountParams) -> Promise<WalletItem> {
        return wrapper(fun: self.session?.createSubaccount, params: details)
            .compactMap { $0 }
            .compactMap { (wallet: WalletItem) in wallet.network = self.gdkNetwork.network; return wallet }
    }

    func renameSubaccount(subaccount: UInt32, newName: String) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.renameSubaccount(subaccount: subaccount, newName: newName) }
            .tapLogger()
            .asVoid()
    }

    func changeSettings(settings: Settings) -> Promise<Settings?> {
        return wrapper(fun: self.session?.changeSettings, params: settings)
    }

    func getUnspentOutputs(subaccount: UInt32, numConfs: Int) -> Promise<[String: Any]> {
        return Guarantee()
            .compactMap { try self.session?.getUnspentOutputs(details: ["subaccount": subaccount, "num_confs": numConfs]) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .compactMap { res in
                let result = res["result"] as? [String: Any]
                return result?["unspent_outputs"] as? [String: Any]
            }.tapLogger()
    }

    func createTransaction(tx: Transaction) -> Promise<Transaction> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.createTransaction(details: tx.details) }
            .then(on: bgq) { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .compactMap(on: bgq) { Transaction($0["result"] as? [String: Any] ?? [:]) }
            .tapLogger()
    }

    func signTransaction(tx: Transaction) -> Promise<[String: Any]> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.signTransaction(details: tx.details) }
            .then(on: bgq) { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .compactMap(on: bgq) { $0["result"] as? [String: Any] }
            .tapLogger()
    }

    func broadcastTransaction(txHex: String) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.broadcastTransaction(tx_hex: txHex) }
            .tapLogger()
            .asVoid()
    }

    func sendTransaction(tx: Transaction) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.sendTransaction(details: tx.details) }
            .then(on: bgq) { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .tapLogger()
            .asVoid()
    }

    func getFeeEstimates() -> [UInt64]? {
        let estimates = try? session?.getFeeEstimates()
        return estimates == nil ? nil : estimates!["fees"] as? [UInt64]
    }

    func loadSystemMessage() -> Promise<String?> {
        return Guarantee()
            .compactMap { try self.session?.getSystemMessage() }
            .tapLogger()
    }

    func ackSystemMessage(message: String) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.ackSystemMessage(message: message) }
            .then { ResolverManager($0, chain: self.gdkNetwork.chain).run() }
            .tapLogger()
            .asVoid()
    }

    func getAvailableCurrencies() -> Promise<[String: [String]]> {
        return Guarantee()
            .compactMap { try self.session?.getAvailableCurrencies() }
            .compactMap { $0?["per_exchange"] as? [String: [String]] }
            .tapLogger()
    }

    func validBip21Uri(uri: String) -> Bool {
        if let prefix = gdkNetwork.bip21Prefix {
            return uri.starts(with: prefix)
        }
        return false
    }

    func getAssets(params: GetAssetsParams) -> GetAssetsResult? {
        if let res = try? session?.getAssets(params: params.toDict() ?? [:]) {
            return GetAssetsResult.from(res) as? GetAssetsResult
        }
        return nil
    }

    func restore(credentials: Credentials? = nil, hw: HWDevice? = nil, forceJustRestored: Bool = false) -> Promise<LoginUserResult> {
        let existDatadir = existDatadir(credentials: credentials)
        var loginData: LoginUserResult?
        return Guarantee()
            .then { self.login(credentials: credentials, hw: hw) }
            .map {
                loginData = $0
                // Avoid to restore existing wallets, unless HW
                if let account = AccountsRepository.shared.find(xpubHashId: $0.xpubHashId),
                   account.gdkNetwork == self.gdkNetwork && hw == nil && !forceJustRestored {
                    throw LoginError.walletsJustRestored()
                }
            }
            .then { self.discovery(credentials: credentials, hw: hw, removeDatadir: !existDatadir) }
            .compactMap { loginData }
    }

    func discovery(credentials: Credentials? = nil, hw: HWDevice? = nil, removeDatadir: Bool) -> Promise<Void> {
        return Guarantee()
            .then { _ in self.subaccounts(true).recover { _ in Promise(error: LoginError.connectionFailed()) }}
            .compactMap { $0.filter({ $0.pointer == 0 }).first }
            .then { !($0.bip44Discovered ?? false) ? self.updateSubaccount(subaccount: 0, hidden: true) : Promise().asVoid() }
            .then { _ in self.subaccounts() }
            .compactMap { subaccounts in
                // Remove liquid account if not found
                if subaccounts.filter({ $0.bip44Discovered ?? false }).isEmpty {
                    if removeDatadir {
                        self.disconnect()
                        self.removeDatadir(credentials: credentials)
                    }
                    return
                }
            }.asVoid()
    }

    func networkConnect() {
        SessionManager.reconnectionQueue.async {
            try? self.session?.reconnectHint(hint: ["tor_hint": "connect", "hint": "connect"])
        }
    }

    func networkDisconnect() {
        SessionManager.reconnectionQueue.async {
            try? self.session?.reconnectHint(hint: ["tor_hint": "disconnect", "hint": "disconnect"])
        }
    }

    func httpRequest(params: [String: Any]) -> [String: Any]? {
        return try? session?.httpRequest(params: params)
    }
}
