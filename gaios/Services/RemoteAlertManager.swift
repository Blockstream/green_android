import Foundation

struct RemoteAlert: Decodable {
    var dismissable: Bool?
    var isWarning: Bool?
    var link: String?
    var message: String?
    var networks: [String]?
    var screens: [String]?
    var title: String?

    enum CodingKeys: String, CodingKey {
        case dismissable
        case isWarning = "is_warning"
        case link
        case message
        case networks
        case screens
        case title
    }

    static func getList(_ banners: [Any]) -> [RemoteAlert] {
        let json = try? JSONSerialization.data(withJSONObject: banners, options: [])
        let alerts = try? JSONDecoder().decode([RemoteAlert].self, from: json ?? Data())
        return alerts ?? []
    }
}

class RemoteAlertManager {

    static let shared = RemoteAlertManager()

    var remoteAlerts: [RemoteAlert] = {
        if let banners: [Any] = AnalyticsManager.shared.getRemoteConfigValue(key: Constants.countlyRemoteConfigBanners) as? [Any] {
            return RemoteAlert.getList(banners)
        }
        return []
    }()

    func alerts(screen: AnalyticsViewName, networks: [NetworkSecurityCase]) -> [RemoteAlert] {
        let networkNames = networks.map { $0.network }
        return remoteAlerts
            .filter { $0.screens?.contains("*") ?? false || $0.screens?.contains(screen.rawValue) ?? false }
            .filter { networks.isEmpty || !($0.networks?.filter { networkNames.contains($0) }.isEmpty ?? true) }
    }
}
