import Foundation

class TorSessionManager {
    static let shared = TorSessionManager()
    private var connected = false
    private var session: Session?

    var enabled: Bool {
        let networkSettings = getUserNetworkSettings()
        return networkSettings["tor"] as? Bool ?? false
    }

    var proxySettings: [String: Any]? {
        return try? session?.getProxySettings()
    }

    func connect() {
        let networkSettings = getUserNetworkSettings()
        let useProxy = networkSettings["proxy"] as? Bool ?? false
        let socks5Hostname = useProxy ? networkSettings["socks5_hostname"] as? String ?? "" : ""
        let socks5Port = useProxy ? networkSettings["socks5_port"] as? String ?? "" : ""
        let proxyURI = useProxy ? String(format: "socks5://%@:%@/", socks5Hostname, socks5Port) : ""
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? CVarArg ?? ""
        let userAgent = String(format: "green_ios_%@", version)
        let network = Constants.electrumPrefix + "mainnet"
        let netParams = ["name": network, "use_tor": true, "proxy": proxyURI, "user_agent": userAgent] as [String: Any]
        do {
            session = nil
            session = try? Session()
            try session?.connect(netParams: netParams)
            connected = true
        } catch {
            print(error.localizedDescription)
        }
    }

    func disconnect() {
        try? session?.reconnectHint(hint: ["tor_hint": "disconnect", "hint": "disconnect"])
        connected = false
        session = nil
    }

    func resume() {
        if !enabled {
            return
        }
        if connected {
            try? session?.reconnectHint(hint: ["tor_hint": "connect", "hint": "connect"])
        } else {
            connect()
        }
    }

    func pause() {
        if !enabled {
            return
        }
        if connected {
            try? session?.reconnectHint(hint: ["tor_hint": "disconnect", "hint": "disconnect"])
        }
    }

}
