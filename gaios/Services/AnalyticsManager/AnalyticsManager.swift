import Foundation
import Countly

enum AnalyticsConsent: Int {
    case notDetermined
    case denied
    case authorized
}

class AnalyticsManager {

    static let shared = AnalyticsManager()

    var isProduction: Bool {
#if DEBUG
        return false
#else
        return true
#endif
    }

    var maxCountlyOffset: Int {
#if DEBUG
        return AnalyticsManager.maxOffsetDevelopment
#else
        return AnalyticsManager.maxOffsetProduction
#endif
    }

    var consent: AnalyticsConsent {
        get {
            return AnalyticsConsent(rawValue: UserDefaults.standard.integer(forKey: AppStorage.userAnalyticsPreference)) ?? .notDetermined
        }
        set {
            let prev = AnalyticsConsent(rawValue: UserDefaults.standard.integer(forKey: AppStorage.userAnalyticsPreference)) ?? .notDetermined
            UserDefaults.standard.set(newValue.rawValue, forKey: AppStorage.userAnalyticsPreference)

            giveConsent(previous: prev)
        }
    }

    var analyticsUUID: String {
        get {
            if let uuid = UserDefaults.standard.string(forKey: AppStorage.analyticsUUID) {
                print("analyticsUUID \(uuid)")
                return uuid
            } else {
                let uuid = UUID().uuidString
                print("analyticsUUID \(uuid)")
                UserDefaults.standard.setValue(uuid, forKey: AppStorage.analyticsUUID)
                return uuid
            }
        }
    }

    let authorizedGroup = [CLYConsent.sessions,
                           CLYConsent.events,
                           CLYConsent.crashReporting,
                           CLYConsent.viewTracking,
                           CLYConsent.userDetails,
                           CLYConsent.location,
                           CLYConsent.remoteConfig]
    let deniedGroup = [CLYConsent.crashReporting, CLYConsent.remoteConfig]

    // list of ignorable common error messages
    let skipExceptionRecording = [
        "id_invalid_amount",
        "id_invalid_address",
        "id_insufficient_funds",
        "id_invalid_private_key",
        "id_action_canceled",
        "id_login_failed"
    ]
    // not ignorable exception counter
    var exceptionCounter = 0

    func invalidateAnalyticsUUID() {
        UserDefaults.standard.removeObject(forKey: AppStorage.analyticsUUID)
    }

    func invalidateCountlyOffset() {
        UserDefaults.standard.removeObject(forKey: AppStorage.countlyOffset)
    }

    var countlyOffset: UInt {
        get {
            if let offset = UserDefaults.standard.object(forKey: AppStorage.countlyOffset) as? UInt {
                print("analyticsOFFSET \(offset)")
                return offset
            } else {
                let offset = secureRandom(max: maxCountlyOffset)
                print("analyticsOFFSET \(offset)")
                UserDefaults.standard.setValue(offset, forKey: AppStorage.countlyOffset)
                return offset
            }
        }
    }

    func secureRandom(max: Int) -> UInt {
        // SystemRandomNumberGenerator is automatically seeded, is safe to use in multiple threads
        // and uses a cryptographically secure algorithm whenever possible.
        var gen = SystemRandomNumberGenerator()
        return UInt(Int.random(in: 1...max, using: &gen))
    }

    func countlyStart() {

        let config: CountlyConfig = CountlyConfig()

        if isProduction {
            config.appKey = AnalyticsManager.appKeyProd
            config.host = getHost()
        } else {
            config.appKey = AnalyticsManager.appKeyDev
            config.host = getHost()
        }

        config.offset = countlyOffset
        config.deviceID = analyticsUUID
        config.features = [.crashReporting]
        config.enablePerformanceMonitoring = true
        config.enableDebug = true
        config.requiresConsent = true
        config.enableRemoteConfig = true
        if isProduction == false {
            config.eventSendThreshold = 1
        }
        config.urlSessionConfiguration = getSessionConfiguration()

        Countly.sharedInstance().start(with: config)

        giveConsent(previous: consent)
    }

    private func giveConsent(previous: AnalyticsConsent) {

        print("giving consent: \(consent)")

        switch consent {
        case .notDetermined:
            break
        case .denied:
            if previous == .authorized {
                Countly.sharedInstance().cancelConsentForAllFeatures()
                // change the deviceID
                invalidateAnalyticsUUID()
                invalidateCountlyOffset()
                Countly.sharedInstance().setNewDeviceID(analyticsUUID, onServer: false)
                Countly.sharedInstance().setNewOffset(countlyOffset)
            }
            Countly.sharedInstance().giveConsent(forFeatures: deniedGroup)
            Countly.sharedInstance().disableLocationInfo()
            updateUserProperties()
        case .authorized:
            Countly.sharedInstance().giveConsent(forFeatures: authorizedGroup)
            updateUserProperties()
        }
    }

