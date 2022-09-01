import Foundation

struct RemoteAlert {
    var dismissable: Bool?
    var isWarning: Bool?
    var link: String?
    var message: String?
    var networks: [String]?
    var screens: [String]?
    var title: String?

    init(_ dic: [String: Any]) {

        let dismissable: Bool? = dic["dismissable"] as? Bool
        let isWarning: Bool? = dic["is_warning"] as? Bool
        let message: String? = dic["message"] as? String
        let networks: [String]? = dic["networks"] as? [String]
        let screens: [String]? = dic["screens"] as? [String]
        let title: String? = dic["title"] as? String
        let link: String? = dic["link"] as? String

        self.dismissable = dismissable
        self.isWarning = isWarning
        self.message = message
        self.networks = networks
        self.screens = screens
        self.title = title
        self.link = link
    }

    static func decode(_ banners: [Any]) -> [RemoteAlert] {
        var remoteAlerts: [RemoteAlert] = []
        banners.forEach { item in
            if let dic = item as? [String: Any] {
                remoteAlerts.append(RemoteAlert(dic))
            }
        }
        return remoteAlerts
    }
}

class RemoteAlertManager {

    static let shared = RemoteAlertManager()

    var remoteAlerts: [RemoteAlert] {
        if let banners: [Any] = AnalyticsManager.shared.getRemoteConfigValue(key: Constants.countlyRemoteConfigBanners) as? [Any] {
            return RemoteAlert.decode(banners)
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
                return true
            }
        }
        alerts.shuffle()
        return alerts.first
    }
}
