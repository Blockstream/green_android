import Foundation
import PromiseKit

class SessionManager: Session {

    static let shared = SessionManager()
    var twoFactorReset: TwoFactorReset?
    var events = [Event]()
    var account: Account?
    var blockHeight: UInt32 = 0
    var connected = false

    public init() {
        let url = try! FileManager.default.url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true).appendingPathComponent(Bundle.main.bundleIdentifier!, isDirectory: true)
        try? FileManager.default.createDirectory(atPath: url.path, withIntermediateDirectories: true, attributes: nil)
        try! gdkInit(config: ["datadir": url.path])
        try! super.init(notificationCompletionHandler: SessionManager.shared.newNotification)
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
        twoFactorReset = nil
        account = nil
        events = [Event]()
        blockHeight = 0
        connected = false
        Jade.shared.xPubsCached.removeAll()
        Ledger.shared.xPubsCached.removeAll()
    }
}
extension SessionManager {
    // Handle notification system

    private func newNotification(notification: [String: Any]?) {
        guard let notificationEvent = notification?["event"] as? String,
                let event = EventType(rawValue: notificationEvent),
                let data = notification?[event.rawValue] as? [String: Any] else {
            return
        }
        switch event {
        case .Block:
            guard let height = data["block_height"] as? UInt32 else { break }
            blockHeight = height
            post(event: .Block, data: data)
        case .Transaction:
            post(event: .Transaction, data: data)
            do {
                let json = try JSONSerialization.data(withJSONObject: data, options: [])
                let txEvent = try JSONDecoder().decode(TransactionEvent.self, from: json)
                events.append(Event(value: data))
                if txEvent.type == "incoming" {
                    txEvent.subAccounts.forEach { pointer in
                        post(event: .AddressChanged, data: ["pointer": UInt32(pointer)])
                    }
                    DispatchQueue.main.async {
                        DropAlert().success(message: NSLocalizedString("id_new_transaction", comment: ""))
                    }
                }
            } catch { break }
        case .TwoFactorReset:
            do {
                let json = try JSONSerialization.data(withJSONObject: data, options: [])
                self.twoFactorReset = try JSONDecoder().decode(TwoFactorReset.self, from: json)
                events.removeAll(where: { $0.kindOf(TwoFactorReset.self)})
                if self.twoFactorReset!.isResetActive {
                    events.append(Event(value: data))
                }
            } catch { break }
            post(event: .TwoFactorReset, data: data)
        case .Settings:
            reloadSystemMessage()
            Settings.shared = Settings.from(data)
            if let acc = account, !acc.isWatchonly {
                reloadTwoFactor()
            }
            post(event: .Settings, data: data)
        case .Session:
            post(event: EventType.Network, data: data)
        case .Network:
            guard let json = try? JSONSerialization.data(withJSONObject: data, options: []),
                  let connection = try? JSONDecoder().decode(Connection.self, from: json) else {
                return
            }
            if !connection.connected || !(connection.loginRequired ?? false) {
                post(event: EventType.Network, data: data)
                return
            }
            let hw: HWProtocol = account?.isLedger ?? false ? Ledger.shared : Jade.shared
            if !(AccountsManager.shared.current?.isHW ?? false) {
                // Login required without hw
                DispatchQueue.main.async {
                    let appDelegate = UIApplication.shared.delegate as? AppDelegate
                    appDelegate?.logout(with: false)
                }
                return
            }
            if hw.connected {
                // Restore connection with hw through hidden login
                reconnect().done { _ in
                    self.post(event: EventType.Network, data: data)
                }.catch { err in
                    print("Error on reconnected with hw: \(err.localizedDescription)")
                    let appDelegate = UIApplication.shared.delegate as? AppDelegate
                    appDelegate?.logout(with: false)
                }
            }
        case .Tor:
            post(event: .Tor, data: data)
        default:
            break
        }
    }

    func post(event: EventType, data: [String: Any]) {
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: event.rawValue),
                                        object: nil, userInfo: data)
    }

    func reloadTwoFactor() {
        events.removeAll(where: { $0.kindOf(Settings.self)})
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map(on: bgq) {
            try self.getTwoFactorConfig()
        }.done { dataTwoFactorConfig in
            if dataTwoFactorConfig != nil {
                let twoFactorConfig = try JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig!, options: []))
                let data = try JSONSerialization.jsonObject(with: JSONEncoder().encode(Settings.shared), options: .allowFragments) as? [String: Any]
                if twoFactorConfig.enableMethods.count <= 1 {
                    self.events.append(Event(value: data!))
                }
            }
        }.catch { _ in
            print("Error on get settings")
        }
    }

    func reloadSystemMessage() {
        events.removeAll(where: { $0.kindOf(SystemMessage.self)})
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map(on: bgq) {
            try self.getSystemMessage()
        }.done { text in
            if !text.isEmpty {
                self.events.append(Event(value: ["text": text]))
            }
        }.catch { _ in
            print("Error on get system message")
        }
    }

    func reconnect() -> Promise<[String: Any]> {
        let bgq = DispatchQueue.global(qos: .background)
        let session = SessionManager.shared
        return Guarantee().map(on: bgq) {_ -> TwoFactorCall in
            let info = AccountsManager.shared.current?.isLedger ?? false ? Ledger.shared.device : Jade.shared.device
            guard let data = try? JSONEncoder().encode(info),
                let device = try? JSONSerialization.jsonObject(with: data, options: .allowFragments) else {
                throw JadeError.Abort("Invalid device configuration")
            }
            return try session.loginUser(details: [:], hw_device: ["device": device])
        }.then(on: bgq) { call in
            call.resolve()
        }
    }
}
