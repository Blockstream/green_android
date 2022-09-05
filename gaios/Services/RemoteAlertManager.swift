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

    var remoteAlerts: [RemoteAlert] {
        if let banners: [Any] = AnalyticsManager.shared.getRemoteConfigValue(key: Constants.countlyRemoteConfigBanners) as? [Any] {
            return RemoteAlert.getList(banners)
        }
        return []
    }

    func getAlert(screen: AnalyticsViewName, network: String?) -> RemoteAlert? {

        var alerts: [RemoteAlert] = []
        remoteAlerts.forEach { item in
            if let screens = item.screens {
                if screens.contains("*") || screens.contains(screen.rawValue) {
                    alerts.append(item)
                }
            }
        }
        if let network = network {
            alerts = alerts.filter { item in
                if let networks = item.networks {
                    return networks.contains( network )
                }
                return false
            }
        } else {
            alerts = alerts.filter { item in
                if item.networks?.isEmpty ?? true {
                    return true
                }
                return false
            }
        }
        alerts.shuffle()
        return alerts.first
    }
}
