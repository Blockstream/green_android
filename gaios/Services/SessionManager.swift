import Foundation
import PromiseKit

class SessionManager: Session {

    static var shared = SessionManager()

    var account: Account?
    var connected = false
    var logged = false
    var notificationManager: NotificationManager
    var twoFactorConfig: TwoFactorConfig?
    var settings: Settings?

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

    public init() {
        notificationManager = NotificationManager()
        try! super.init(notificationCompletionHandler: notificationManager.newNotification)
    }

    public static func newSession(account: Account?) -> SessionManager {
        // Todo: destroy the session in a thread-safe way
        //SessionManager.shared = SessionManager()
        let session = SessionManager.shared
        try? session.disconnect()
        session.account = account
        session.connected = false
        session.logged = false
        session.twoFactorConfig = nil
        session.settings = nil
        return SessionManager.shared
    }

    public func connect() throws {
        if connected == false {
            try connect(network: self.account?.networkName ?? "mainnet")
        }
    }

    public func connect(network: String, params: [String: Any]? = nil) throws {
        let networkSettings = params ?? getUserNetworkSettings()
        let useProxy = networkSettings["proxy"] as? Bool ?? false
        let socks5Hostname = useProxy ? networkSettings["socks5_hostname"] as? String ?? "" : ""
        let socks5Port = useProxy ? networkSettings["socks5_port"] as? String ?? "" : ""
        let useTor = getGdkNetwork(network).serverType == "green" ? networkSettings["tor"] as? Bool ?? false : false
        let proxyURI = useProxy ? String(format: "socks5://%@:%@/", socks5Hostname, socks5Port) : ""
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? CVarArg ?? ""
        let userAgent = String(format: "green_ios_%@", version)
        var netParams: [String: Any] = ["name": network, "use_tor": useTor, "proxy": proxyURI, "user_agent": userAgent]
        #if DEBUG
        netParams["log_level"] = "debug"
        #endif

        // SPV available only for btc singlesig
        if let spvEnabled = networkSettings[Constants.spvEnabled] as? Bool,
           network == Constants.electrumPrefix + "mainnet" || network == Constants.electrumPrefix + "testnet" {
            netParams["spv_enabled"] = spvEnabled
        }
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
            }
        }
        // Connect
        do {
            try super.connect(netParams: netParams)
            connected = true
        } catch {
            throw AuthenticationTypeHandler.AuthError.ConnectionFailed
        }
    }

    func transactions(first: UInt32 = 0) -> Promise<Transactions> {
        let bgq = DispatchQueue.global(qos: .background)
        let pointer = activeWallet
        return Guarantee().then(on: bgq) {_ in
            try SessionManager.shared.getTransactions(details: ["subaccount": pointer, "first": first, "count": Constants.trxPerPage]).resolve()
        }.compactMap(on: bgq) { data in
            let result = data["result"] as? [String: Any]
            let dict = result?["transactions"] as? [[String: Any]]
            let list = dict?.map { Transaction($0) }
            return Transactions(list: list ?? [])
        }
    }

    func hasTransactions(pointer: UInt32, first: UInt32 = 0) -> Promise<Bool> {
        let bgq = DispatchQueue.global(qos: .background)
        let pointer = pointer
        return Guarantee().then(on: bgq) {_ in
            try SessionManager.shared.getTransactions(details: ["subaccount": pointer, "first": first, "count": Constants.trxPerPage]).resolve()
        }.compactMap(on: bgq) { data in
            let result = data["result"] as? [String: Any]
            let dict = result?["transactions"] as? [[String: Any]]
            let list = dict?.map { Transaction($0) }
            return list?.count ?? 0 > 0
        }
    }

    func subaccount() -> Promise<WalletItem> {
        let bgq = DispatchQueue.global(qos: .background)
        let pointer = activeWallet
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

    func subaccounts() -> Promise<[WalletItem]> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().then(on: bgq) {
            try self.getSubaccounts().resolve()
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
            try SessionManager.shared.getTwoFactorConfig()
        }.compactMap { dataTwoFactorConfig in
            let twoFactorConfig = try JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig, options: []))
            self.twoFactorConfig = twoFactorConfig
            return twoFactorConfig
        }
    }

    func loadSettings() -> Promise<Settings> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().compactMap(on: bgq) {
            try SessionManager.shared.getSettings()
        }.compactMap { data in
            self.settings = Settings.from(data)
            return self.settings
        }
    }

    func loadSystemMessage() -> Promise<String> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().map(on: bgq) {
            try SessionManager.shared.getSystemMessage()
        }
    }

    func registerLogin(mnemonic: String? = nil, password: String? = nil, hwDevice: HWDevice? = nil)-> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .map(on: bgq) {
                try self.connect()
            }.map(on: bgq) {
                if let hwDevice = hwDevice,
                    let data = try? JSONEncoder().encode(hwDevice),
                    let device = try? JSONSerialization.jsonObject(with: data, options: .allowFragments) {
                    return ["device": device]
                }
                return [:]
            }.then(on: bgq) { device in
                try super.registerUser(mnemonic: mnemonic ?? "", hw_device: device).resolve()
            }.then(on: bgq) { _ in
                self.login(details: mnemonic != nil ? ["mnemonic": mnemonic ?? "", "password": password ?? ""] : [:],
                      hwDevice: hwDevice)
            }
    }

    func login(details: [String: Any], hwDevice: HWDevice? = nil)-> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .map(on: bgq) {
                try self.connect()
            }.map(on: bgq) {
                if let hwDevice = hwDevice,
                    let data = try? JSONEncoder().encode(hwDevice),
                    let device = try? JSONSerialization.jsonObject(with: data, options: .allowFragments) {
                    return ["device": device]
                }
                return [:]
            }.then(on: bgq) { device in
                try super.loginUser(details: details, hw_device: device).resolve()
            }.then { _ -> Promise<Void> in
                self.logged = true
                if let account = self.account,
                        !account.isWatchonly && !(account.isSingleSig ?? false) {
                    return self.loadTwoFactorConfig().then { _ in Promise<Void>() }
                }
                return Promise<Void>()
            }.then { _ -> Promise<Void> in
                if self.account?.network == "liquid" {
                    return Registry.shared.load()
                }
                return Promise<Void>()
            }
    }
}
