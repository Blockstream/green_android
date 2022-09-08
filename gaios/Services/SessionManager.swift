import Foundation
import PromiseKit

public enum LoginError: Error, Equatable {
    case walletsJustRestored(_ localizedDescription: String? = nil)
    case walletNotFound(_ localizedDescription: String? = nil)
    case invalidMnemonic(_ localizedDescription: String? = nil)
    case connectionFailed(_ localizedDescription: String? = nil)
}

extension Thenable {

    // extend PromiseKit::Thenable to handle exceptions
    func tapLogger(on: DispatchQueue? = conf.Q.return, flags: DispatchWorkItemFlags? = nil) -> Promise<T> {
       return tap(on: on, flags: flags) {
           switch $0 {
           case .rejected(let error):
               switch error {
               case TwoFactorCallError.failure(let txt), TwoFactorCallError.cancel(let txt):
                   AnalyticsManager.shared.recordException(txt)
               case GaError.GenericError(let txt), GaError.TimeoutError(let txt), GaError.SessionLost(let txt), GaError.NotAuthorizedError(let txt):
                   AnalyticsManager.shared.recordException(txt ?? "")
               default:
                   break
               }
           default:
               break
           }
       }
   }
}

class GDKSession: Session {
    var connected = false
    var logged = false
    var ephemeral = false

    override func connect(netParams: [String: Any]) throws {
        try super.connect(netParams: netParams)
        connected = true
    }

    override init() {
        try! super.init()
    }

    deinit {
        super.setNotificationHandler(notificationCompletionHandler: nil)
    }
}

class SessionManager {

    var notificationManager: NotificationManager?
    var twoFactorConfig: TwoFactorConfig?
    var settings: Settings?
    var session: GDKSession?
    var gdkNetwork: GdkNetwork

    var registry: AssetsManagerProtocol? {
        return AssetsManager.get(for: gdkNetwork)
    }

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
        // Connect
        do {
            if notificationManager == nil {
                self.notificationManager = NotificationManager(session: self)
            }
            if let notificationManager = notificationManager {
                session?.setNotificationHandler(notificationCompletionHandler: notificationManager.newNotification)
            }
            try session?.connect(netParams: netParams)
        } catch {
            switch error {
            case GaError.GenericError(let txt), GaError.SessionLost(let txt), GaError.TimeoutError(let txt):
                throw LoginError.connectionFailed(txt ?? "")
            default:
                throw LoginError.connectionFailed()
            }
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
            .recover { _ in
                Guarantee()
                    .compactMap(on: bgq) { try self.session?.getSubaccount(subaccount: 0) }
                    .then(on: bgq) { $0.resolve(session: self) }
            }.compactMap(on: bgq) { data in
                let result = data["result"] as? [String: Any]
                let jsonData = try JSONSerialization.data(withJSONObject: result ?? [:])
                let wallet = try JSONDecoder().decode(WalletItem.self, from: jsonData)
                wallet.network = self.gdkNetwork.network
                return wallet
            }.tapLogger()
    }

