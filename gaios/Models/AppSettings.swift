import Foundation

struct AppSettings: Codable {

    static var shared = AppSettings()
    static let testnetIsVisible = "testnet_is_visible"
    static let experimental = "experimental"

    var testnet: Bool {
        get { UserDefaults.standard.bool(forKey: AppSettings.testnetIsVisible) == true }
        set { UserDefaults.standard.set(newValue, forKey: AppSettings.testnetIsVisible) }
    }

    var experimental: Bool {
        get { UserDefaults.standard.bool(forKey: AppSettings.experimental) == true }
        set { UserDefaults.standard.set(newValue, forKey: AppSettings.experimental) }
    }

    var gdkSettings: GdkSettings? {
        get { GdkSettings.read() }
        set { newValue?.write() }
    }

    func write() {
        let newValue = self.toDict()
        UserDefaults.standard.set(newValue, forKey: "network_settings")
        UserDefaults.standard.synchronize()
    }
    
    var lightningCodeOverride = false
    var lightningEnabled: Bool {
        let featureLightning = AnalyticsManager.shared.getRemoteConfigValue(key: "feature_lightning") as? Bool
        return featureLightning ?? false || lightningCodeOverride
    }
}
