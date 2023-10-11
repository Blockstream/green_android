import Foundation
import gdk

struct GdkSettings: Codable {

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

    static let btcElectrumSrvDefaultEndPoint = "blockstream.info:700"
    static let liquidElectrumSrvDefaultEndPoint = "blockstream.info:995"
    static let testnetElectrumSrvDefaultEndPoint = "blockstream.info:993"
    static let liquidTestnetElectrumSrvDefaultEndPoint = "blockstream.info:465"

    static func read() -> GdkSettings? {
        let value = UserDefaults.standard.value(forKey: "network_settings") as? [String: Any] ?? [:]
        return GdkSettings.from(value) as? GdkSettings
    }

    func write() {
        let newValue = self.toDict()
        UserDefaults.standard.set(newValue, forKey: "network_settings")
        UserDefaults.standard.synchronize()
    }
    
    func toNetworkParams(_ network: String) -> NetworkSettings {
        let gdkSettings = GdkSettings.read()
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? CVarArg ?? ""
        let proxyURI = String(format: "socks5://%@:%@/", gdkSettings?.socks5Hostname ?? "", gdkSettings?.socks5Port ?? "")
        let gdkNetwork = GdkNetworks.shared.get(network: network)
        
        let electrumUrl: String? = {
            if let srv = gdkSettings?.btcElectrumSrv, gdkNetwork.mainnet && !gdkNetwork.liquid && !srv.isEmpty {
                return srv
            } else if let srv = gdkSettings?.testnetElectrumSrv, !gdkNetwork.mainnet && !gdkNetwork.liquid && !srv.isEmpty {
                return srv
            } else if let srv = gdkSettings?.liquidElectrumSrv, gdkNetwork.mainnet && gdkNetwork.liquid && !srv.isEmpty {
                return srv
            } else if let srv = gdkSettings?.liquidTestnetElectrumSrv, !gdkNetwork.mainnet && gdkNetwork.liquid && !srv.isEmpty {
                return srv
            } else {
                return nil
            }
        }()
    
        return NetworkSettings(
            name: network,
            useTor: gdkSettings?.tor ?? false,
            proxy: (gdkSettings?.proxy ?? false) ? proxyURI : nil,
            userAgent: String(format: "green_ios_%@", version),
            spvEnabled: (gdkSettings?.spvEnabled ?? false) && !gdkNetwork.liquid,
            electrumUrl: (gdkSettings?.personalNodeEnabled ?? false) ? electrumUrl : nil,
            electrumOnionUrl: (gdkSettings?.personalNodeEnabled ?? false && gdkSettings?.tor ?? false) ? electrumUrl : nil)
    }
}
