import Foundation
import PromiseKit

public enum LoginError: Error, Equatable {
    case walletsJustRestored
    case walletNotFound
    case invalidMnemonic
    case connectionFailed
}

class SessionManager: Session {

    var account: Account?
    var connected = false
    var logged = false
    var currentConnected = false
    var notificationManager: NotificationManager
    var twoFactorConfig: TwoFactorConfig?
    var settings: Settings?
    var registry: AssetsManager? {
        if let network = account?.gdkNetwork, network.liquid {
            return network.mainnet ? AssetsManager.liquid : AssetsManager.elements
        }
        return nil
    }

    var isResetActive: Bool? {
        get { twoFactorConfig?.twofactorReset.isResetActive }
    }

    var activeWallet: UInt32 {
        get {
            let pointerKey = String(format: "%@_wallet_pointer", self.account?.id ?? "")
            let pointer = UserDefaults.standard.integer(forKey: pointerKey)
            return UInt32(pointer)
        }
        set {
            let pointerKey = String(format: "%@_wallet_pointer", self.account?.id ?? "")
            UserDefaults.standard.set(Int(newValue), forKey: pointerKey)
            UserDefaults.standard.synchronize()
        }
    }

    public init(account: Account) {
        self.account = account
        notificationManager = NotificationManager(account: account)
        try! super.init()
    }

    deinit {
        setNotificationHandler(notificationCompletionHandler: nil)
    }

    public func destroy() {
        if let id = account?.id {
            SessionsManager.shared.removeValue(forKey: id)
        }
    }

