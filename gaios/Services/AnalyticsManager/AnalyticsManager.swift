import Foundation
import Countly

enum AnalyticsConsent: Int {
    case notDetermined
    case denied
    case authorized
}

protocol AnalyticsManagerDelegate: AnyObject {
    func remoteConfigIsReady()
}

class AnalyticsManager {

    static let shared = AnalyticsManager()

    let host = (Bundle.main.infoDictionary?["COUNTLY_APP_HOST"] as? String ?? "")
        .replacingOccurrences(of: "\\", with: "")
    let hostOnion = (Bundle.main.infoDictionary?["COUNTLY_APP_HOST_ONION"] as? String ?? "")
        .replacingOccurrences(of: "\\", with: "")
    let appKey = (Bundle.main.infoDictionary?["COUNTLY_APP_KEY"] as? String ?? "")
    var maxCountlyOffset: Int {
        if let offset = Bundle.main.infoDictionary?["COUNTLY_APP_MAX_OFFSET"] as? String,
           let number = Int(offset) {
            return number * 1000
        }
        return 1
    }
    var eventSendThreshold: UInt? {
        if let offset = Bundle.main.infoDictionary?["COUNTLY_APP_EVENT_SEND_THRESHOLD"] as? String {
            return UInt(offset)
        }
        return nil
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
                           CLYConsent.remoteConfig,
                           CLYConsent.metrics,
                           CLYConsent.feedback]

    let deniedGroup = [CLYConsent.crashReporting,
                       CLYConsent.remoteConfig,
                       CLYConsent.metrics,
                       CLYConsent.feedback]

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

    var countlyFeedbackWidget: CountlyFeedbackWidget?

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

    weak var delegate: AnalyticsManagerDelegate?

    func secureRandom(max: Int) -> UInt {
        // SystemRandomNumberGenerator is automatically seeded, is safe to use in multiple threads
        // and uses a cryptographically secure algorithm whenever possible.
        var gen = SystemRandomNumberGenerator()
        return UInt(Int.random(in: 1...max, using: &gen))
    }

    func countlyStart() {

        let config: CountlyConfig = CountlyConfig()
        config.appKey = appKey
        config.host = getHost()
        config.offset = countlyOffset
        config.deviceID = analyticsUUID
        config.features = [.crashReporting]
        config.enablePerformanceMonitoring = true
        config.enableDebug = true
        config.requiresConsent = true
        config.enableRemoteConfig = true
        if let threshold = eventSendThreshold {
            config.eventSendThreshold = threshold
        }
        config.urlSessionConfiguration = getSessionConfiguration(session: nil)

        if consent == .notDetermined {
            config.consents = deniedGroup
        }

        config.remoteConfigCompletionHandler = { error in
            if error == nil {
                print("Remote Config is ready to use!")
                self.delegate?.remoteConfigIsReady()
            } else {
                print("There was an error while fetching Remote Config:\n\(error!.localizedDescription)")
            }
        }
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

    public func setupSession(session: Session?) {
        let host = getHost()
        let conf = getSessionConfiguration(session: session)
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
            return hostOnion
        }
        return host
    }

    private func getSessionConfiguration(session: Session?) -> URLSessionConfiguration {
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
            let proxySettings = try? session?.getProxySettings()
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
        let accounts = AccountDao.shared.swAccounts

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

    func getSurvey(completion: @escaping (CountlyWidget?) -> Void) {
        guard consent != .notDetermined else {
            completion(nil)
            return
        }
        Countly.sharedInstance().getFeedbackWidgets({ [weak self] widgets, error in
            if error == nil, let widget = (widgets?.filter { $0.type == .NPS || $0.type == .survey })?.first {
                widget.getData { wData, error in

                    if error == nil, let data = wData {
                        let w = CountlyWidget.build(data)
                        self?.countlyFeedbackWidget = widget
                        completion(w)
                    } else {
                        completion(nil)
                    }
                }
            } else {
                completion(nil)
            }
        })
    }

    func submitSurvey(_ result: [AnyHashable: Any]) {
        guard let widget = countlyFeedbackWidget else { return }
        widget.recordResult(result)
    }

    func submitNPS(_ result: [AnyHashable: Any]) {
        guard let widget = countlyFeedbackWidget else { return }
        widget.recordResult(result)
    }

    func submitExclude() {
        guard let widget = countlyFeedbackWidget else { return }
        widget.recordResult(nil)
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

    func recordFeedback(rating: Int, email: String?, comment: String) {
        Countly.sharedInstance()
            .recordRatingWidget(withID: AnalyticsManager.ratingWidgetId,
                                rating: rating,
                                email: email,
                                comment: comment,
                                userCanBeContacted: true)
    }
}
