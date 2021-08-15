import Foundation
import PromiseKit

class SessionManager: Session {

    static let shared = SessionManager()

    var account: Account?
    var connected = false

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
        let url = try! FileManager.default.url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true).appendingPathComponent(Bundle.main.bundleIdentifier!, isDirectory: true)
        try? FileManager.default.createDirectory(atPath: url.path, withIntermediateDirectories: true, attributes: nil)
        try! gdkInit(config: ["datadir": url.path])
        try! super.init(notificationCompletionHandler: NotificationManager.shared.newNotification)
    }

    public func connect(_ account: Account) throws {
        self.account = account
        disconnect()
        try connect(network: account.network)
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
        let netParams: [String: Any] = ["name": network, "use_tor": useTor, "proxy": proxyURI, "user_agent": userAgent]
        do {
            try super.connect(netParams: netParams)
            connected = true
        } catch {
            throw AuthenticationTypeHandler.AuthError.ConnectionFailed
        }
    }

    override func disconnect() {
        try? super.disconnect()
        account = nil
        connected = false
        Jade.shared.xPubsCached.removeAll()
        Ledger.shared.xPubsCached.removeAll()
        NotificationManager.shared.blockHeight = 0
        NotificationManager.shared.events = []
        NotificationManager.shared.twoFactorReset = nil
    }

    func transactions(first: UInt32 = 0) -> Promise<Transactions> {
        let bgq = DispatchQueue.global(qos: .background)
        let pointer = activeWallet
        return Guarantee().then(on: bgq) {_ in
            try SessionManager.shared.getTransactions(details: ["subaccount": pointer, "first": first, "count": 15]).resolve()
        }.compactMap(on: bgq) { data in
            let result = data["result"] as? [String: Any]
            let dict = result?["transactions"] as? [[String: Any]]
            let list = dict?.map { Transaction($0) }
            return Transactions(list: list ?? [])
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
}
