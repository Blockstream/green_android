import Foundation

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
    
    //var notificationManager: NotificationManager?
    var twoFactorConfig: TwoFactorConfig?
    var settings: Settings?
    var session: GDKSession?
    var gdkNetwork: GdkNetwork
    var registry: AssetsManager?
    var blockHeight: UInt32 = 0
    
    var connected = false
    var logged = false
    weak var hw: BLEDevice?
    
    // Serial reconnect queue for network events
    static let reconnectionQueue = DispatchQueue(label: "reconnection_queue")
    let bgq = DispatchQueue.global(qos: .background)
    
    var isResetActive: Bool? {
        get { twoFactorConfig?.twofactorReset.isResetActive }
    }
    
    init(_ gdkNetwork: GdkNetwork) {
        self.gdkNetwork = gdkNetwork
        session = GDKSession()
        registry = AssetsManager(testnet: !gdkNetwork.mainnet)
    }
    
    deinit {
        logged = false
        connected = false
    }
    
    public func connect() async throws {
        if connected {
            return
        }
        self.networkConnect()
        try await self.connect(network: self.gdkNetwork.network)
        AnalyticsManager.shared.setupSession(session: self.session) // Update analytics endpoint with session tor/proxy
    }
    
    public func disconnect() async throws {
        logged = false
        connected = false
        SessionManager.reconnectionQueue.async {
            self.session = GDKSession()
        }
    }
    
    private func connect(network: String, params: [String: Any]? = nil) async throws {
        do {
            session?.setNotificationHandler(notificationCompletionHandler: newNotification)
            try session?.connect(netParams: GdkSettings.read()?.toNetworkParams(network).toDict() ?? [:])
            connected = true
        } catch {
            switch error {
            case GaError.GenericError(let txt), GaError.SessionLost(let txt), GaError.TimeoutError(let txt):
                throw LoginError.connectionFailed(txt ?? "")
            default:
                throw LoginError.connectionFailed()
            }
        }
    }
    
    func walletIdentifier(credentials: Credentials) -> WalletIdentifier? {
        let res = try? self.session?.getWalletIdentifier(
            net_params: GdkSettings.read()?.toNetworkParams(gdkNetwork.network).toDict() ?? [:],
            details: credentials.toDict() ?? [:])
        return WalletIdentifier.from(res ?? [:]) as? WalletIdentifier
    }
    
    func walletIdentifier(masterXpub: String) -> WalletIdentifier? {
        let details = ["master_xpub": masterXpub]
        let res = try? self.session?.getWalletIdentifier(
            net_params: GdkSettings.read()?.toNetworkParams(gdkNetwork.network).toDict() ?? [:],
            details: details)
        return WalletIdentifier.from(res ?? [:]) as? WalletIdentifier
    }
    
    func existDatadir(masterXpub: String) -> Bool  {
        if let hash = walletIdentifier(masterXpub: masterXpub) {
            return existDatadir(walletHashId: hash.walletHashId)
        }
        return false
    }
    
    func existDatadir(credentials: Credentials) -> Bool  {
        if let hash = walletIdentifier(credentials: credentials) {
            return existDatadir(walletHashId: hash.walletHashId)
        }
        return false
    }
    
    func existDatadir(walletHashId: String) -> Bool  {
        // true for multisig
        if gdkNetwork.multisig {
            return true
        }
        if let path = GdkInit.defaults().datadir {
            let dir = "\(path)/state/\(walletHashId)"
            return FileManager.default.fileExists(atPath: dir)
        }
        return false
    }
    
    func removeDatadir(masterXpub: String) {
        if let hash = walletIdentifier(masterXpub: masterXpub) {
            removeDatadir(walletHashId: hash.walletHashId)
        }
    }
    
    func removeDatadir(credentials: Credentials) {
        if let hash = walletIdentifier(credentials: credentials) {
            removeDatadir(walletHashId: hash.walletHashId)
        }
    }
    
    func removeDatadir(walletHashId: String) {
        if let path = GdkInit.defaults().datadir {
            let dir = "\(path)/state/\(walletHashId)"
            try? FileManager.default.removeItem(atPath: dir)
        }
    }
    
    func enabled() -> Bool {
        return true
    }
    
    func resolve(_ twoFactorCall: TwoFactorCall?) async throws -> [String: Any]? {
        let rm = ResolverManager(twoFactorCall, chain: self.gdkNetwork.chain, hwDevice: hw?.interface)
        return try await rm.run()
    }
    
    func transactions(subaccount: UInt32, first: UInt32 = 0) async throws -> Transactions {
        let txs = try self.session?.getTransactions(details: ["subaccount": subaccount,
                                                              "first": first,
                                                              "count": Constants.trxPerPage])
        let res = try await resolve(txs)
        let result = res?["result"] as? [String: Any]
        let dict = result?["transactions"] as? [[String: Any]]
        let list = dict?.map { Transaction($0) }
        return Transactions(list: list ?? [])
    }
    
    func subaccount(_ pointer: UInt32) async throws -> WalletItem? {
        let subaccount = try self.session?.getSubaccount(subaccount: pointer)
        let res = try await resolve(subaccount)
        let result = res?["result"] as? [String: Any]
        let wallet = WalletItem.from(result ?? [:]) as? WalletItem
        wallet?.network = self.gdkNetwork.network
        return wallet
    }

    func subaccounts(_ refresh: Bool = false) async throws -> [WalletItem] {
        let params = GetSubaccountsParams(refresh: refresh)
        let res: GetSubaccountsResult = try await wrapperAsync(fun: self.session?.getSubaccounts, params: params)
        let wallets = res.subaccounts
        wallets.forEach { $0.network = self.gdkNetwork.network }
        return wallets.sorted()
    }
    
    func parseTxInput(_ input: String, satoshi: Int64?, assetId: String?) async throws -> ValidateAddresseesResult {
        let asset = assetId == AssetInfo.btcId ? nil : assetId
        let addressee = Addressee.from(address: input, satoshi: satoshi, assetId: asset)
        let addressees = ValidateAddresseesParams(addressees: [addressee])
        return try await self.wrapperAsync(fun: self.session?.validate, params: addressees)
    }
    
    func loadTwoFactorConfig() async throws -> TwoFactorConfig? {
        if let dataTwoFactorConfig = try self.session?.getTwoFactorConfig() {
            let res = TwoFactorConfig.from(dataTwoFactorConfig) as? TwoFactorConfig
            self.twoFactorConfig = res
        }
        return self.twoFactorConfig
    }
    
    func loadSettings() async throws -> Settings? {
        if let data = try self.session?.getSettings() {
            self.settings = Settings.from(data)
        }
        return self.settings
    }
    
    // create a default segwit account if doesn't exist on singlesig
    func createDefaultSubaccount(wallets: [WalletItem]) async throws {
        let notFound = !wallets.contains(where: {$0.type == AccountType.segWit })
        if gdkNetwork.electrum && notFound {
            let res = try self.session?.createSubaccount(details: ["name": "", "type": AccountType.segWit.rawValue])
            _ = try await resolve(res)
        }
    }
    
    func reconnect() async throws {
        let res = try self.session?.loginUserSW(details: [:])
        _ = try await resolve(res)
    }
    
    func loginUser(_ params: Credentials, restore: Bool) async throws -> LoginUserResult {
        try await connect()
        let res: LoginUserResult = try await self.wrapperAsync(fun: self.session?.loginUserSW, params: params)
        try await onLogin(res)
        return res
    }
    
    func loginUser(_ params: HWDevice, restore: Bool) async throws -> LoginUserResult {
        try await connect()
        let res: LoginUserResult = try await self.wrapperAsync(fun: self.session?.loginUserHW, params: params)
        try await onLogin(res)
        return res
    }
    
    func loginUser(credentials: Credentials? = nil, hw: HWDevice? = nil, restore: Bool) async throws -> LoginUserResult {
        if let credentials = credentials {
            return try await loginUser(credentials, restore: restore)
        } else if let hw = hw {
            return try await loginUser(hw, restore: restore)
        } else {
            throw GaError.GenericError("No login method specified")
        }
    }
    
    private func onLogin(_ data: LoginUserResult) async throws {
        logged = true
        if self.gdkNetwork.multisig {
            //try await self.loadTwoFactorConfig()
        }
    }
    
    typealias GdkFunc = ([String: Any]) throws -> TwoFactorCall

    func wrapperAsync<T: Codable, K: Codable>(fun: GdkFunc?, params: T) async throws -> K {
        let dict = params.toDict()
        if let fun = try fun?(dict ?? [:]) {
            let res = try await resolve(fun)
            let result = res?["result"] as? [String: Any]
            if let res = K.from(result ?? [:]) as? K {
                return res
            }
        }
        throw GaError.GenericError()
    }
    
    func decryptWithPin(_ params: DecryptWithPinParams) async throws -> Credentials {
        return try await wrapperAsync(fun: self.session?.decryptWithPin, params: params)
    }
    
    func load(refreshSubaccounts: Bool = true) async throws {
        if refreshSubaccounts {
            do {
                _ = try await self.subaccounts(true)
            } catch { }
            let subaccounts = try await self.subaccounts(false)
            _ = try await createDefaultSubaccount(wallets: subaccounts)
        }
    }
    
    func getCredentials(password: String) async throws -> Credentials? {
        let cred = Credentials(password: password)
        let res: Credentials = try await wrapperAsync(fun: self.session?.getCredentials, params: cred)
        return res
    }

    func register(credentials: Credentials? = nil, hw: HWDevice? = nil) async throws {
        try await self.connect()
        let res = try self.session?.registerUser(details: credentials?.toDict() ?? [:], hw_device: ["device": hw?.toDict() ?? [:]])
        _ = try await resolve(res)
    }
    
    func encryptWithPin(_ params: EncryptWithPinParams) async throws -> EncryptWithPinResult {
        return try await wrapperAsync(fun: self.session?.encryptWithPin, params: params)
    }

    func resetTwoFactor(email: String, isDispute: Bool) async throws {
        let res = try self.session?.resetTwoFactor(email: email, isDispute: isDispute)
        _ = try await resolve(res)
    }
    
    func cancelTwoFactorReset() async throws {
        let res = try self.session?.cancelTwoFactorReset()
        _ = try await resolve(res)
    }
    
    func undoTwoFactorReset(email: String) async throws {
        let res = try self.session?.undoTwoFactorReset(email: email)
        _ = try await resolve(res)
    }
    
    func setWatchOnly(username: String, password: String) async throws {
        _ = try self.session?.setWatchOnly(username: username, password: password)
    }
    
    func getWatchOnlyUsername() async throws -> String? {
        return try session?.getWatchOnlyUsername()
    }

    func setCSVTime(value: Int) async throws {
        let res = try self.session?.setCSVTime(details: ["value": value])
        _ = try await resolve(res)
    }
    
    func setTwoFactorLimit(details: [String: Any]) async throws {
        let res = try self.session?.setTwoFactorLimit(details: details)
        _ = try await resolve(res)
    }
    
    func convertAmount(input: [String: Any]) throws -> [String: Any] {
        try self.session?.convertAmount(input: input) ?? [:]
    }
    
    func refreshAssets(icons: Bool, assets: Bool, refresh: Bool) throws {
        try self.session?.refreshAssets(params: ["icons": icons, "assets": assets, "refresh": refresh])
    }
    
    func getReceiveAddress(subaccount: UInt32) async throws -> Address {
        let params = Address(address: nil, pointer: nil, branch: nil, subtype: nil, userPath: nil, subaccount: subaccount, scriptType: nil, addressType: nil, script: nil)
        let res: Address = try await wrapperAsync(fun: self.session?.getReceiveAddress, params: params)
        return res
    }
    
    func getBalance(subaccount: UInt32, numConfs: Int) async throws -> [String: Int64] {
        let balance = try self.session?.getBalance(details: ["subaccount": subaccount, "num_confs": numConfs])
        let res = try await resolve(balance)
        return res?["result"] as? [String: Int64] ?? [:]
    }
    
    func changeSettingsTwoFactor(method: TwoFactorType, config: TwoFactorConfigItem) async throws {
        let res = try self.session?.changeSettingsTwoFactor(method: method.rawValue, details: config.toDict() ?? [:])
        _ = try await resolve(res)
    }
    
    func updateSubaccount(subaccount: UInt32, hidden: Bool) async throws {
        let res = try self.session?.updateSubaccount(details: ["subaccount": subaccount, "hidden": hidden])
        _ = try await resolve(res)
    }
    
    func createSubaccount(_ details: CreateSubaccountParams) async throws -> WalletItem {
        let wallet: WalletItem = try await wrapperAsync(fun: self.session?.createSubaccount, params: details)
        wallet.network = self.gdkNetwork.network
        return wallet
    }
    
    func renameSubaccount(subaccount: UInt32, newName: String) async throws {
        try self.session?.renameSubaccount(subaccount: subaccount, newName: newName)
    }
    
    func changeSettings(settings: Settings) async throws -> Settings? {
        return try await wrapperAsync(fun: self.session?.changeSettings, params: settings)
    }
    
    func getUnspentOutputs(subaccount: UInt32, numConfs: Int) async throws -> [String: Any] {
        let utxos = try self.session?.getUnspentOutputs(details: ["subaccount": subaccount, "num_confs": numConfs])
        let res = try await resolve(utxos)
        let result = res?["result"] as? [String: Any]
        return result?["unspent_outputs"] as? [String: Any] ?? [:]
    }
    
    func wrapperTransaction(fun: GdkFunc?, tx: Transaction) async throws -> Transaction {
        if let fun = try fun?(tx.details) {
            let res = try await resolve(fun)
            let result = res?["result"] as? [String: Any]
            return Transaction(result ?? [:], subaccount: tx.subaccount)
        }
        throw GaError.GenericError()
    }

    func createTransaction(tx: Transaction) async throws -> Transaction {
        try await wrapperTransaction(fun: self.session?.createTransaction, tx: tx)
    }

    func blindTransaction(tx: Transaction) async throws -> Transaction {
        try await wrapperTransaction(fun: self.session?.blindTransaction, tx: tx)
    }

    func signTransaction(tx: Transaction) async throws -> Transaction {
        try await wrapperTransaction(fun: self.session?.signTransaction, tx: tx)
    }

    func sendTransaction(tx: Transaction) async throws {
        _ = try await wrapperTransaction(fun: self.session?.sendTransaction, tx: tx)
    }

    func broadcastTransaction(txHex: String) async throws {
        _ = try self.session?.broadcastTransaction(tx_hex: txHex)
    }

    func getFeeEstimates() async throws -> [UInt64]? {
        let estimates = try? session?.getFeeEstimates()
        return estimates == nil ? nil : estimates!["fees"] as? [UInt64]
    }

    func loadSystemMessage() async throws -> String? {
        try self.session?.getSystemMessage()
    }

    func ackSystemMessage(message: String) async throws {
        let res = try self.session?.ackSystemMessage(message: message)
        _ = try await resolve(res)
    }

    func getAvailableCurrencies() async throws -> [String: [String]] {
        let res = try self.session?.getAvailableCurrencies()
        return res?["per_exchange"] as? [String: [String]] ?? [:]
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

    func discovery() async throws -> Bool {
        do {
            let subaccounts = try await self.subaccounts(true)
            if let first = subaccounts.filter({ $0.pointer == 0 }).first,
               first.isSinglesig && !(first.bip44Discovered ?? false) {
                _ = try await self.updateSubaccount(subaccount: 0, hidden: true)
            }
            return !subaccounts.filter({ $0.bip44Discovered ?? false }).isEmpty
        } catch { throw LoginError.connectionFailed() }
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
extension SessionManager {
    public func newNotification(notification: [String: Any]?) {
        guard let notificationEvent = notification?["event"] as? String,
                let event = EventType(rawValue: notificationEvent),
                let data = notification?[event.rawValue] as? [String: Any] else {
            return
        }
        #if DEBUG
        print("notification \(event): \(data)")
        #endif
        switch event {
        case .Block:
            guard let height = data["block_height"] as? UInt32 else { break }
            blockHeight = height
            post(event: .Block, userInfo: data)
        case .Transaction:
            post(event: .Transaction, userInfo: data)
            let txEvent = TransactionEvent.from(data) as? TransactionEvent
            if txEvent?.type == "incoming" {
                txEvent?.subAccounts.forEach { pointer in
                    post(event: .AddressChanged, userInfo: ["pointer": UInt32(pointer)])
                }
                DispatchQueue.main.async {
                    DropAlert().success(message: NSLocalizedString("id_new_transaction", comment: ""))
                }
            }
        case .TwoFactorReset:
            Task { try? await loadTwoFactorConfig() }
            self.post(event: .TwoFactorReset, userInfo: data)
        case .Settings:
            settings = Settings.from(data)
            post(event: .Settings, userInfo: data)
        case .Network:
            guard let connection = Connection.from(data) as? Connection else { return }
            // avoid handling notification for unlogged session
            guard connected && logged else { return }
            // notify disconnected network state
            if connection.currentState == "disconnected" {
                self.post(event: EventType.Network, userInfo: data)
                return
            }
            // Restore connection through hidden login
            //do {
                //try await reconnect()
            //    self.post(event: EventType.Network, userInfo: data)
            //}.catch { err in
                //print("Error on reconnected with hw: \(err.localizedDescription)")
                //let appDelegate = UIApplication.shared.delegate as? AppDelegate
                //appDelegate?.logout(with: false)
        case .Tor:
            post(event: .Tor, userInfo: data)
        case .Ticker:
            post(event: .Ticker, userInfo: data)
        default:
            break
        }
    }

    func post(event: EventType, object: Any? = nil, userInfo: [String: Any] = [:]) {
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: event.rawValue),
                                        object: object, userInfo: userInfo)
    }
}
