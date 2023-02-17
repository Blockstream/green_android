import Foundation
import PromiseKit

public enum LoginError: Error, Equatable {
    case walletsJustRestored(_ localizedDescription: String? = nil)
    case walletNotFound(_ localizedDescription: String? = nil)
    case invalidMnemonic(_ localizedDescription: String? = nil)
    case connectionFailed(_ localizedDescription: String? = nil)
    case failed(_ localizedDescription: String? = nil)
    case walletMismatch(_ localizedDescription: String? = nil)
}

class GDKSession: Session {
    var connected = false
    var logged = false
    var ephemeral = false
    var netParams = [String: Any]()

    override func connect(netParams: [String: Any]) throws {
        try super.connect(netParams: netParams)
        self.connected = true
        self.netParams = netParams
    }

    override init() {
        try! super.init()
    }

    deinit {
        super.setNotificationHandler(notificationCompletionHandler: nil)
    }

    public func loginUserSW(details: [String: Any]) throws -> TwoFactorCall {
        try loginUser(details: details)
    }

    public func loginUserHW(device: [String: Any]) throws -> TwoFactorCall {
        try loginUser(details: [:], hw_device: ["device": device])
    }

    public func registerUserSW(details: [String: Any]) throws -> TwoFactorCall {
        try registerUser(details: details)
    }

    public func registerUserHW(device: [String: Any]) throws -> TwoFactorCall {
        try registerUser(details: [:], hw_device: ["device": device])
    }

}

class SessionManager {