    public func setupSession() {
        let host = getHost()
        let conf = getSessionConfiguration()
        Countly.sharedInstance().setNewHost(host)
        Countly.sharedInstance().setNewURLSessionConfiguration(conf)
        /*URLSession(configuration: conf).dataTask(with: URL(string: host+"/i")!) {
                data, response, error in
                print (data)
                print (response)
                print (error)
        }.resume()*/
    }

    private func getHost() -> String {
        let networkSettings = getUserNetworkSettings()
        if let tor = networkSettings["tor"] as? Bool, tor {
            return AnalyticsManager.onionHost
        }
        return AnalyticsManager.host
    }

    private func getSessionConfiguration() -> URLSessionConfiguration {
        let configuration = URLSessionConfiguration.ephemeral
        let networkSettings = getUserNetworkSettings()
        let useProxy = networkSettings["proxy"] as? Bool ?? false
        let useTor = networkSettings["tor"] as? Bool ?? false
        // set explicit proxy
        if useProxy {
            let socks5Hostname = networkSettings["socks5_hostname"] as? String
            let socks5Port = networkSettings["socks5_port"] as? String
            configuration.connectionProxyDictionary = [
                kCFStreamPropertySOCKSProxyHost: socks5Hostname ?? "",
                kCFStreamPropertySOCKSProxyPort: socks5Port ?? ""
            ]
        }
        // set implicit tor proxy
        if useTor {
            let proxySettings = TorSessionManager.shared.proxySettings
            let proxy = proxySettings?["proxy"] as? String ?? ""
            let parser = proxy.split(separator: ":").map { $0.replacingOccurrences(of: "/", with: "") }
            if parser.first == "socks5" && parser.count == 3 {
                configuration.connectionProxyDictionary = [
                    kCFStreamPropertySOCKSProxyHost: parser[1],
                    kCFStreamPropertySOCKSProxyPort: Int(parser[2]) ?? 0,
                    kCFProxyTypeKey: kCFProxyTypeSOCKS
                ]
            }
        }
        return configuration
    }

    private func updateUserProperties() {
        let accounts = AccountsManager.shared.swAccounts

        let bitcoin_wallets = accounts.filter { $0.network == "mainnet"}
        let liquid_wallets = accounts.filter { $0.network == "liquid"}

        var props: [String: String] = [:]
        props[AnalyticsManager.strUserPropertyTotalWallets] = "\((bitcoin_wallets + liquid_wallets).count)"

        Countly.user().custom = props as CountlyUserDetailsNullableDictionary
        Countly.user().save()
    }

    func appLoadingFinished() {
        guard consent != .notDetermined else { return }
        Countly.sharedInstance().appLoadingFinished()
    }

    func userPropertiesDidChange() {
        guard consent != .notDetermined else { return }
        updateUserProperties()
    }

    func recordException(_ msg: String) {
        if !msg.isEmpty && !skipExceptionRecording.contains(msg) {
            exceptionCounter += 1
            let exception = NSException(name: NSExceptionName(rawValue: msg), reason: "")
            guard consent == .authorized else { return }
            Countly.sharedInstance().recordHandledException(exception)
        }
    }

    func recordEvent(_ key: AnalyticsEventName) {
        guard consent == .authorized else { return }
        Countly.sharedInstance().recordEvent(key.rawValue)
    }

    func recordEvent(_ key: AnalyticsEventName, sgmt: [String: String]) {
        guard consent == .authorized else { return }
        Countly.sharedInstance().recordEvent(key.rawValue, segmentation: sgmt, count: 1, sum: 0.0)
    }

    func cancelEvent(_ key: AnalyticsEventName) {
        guard consent == .authorized else { return }
        Countly.sharedInstance().cancelEvent(key.rawValue)
    }

    func startEvent(_ key: AnalyticsEventName) {
        guard consent == .authorized else { return }
        Countly.sharedInstance().startEvent(key.rawValue)
    }

    func endEvent(_ key: AnalyticsEventName, sgmt: [String: String]) {
        guard consent == .authorized else { return }
        Countly.sharedInstance().endEvent(key.rawValue, segmentation: sgmt, count: 1, sum: 0.0)
    }

    func recordView(_ name: AnalyticsViewName) {
        guard consent == .authorized else { return }
        Countly.sharedInstance().recordView(name.rawValue)
    }

    func recordView(_ name: AnalyticsViewName, sgmt: [String: String]?) {
        guard consent == .authorized else { return }
        guard let s = sgmt else { return }
        Countly.sharedInstance().recordView(name.rawValue, segmentation: s)
    }

    func getRemoteConfigValue(key: String) -> Any? {

        let value = Countly.sharedInstance().remoteConfigValue(forKey: key)
        print("Remote config value: \(value)")
        return value
    }
}
