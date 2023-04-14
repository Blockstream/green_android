import Foundation

struct AppSettings: Codable {

    enum CodingKeys: String, CodingKey {
        case tor
        case proxy
        case socks5Hostname = "socks5_hostname"
        case socks5Port = "socks5_port"
        case spvEnabled = "spv_enabled"
        case personalNodeEnabled = "personal_node_enabled"
        case btcElectrumSrv = "btc_electrum_srv"
        case liquidElectrumSrv = "liquid_electrum_srv"
        case testnetElectrumSrv = "testnet_electrum_srv"
        case liquidTestnetElectrumSrv = "liquid_testnet_electrum_srv"
    }
    let tor: Bool?
    let proxy: Bool?
    let socks5Hostname: String?
    let socks5Port: String?
    let spvEnabled: Bool?
    let personalNodeEnabled: Bool?
    let btcElectrumSrv: String?
    let liquidElectrumSrv: String?
    let testnetElectrumSrv: String?
    let liquidTestnetElectrumSrv: String?

    var testnet: Bool {
        get {
            UserDefaults.standard.bool(forKey: AppSettings.testnetIsVisible) == true
        }
        set {
            UserDefaults.standard.set(newValue, forKey: AppSettings.testnetIsVisible)
        }
    }

    static let testnetIsVisible = "testnet_is_visible"
    static let btcElectrumSrvDefaultEndPoint = "blockstream.info:700"
    static let liquidElectrumSrvDefaultEndPoint = "blockstream.info:995"
    static let testnetElectrumSrvDefaultEndPoint = "blockstream.info:993"
    static let liquidTestnetElectrumSrvDefaultEndPoint = "blockstream.info:465"

    static func read() -> AppSettings? {
        let value = UserDefaults.standard.value(forKey: "network_settings") as? [String: Any] ?? [:]
        return AppSettings.from(value) as? AppSettings
    }

    func write() {
        let newValue = self.toDict()
        UserDefaults.standard.set(newValue, forKey: "network_settings")
        UserDefaults.standard.synchronize()
    }
}