    func subaccounts(_ refresh: Bool = false) -> Promise<[WalletItem]> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.getSubaccounts(details: ["refresh": refresh]) }
            .then(on: bgq) { $0.resolve(session: self) }
            .compactMap(on: bgq) { data in
                let result = data["result"] as? [String: Any]
                let subaccounts = result?["subaccounts"] as? [[String: Any]]
                let jsonData = try JSONSerialization.data(withJSONObject: subaccounts ?? [:])
                let wallets = try JSONDecoder().decode([WalletItem].self, from: jsonData)
                wallets.forEach { $0.network = self.gdkNetwork.network }
                return wallets.sorted()
            }.tapLogger()
    }

    func loadTwoFactorConfig() -> Promise<TwoFactorConfig> {
       let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.getTwoFactorConfig() }
            .compactMap { dataTwoFactorConfig in
                let twoFactorConfig = try JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig, options: []))
                self.twoFactorConfig = twoFactorConfig
                return twoFactorConfig
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

    func discover(mnemonic: String?, password: String?) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .map(on: bgq) { _ in
                if let mnemonic = mnemonic {
                    guard try gaios.validateMnemonic(mnemonic: mnemonic) else {
                        throw LoginError.invalidMnemonic()
                    }
                }
            }.then(on: bgq) { _ in
                self.loginUser(details: ["mnemonic": mnemonic ?? "", "password": password ?? ""])
            }.map(on: bgq) { walletHashId in
                // check if wallet just exist
                if AccountsManager.shared.accounts.contains(where: {
                    $0.walletHashId == walletHashId && !$0.isHW &&
                    $0.networkName == self.gdkNetwork.network
                    }) {
                    throw LoginError.walletsJustRestored()
                }
            }.recover { err in
                switch err {
                case TwoFactorCallError.failure(let localizedDescription):
                    if localizedDescription == "id_login_failed", !self.gdkNetwork.electrum {
                        throw LoginError.walletNotFound()
                    }
                default:
                    throw err
                }
            }.then(on: bgq) {
                // discover subaccounts
                self.subaccounts(true).recover { _ in
                    Promise { _ in
                        throw LoginError.connectionFailed()
                    }
                }
            }.get(on: bgq) { wallets in
                // check account discover if singlesig
                if self.gdkNetwork.electrum {
                    if wallets.filter({ $0.bip44Discovered ?? false }).isEmpty {
                        throw LoginError.walletNotFound()
                    }
                }
           }.tapLogger()
            .asVoid()
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

    func restore(_ credentials: Credentials) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { self.loginWithCredentials(credentials) }
            .then(on: bgq) { _ in self.load(refreshSubaccounts: true) }
            .tapLogger()
            .asVoid()
    }

    func create(_ credentials: Credentials) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { self.connect() }
            .then(on: bgq) { _ in self.registerSW(credentials) }
            .then(on: bgq) { _ in self.loginWithCredentials(credentials) }
            .then(on: bgq) { _ in self.createDefaultSubaccount(wallets: []) }
            .then(on: bgq) { _ in self.load(refreshSubaccounts: false) }
            .recover { err in
                switch err {
                case LoginError.walletNotFound,
                    LoginError.walletsJustRestored:
                    // Enable restore for HW
                    return
                default:
                    throw err
                }
            }.tapLogger()
    }

    func reconnect() -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { self.loginUser() }
            .asVoid()
    }

    private func loginUser(details: [String: Any] = [:], hwDevice: [String: Any] = [:]) -> Promise<String> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { self.connect() }
            .compactMap(on: bgq) { try self.session?.loginUser(details: details, hw_device: hwDevice) }
            .then(on: bgq) { $0.resolve(session: self) }
            .compactMap { res in
                self.session?.logged = true
                let result = res["result"] as? [String: Any]
                return result?["wallet_hash_id"] as? String
            }.tapLogger()
    }

    func loginWithPin(_ pin: String, pinData: PinData) -> Promise<String> {
        let bgq = DispatchQueue.global(qos: .background)
        let data = try? JSONEncoder().encode(pinData)
        let pin_data = try? JSONSerialization.jsonObject(with: data ?? Data(), options: .allowFragments) as? [String: Any]
        return Guarantee()
            .then(on: bgq) { self.loginUser(details: ["pin": pin, "pin_data": pin_data ?? [:]]) }
    }

    func loginWithCredentials(_ credentials: Credentials) -> Promise<String> {
        let bgq = DispatchQueue.global(qos: .background)
        let data = try? JSONEncoder().encode(credentials)
        let details = try? JSONSerialization.jsonObject(with: data ?? Data(), options: .allowFragments) as? [String: Any]
        return Guarantee()
            .then(on: bgq) { self.loginUser(details: details ?? [:]) }
    }

    func decryptWithPin(pin: String, pinData: PinData) -> Promise<Credentials> {
        let bgq = DispatchQueue.global(qos: .background)
        let data = try? JSONEncoder().encode(pinData)
        let pindata = try? JSONSerialization.jsonObject(with: data ?? Data(), options: .allowFragments) as? [String: Any]
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.decryptWithPin(details: ["pin": pin, "pin_data": pindata ?? [:]]) }
            .then(on: bgq) { $0.resolve(session: self) }
            .compactMap { res in
                let result = res["result"] as? [String: Any]
                let json = try? JSONSerialization.data(withJSONObject: result, options: [])
                return try? JSONDecoder().decode(Credentials.self, from: json ?? Data())
            }
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
            }.map(on: bgq) {
                self.registry?.loadAsync(session: self)
            }.tapLogger()
    }

    func loginWatchOnly(_ username: String, _ password: String) -> Promise<String> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { self.loginUser(details: ["username": username, "password": password]) }
            .get { _ in self.registry?.loadAsync(session: self) }
    }

    func loginWithHW(_ hwDevice: HWDevice) -> Promise<String> {
        let bgq = DispatchQueue.global(qos: .background)
        let data = try? JSONEncoder().encode(hwDevice)
        let device = try? JSONSerialization.jsonObject(with: data ?? Data(), options: .allowFragments) as? [String: Any]
        return Guarantee()
            .then(on: bgq) { self.connect() }
            .then(on: bgq) { self.registerHW(hw: hwDevice) }
            .then(on: bgq) { self.loginUser(hwDevice: ["device": device ?? [:]]) }
    }

    func getCredentials(password: String) -> Promise<Credentials> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.getCredentials(details: ["password": password]) }
            .then(on: bgq) { $0.resolve(session: self) }
            .compactMap {
                let result = $0["result"] as? [String: Any]
                let jsonData = try JSONSerialization.data(withJSONObject: result ?? "")
                return try JSONDecoder().decode(Credentials.self, from: jsonData)
            }.tapLogger()
    }

    func registerSW(_ credentials: Credentials) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        let data = try? JSONEncoder().encode(credentials)
        let details = try? JSONSerialization.jsonObject(with: data ?? Data(), options: .allowFragments) as? [String: Any]
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.registerUser(details: details ?? [:], hw_device: [:]) }
            .then(on: bgq) { $0.resolve(session: self) }
            .tapLogger()
            .asVoid()
    }

    func registerHW(hw: HWDevice) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        let data = try? JSONEncoder().encode(hw)
        let device = try? JSONSerialization.jsonObject(with: data ?? Data(), options: .allowFragments)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.registerUser(details: [:], hw_device: ["device": device ?? [:]]) }
            .then(on: bgq) { $0.resolve(session: self) }
            .tapLogger()
            .asVoid()
    }

    func decryptWithPin(pin: String, pinData: PinData) -> Promise<Credentials> {
        let bgq = DispatchQueue.global(qos: .background)
        let text = try? JSONSerialization.jsonObject(with: JSONEncoder().encode(pinData), options: .allowFragments) as? [String: Any]
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.decryptWithPin(details: ["pin": pin, "pin_data": text ?? ""]) }
            .then(on: bgq) { $0.resolve(session: self) }
            .compactMap { res in
                let result = res["result"] as? [String: Any]
                let jsonData = try JSONSerialization.data(withJSONObject: result ?? "")
                return try JSONDecoder().decode(Credentials.self, from: jsonData)
            }.tapLogger()
    }

    func encryptWithPin(pin: String, text: [String: Any?]) -> Promise<PinData> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .compactMap(on: bgq) { try self.session?.encryptWithPin(details: ["pin": pin, "plaintext": text]) }
            .then(on: bgq) { $0.resolve(session: self) }
            .compactMap { res in
                let result = res["result"] as? [String: Any]
                let txt = result?["pin_data"] as? [String: Any]
                let jsonData = try JSONSerialization.data(withJSONObject: txt ?? "")
                return try JSONDecoder().decode(PinData.self, from: jsonData)
            }.tapLogger()
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
        return Guarantee()
            .compactMap { try self.session?.getReceiveAddress(details: ["subaccount": subaccount]) }
            .then { $0.resolve(session: self) }
            .compactMap { res in
                let result = res["result"] as? [String: Any]
                let data = try? JSONSerialization.data(withJSONObject: result!, options: [])
                return try? JSONDecoder().decode(Address.self, from: data!)
            }.tapLogger()
    }

    func getBalance(subaccount: UInt32, numConfs: Int) -> Promise<[String: Int64]> {
        return Guarantee()
            .compactMap { try self.session?.getBalance(details: ["subaccount": subaccount, "num_confs": numConfs]) }
            .then { $0.resolve(session: self) }
            .compactMap { $0["result"] as? [String: Int64] }
            .tapLogger()
    }

    func changeSettingsTwoFactor(method: TwoFactorType, config: TwoFactorConfigItem) -> Promise<Void> {
        let details = try? JSONSerialization.jsonObject(with: JSONEncoder().encode(config), options: .allowFragments) as? [String: Any]
        return Guarantee()
            .compactMap { try self.session?.changeSettingsTwoFactor(method: method.rawValue, details: details ?? [:]) }
            .then { $0.resolve(session: self) }.asVoid()
            .tapLogger()
    }

    func updateSubaccount(subaccount: UInt32, hidden: Bool) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.updateSubaccount(details: ["subaccount": subaccount, "hidden": hidden]) }
            .then { $0.resolve(session: self) }.asVoid()
            .tapLogger()
    }

    func createSubaccount(details: [String: Any]) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.createSubaccount(details: details) }
            .then { $0.resolve(session: self) }.asVoid()
    }

    func renameSubaccount(subaccount: UInt32, newName: String) -> Promise<Void> {
        return Guarantee()
            .compactMap { try self.session?.renameSubaccount(subaccount: subaccount, newName: newName) }
            .tapLogger()
            .asVoid()
    }

    func changeSettings(settings: Settings) -> Promise<Void> {
        let settings = try? JSONSerialization.jsonObject(with: JSONEncoder().encode(settings), options: .allowFragments) as? [String: Any]
        return Guarantee()
            .compactMap { try self.session?.changeSettings(details: settings ?? [:]) }
            .then { $0.resolve(session: self) }.asVoid()
            .tapLogger()
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