    public func connect() throws {
        print("connect")
        if connected == false {
            try connect(network: self.account?.networkName ?? "mainnet")
        }
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
            setNotificationHandler(notificationCompletionHandler: notificationManager.newNotification)
            try super.connect(netParams: netParams)
            connected = true
            currentConnected = true
        } catch {
            throw LoginError.connectionFailed
        }
    }

    func transactions(first: UInt32 = 0) -> Promise<Transactions> {
        let bgq = DispatchQueue.global(qos: .background)
        let pointer = activeWallet
        return Guarantee().then(on: bgq) {_ in
            try self.getTransactions(details: ["subaccount": pointer, "first": first, "count": Constants.trxPerPage]).resolve()
        }.compactMap(on: bgq) { data in
            let result = data["result"] as? [String: Any]
            let dict = result?["transactions"] as? [[String: Any]]
            let list = dict?.map { Transaction($0) }
            return Transactions(list: list ?? [])
        }
    }

    func subaccount(_ pointer: UInt32? = nil) -> Promise<WalletItem> {
        let bgq = DispatchQueue.global(qos: .background)
        let pointer = pointer ?? activeWallet
        return Guarantee().then(on: bgq) {
            try self.getSubaccount(subaccount: pointer).resolve()
        }.recover {_ in
            return Guarantee().compactMap { [self] in
                activeWallet = 0
            }.then(on: bgq) {
                try self.getSubaccount(subaccount: 0).resolve()
            }
        }.compactMap(on: bgq) { data in
            let result = data["result"] as? [String: Any]
            let jsonData = try JSONSerialization.data(withJSONObject: result ?? [:])
            return try JSONDecoder().decode(WalletItem.self, from: jsonData)
        }
    }

    func subaccounts(_ refresh: Bool = false) -> Promise<[WalletItem]> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().then(on: bgq) {
            try self.getSubaccounts(details: ["refresh": refresh]).resolve()
        }.compactMap(on: bgq) { data in
            let result = data["result"] as? [String: Any]
            let subaccounts = result?["subaccounts"] as? [[String: Any]]
            let jsonData = try JSONSerialization.data(withJSONObject: subaccounts ?? [:])
            let wallets = try JSONDecoder().decode([WalletItem].self, from: jsonData)
            return wallets
        }
    }

    func loadTwoFactorConfig() -> Promise<TwoFactorConfig> {
       let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().compactMap(on: bgq) {
            try self.getTwoFactorConfig()
        }.compactMap { dataTwoFactorConfig in
            let twoFactorConfig = try JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig, options: []))
            self.twoFactorConfig = twoFactorConfig
            return twoFactorConfig
        }
    }

    func loadSettings() -> Promise<Settings> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().compactMap(on: bgq) {
            try self.getSettings()
        }.compactMap { data in
            self.settings = Settings.from(data)
            return self.settings
        }
    }

    func loadSystemMessage() -> Promise<String> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().map(on: bgq) {
            try self.getSystemMessage()
        }
    }

    func discover(mnemonic: String?, password: String?, hwDevice: HWDevice?) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        let isSingleSig = account?.isSingleSig ?? false
        return Guarantee()
            .map(on: bgq) { _ in
                if let mnemonic = mnemonic {
                    guard try gaios.validateMnemonic(mnemonic: mnemonic) else {
                        throw LoginError.invalidMnemonic
                    }
                }
            }.map(on: bgq) {
                try self.connect()
            }.then(on: bgq) { _ in
                try super.loginUser(details: ["mnemonic": mnemonic ?? "", "password": password ?? ""], hw_device: [:]).resolve()
            }.map(on: bgq) { res in
                // check if wallet just exist
                let result = res["result"] as? [String: Any]
                let walletHashId = result?["wallet_hash_id"] as? String
                if AccountsManager.shared.accounts.contains(where: {
                        $0.walletHashId == walletHashId &&
                        $0.id != self.account?.id &&
                        $0.isSingleSig == isSingleSig &&
                        $0.networkName == self.account?.networkName
                    }) {
                    throw LoginError.walletsJustRestored
                }
            }.recover { err in
                switch err {
                case TwoFactorCallError.failure(let localizedDescription):
                    if localizedDescription == "id_login_failed", !isSingleSig {
                        throw LoginError.walletNotFound
                    }
                default:
                    throw err
                }
            }.then(on: bgq) {
                // discover subaccounts
                self.subaccounts(true).recover { _ in
                    Promise { _ in
                        throw LoginError.connectionFailed
                    }
                }
            }.get(on: bgq) { wallets in
                // check account discover if singlesig
                if isSingleSig {
                    if wallets.filter({ $0.bip44Discovered ?? false }).isEmpty {
                        throw LoginError.walletNotFound
                    }
                }
           }.asVoid()
    }

    func restore(mnemonic: String?, password: String?, hwDevice: HWDevice? = nil) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        let isSingleSig = account?.isSingleSig ?? false
        return self.discover(mnemonic: mnemonic, password: password, hwDevice: hwDevice)
            .recover { err in
                switch err {
                case LoginError.walletNotFound:
                    if !isSingleSig {
                        throw err
                    }
                default:
                    throw err
                }
            }.then(on: bgq) { _ in
                self.login(details: ["mnemonic": mnemonic ?? "", "password": password ?? ""], hwDevice: hwDevice)
            }
    }

    func create(mnemonic: String? = nil, password: String? = nil, hwDevice: HWDevice? = nil) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .map(on: bgq) {
                try self.connect()
            }.then(on: bgq) { _ in
                self.registerUser(mnemonic: mnemonic, hwDevice: hwDevice)
            }.then(on: bgq) { _ in
                self.login(details: ["mnemonic": mnemonic ?? "", "password": password ?? ""], hwDevice: hwDevice)
            }.recover { err in
                switch err {
                case LoginError.walletNotFound,
                    LoginError.walletsJustRestored:
                    // Enable restore for HW
                    return
                default:
                    throw err
                }
            }
    }

    private func registerUser(mnemonic: String?, hwDevice: HWDevice? = nil) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .map(on: bgq) {
                if let hwDevice = hwDevice,
                    let data = try? JSONEncoder().encode(hwDevice),
                    let device = try? JSONSerialization.jsonObject(with: data, options: .allowFragments) {
                    return ["device": device]
                }
                return [:]
            }.then(on: bgq) { device in
                try super.registerUser(mnemonic: mnemonic ?? "", hw_device: device).resolve()
            }.asVoid()
    }

    func reconnect() -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .map(on: bgq) {
                try self.connect()
            }.then(on: bgq) {
                try super.loginUser(details: [:]).resolve().asVoid()
            }
    }

    func login(details: [String: Any], hwDevice: HWDevice? = nil) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        let isSingleSig = self.account?.isSingleSig ?? false
        let isWatchonly = self.account?.isWatchonly ?? false
        return Guarantee()
            .map(on: bgq) {
                try self.connect()
            }.compactMap(on: bgq) { _ in
                if let hwDevice = hwDevice,
                    let data = try? JSONEncoder().encode(hwDevice),
                    let device = try? JSONSerialization.jsonObject(with: data, options: .allowFragments) {
                    return ([:], ["device": device])
                }
                return (details, [:])
            }.then(on: bgq) { (details, device) in
                try super.loginUser(details: details, hw_device: device).resolve()
            }.compactMap { res in
                self.logged = true
                // update wallet hash id
                let result = res["result"] as? [String: Any]
                let walletHashId = result?["wallet_hash_id"] as? String
                var prevHashId = self.account?.walletHashId
                self.account?.walletHashId = walletHashId
                // for hw, use previously account if exist
                if hwDevice != nil {
                    let acc = AccountsManager.shared.accounts.filter {
                        ($0.walletHashId == walletHashId || $0.walletHashId == nil) &&
                        $0.name == self.account?.name &&
                        $0.isSingleSig == self.account?.isSingleSig &&
                        $0.id != self.account?.id &&
                        $0.isHW
                    }.first
                    if let acc = acc {
                        prevHashId = acc.walletHashId
                        self.account = acc
                        self.account?.walletHashId = walletHashId
                    }
                }
                return prevHashId == nil
            }.then(on: bgq) { firstInitialization in
                // discover subaccounts at 1st login
                self.subaccounts(firstInitialization).recover { _ in
                    Promise { _ in
                        throw LoginError.connectionFailed
                    }
                }
            }.then(on: bgq) { wallets -> Promise<Void> in
                // create a default segwit account if doesn't exist on singlesig
                if isSingleSig && !wallets.contains(where: {$0.type == AccountType.segWit.rawValue }) {
                    return try self.createSubaccount(details: ["name": "Segwit Account", "type": AccountType.segWit.rawValue])
                        .resolve().asVoid()
                }
                return Promise<Void>().asVoid()
            }.then { _ -> Promise<Void> in
                // load 2fa config on multisig
                if isWatchonly && !isSingleSig {
                    return self.loadTwoFactorConfig().asVoid()
                }
                return Promise<Void>()
            }.map { _ in
                // load async assets registry
                self.registry?.cache(session: self)
                self.registry?.loadAsync(session: self)
            }
    }
}