    var notificationManager: NotificationManager?
    var twoFactorConfig: TwoFactorConfig?
    var settings: Settings?
    var session: GDKSession?
    var gdkNetwork: GdkNetwork
    var registry: AssetsManager?

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
        session = nil
    }

    public func connect() -> Promise<Void> {
        if session?.connected ?? false {
            return Promise().asVoid()
        }
        return Guarantee()
            .compactMap { try self.connect(network: self.gdkNetwork.network) }
            .compactMap { AnalyticsManager.shared.setupSession(session: self.session) } // Update analytics endpoint with session tor/proxy
    }

    private func connect(network: String, params: [String: Any]? = nil) throws {
        do {
            if notificationManager == nil {
                self.notificationManager = NotificationManager(session: self)
            }
            if let notificationManager = notificationManager {
                session?.setNotificationHandler(notificationCompletionHandler: notificationManager.newNotification)
            }
            try session?.connect(netParams: networkParams(network, params: params))
        } catch {
            switch error {
            case GaError.GenericError(let txt), GaError.SessionLost(let txt), GaError.TimeoutError(let txt):
                throw LoginError.connectionFailed(txt ?? "")
            default:
                throw LoginError.connectionFailed()
            }
        }
    }

    func networkParams(_ network: String, params: [String: Any]? = nil) -> [String: Any] {
        let networkSettings = params ?? getUserNetworkSettings()
        let useProxy = networkSettings["proxy"] as? Bool ?? false
        let socks5Hostname = useProxy ? networkSettings["socks5_hostname"] as? String ?? "" : ""
        let socks5Port = useProxy ? networkSettings["socks5_port"] as? String ?? "" : ""
        let useTor = networkSettings["tor"] as? Bool ?? false
        let proxyURI = useProxy ? String(format: "socks5://%@:%@/", socks5Hostname, socks5Port) : ""
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? CVarArg ?? ""
        let userAgent = String(format: "green_ios_%@", version)
        var netParams: [String: Any] = ["name": network, "use_tor": useTor, "proxy": proxyURI, "user_agent": userAgent]

        // SPV available only for btc singlesig
        let spvEnabled = networkSettings[Constants.spvEnabled] as? Bool
        netParams["spv_enabled"] = (spvEnabled ?? false) && !getGdkNetwork(network).liquid

        // Personal nodes
        if let personalNodeEnabled = networkSettings[Constants.personalNodeEnabled] as? Bool, personalNodeEnabled {
            if let btcElectrumSrv = networkSettings[Constants.btcElectrumSrv] as? String,
                    network == Constants.electrumPrefix + "mainnet" && !btcElectrumSrv.isEmpty {
                netParams["electrum_url"] = btcElectrumSrv
            } else if let testnetElectrumSrv = networkSettings[Constants.testnetElectrumSrv] as? String,
                network == Constants.electrumPrefix + "testnet" && !testnetElectrumSrv.isEmpty {
                netParams["electrum_url"] = testnetElectrumSrv
            } else if let liquidElectrumSrv = networkSettings[Constants.liquidElectrumSrv] as? String,
                network == Constants.electrumPrefix + "liquid" && !liquidElectrumSrv.isEmpty {
                netParams["electrum_url"] = liquidElectrumSrv
            } else if let liquidTestnetElectrumSrv = networkSettings[Constants.liquidTestnetElectrumSrv] as? String,
                network == Constants.electrumPrefix + "testnet-liquid" && !liquidTestnetElectrumSrv.isEmpty {
                netParams["electrum_url"] = liquidTestnetElectrumSrv
            }
        }
        return netParams
    }

    func walletIdentifier(_ network: String, credentials: Credentials) -> WalletIdentifier? {
        let res = try? self.session?.getWalletIdentifier(net_params: networkParams(network), details: credentials.toDict() ?? [:])
        return WalletIdentifier.from(res ?? [:]) as? WalletIdentifier
    }

    func walletIdentifier(_ network: String, masterXpub: String) -> WalletIdentifier? {
        let details = ["master_xpub": masterXpub]
        let res = try? self.session?.getWalletIdentifier(net_params: networkParams(network), details: details)
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
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { _ in try self.session?.getTransactions(details: ["subaccount": subaccount,
                                                        "first": first,
                                                        "count": Constants.trxPerPage]) }
            .then { $0.resolve(session: self) }
            .compactMap(on: bgq) { data in
                let result = data["result"] as? [String: Any]
                let dict = result?["transactions"] as? [[String: Any]]
                let list = dict?.map { Transaction($0) }
                return Transactions(list: list ?? [])
            }.tapLogger()
    }

    func subaccount(_ pointer: UInt32) -> Promise<WalletItem> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.getSubaccount(subaccount: pointer) }
            .then(on: bgq) { $0.resolve(session: self) }
            .compactMap(on: bgq) { data in
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
       let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.getTwoFactorConfig() }
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
        let bgq = DispatchQueue.global(qos: .background)
        let notFound = !wallets.contains(where: {$0.type == AccountType.segWit })
        if gdkNetwork.electrum && notFound {
            return Guarantee()
                .compactMap(on: bgq) { try self.session?.createSubaccount(details: ["name": "",
                                                             "type": AccountType.segWit.rawValue]) }
                .then(on: bgq) { $0.resolve(session: self) }
                .tapLogger()
                .asVoid()
        }
        return Promise<Void>().asVoid()
    }

    func reconnect() -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.loginUserSW(details: [:]) }
            .then { $0.resolve(session: self) }
            .tapLogger()
            .asVoid()
    }

    func loginUser(_ params: Credentials) -> Promise<LoginUserResult> {
        let initialized = gdkNetwork.electrum && existDatadir(credentials: params)
        return connect()
            .then { self.wrapper(fun: self.session?.loginUserSW, params: params) }
            .compactMap { $0 }
            .then { res in
                self.subaccounts(!initialized)
                    .then { _ in self.onLogin(res) }
                    .map { res }
            }
    }

    func loginUser(_ params: HWDevice) -> Promise<LoginUserResult> {
        return connect()
            .then { self.wrapper(fun: self.session?.loginUserHW, params: params) }
            .compactMap { $0 }
            .then { res in
                self.subaccounts()
                    .then { _ in self.onLogin(res) }
                    .map { res }
            }
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
            .then(on: bgq) { $0.resolve(session: self) }
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
            .then(on: bgq) { $0.resolve(session: self) }
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
            .then { $0.resolve(session: self) }.asVoid()
            .tapLogger()
    }

    func cancelTwoFactorReset() -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.cancelTwoFactorReset() }
            .then { $0.resolve(session: self) }.asVoid()
            .tapLogger()
    }

    func undoTwoFactorReset(email: String) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.undoTwoFactorReset(email: email) }
            .then { $0.resolve(session: self) }.asVoid()
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
            .then { $0.resolve(session: self) }.asVoid()
    }

    func setTwoFactorLimit(details: [String: Any]) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.setTwoFactorLimit(details: details) }
            .then { $0.resolve(session: self) }.asVoid()
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
            .then { $0.resolve(session: self) }
            .compactMap { $0["result"] as? [String: Int64] }
            .tapLogger()
    }

    func changeSettingsTwoFactor(method: TwoFactorType, config: TwoFactorConfigItem) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.changeSettingsTwoFactor(method: method.rawValue, details: config.toDict() ?? [:]) }
            .then { $0.resolve(session: self) }.asVoid()
            .tapLogger()
    }

    func updateSubaccount(subaccount: UInt32, hidden: Bool) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.updateSubaccount(details: ["subaccount": subaccount, "hidden": hidden]) }
            .then { $0.resolve(session: self) }.asVoid()
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
            .then { $0.resolve(session: self) }
            .compactMap { res in
                let result = res["result"] as? [String: Any]
                return result?["unspent_outputs"] as? [String: Any]
            }.tapLogger()
    }

    func createTransaction(tx: Transaction) -> Promise<Transaction> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.createTransaction(details: tx.details) }
            .then(on: bgq) { $0.resolve(session: self) }
            .compactMap(on: bgq) { Transaction($0["result"] as? [String: Any] ?? [:]) }
            .tapLogger()
    }

    func signTransaction(tx: Transaction) -> Promise<[String: Any]> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.signTransaction(details: tx.details) }
            .then(on: bgq) { $0.resolve(session: self) }
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
            .then(on: bgq) { $0.resolve(session: self) }
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
            .then { $0.resolve(session: self) }
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
}
