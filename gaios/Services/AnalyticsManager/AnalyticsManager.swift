import Foundation
import Countly
import gdk
import greenaddress

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
                           CLYConsent.performanceMonitoring,
                           CLYConsent.feedback]

    let deniedGroup = [CLYConsent.crashReporting,
                       CLYConsent.remoteConfig,
                       CLYConsent.metrics,
                       CLYConsent.feedback]

    // list of ignorable common error messages
    let skipExceptionRecording = [
        "id_invalid_amount",
        "id_no_amount_specified",
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
    
    var activeNetworks: [NetworkSecurityCase]? {
        let wm = WalletManager.current
        return wm?.activeNetworks
            .filter { net in !(wm?.subaccounts.filter { !$0.hidden && $0.networkType == net }.isEmpty ?? false) }
    }

    var analyticsNtw: AnalyticsManager.NtwTypeDescriptor? {
        if let activeNetworks = activeNetworks {
            let bitcoinNtws = activeNetworks.filter { $0 == .bitcoinSS || $0 == .bitcoinMS }
            let liquidNtws = activeNetworks.filter { $0 == .liquidSS || $0 == .liquidMS }
            let testnetNtws = activeNetworks.filter { $0 == .testnetSS || $0 == .testnetMS }
            let testnetLiquidNtws = activeNetworks.filter { $0 == .testnetLiquidSS || $0 == .testnetLiquidMS }

            if bitcoinNtws.count > 0 && liquidNtws.count > 0 { return AnalyticsManager.NtwTypeDescriptor.mainnetMixed }
            if bitcoinNtws.count > 0 { return AnalyticsManager.NtwTypeDescriptor.mainnet }
            if liquidNtws.count > 0 { return AnalyticsManager.NtwTypeDescriptor.liquid }
            if testnetNtws.count > 0 && testnetLiquidNtws.count > 0 { return AnalyticsManager.NtwTypeDescriptor.testnetMixed }
            if testnetNtws.count > 0 { return AnalyticsManager.NtwTypeDescriptor.testnet }
            if testnetLiquidNtws.count > 0 { return AnalyticsManager.NtwTypeDescriptor.testnetLiquid }
        }
        return nil
    }

    var analyticsSec: [SecTypeDescriptor]? {
        if let activeNetworks = activeNetworks {
            let hasSinglesig = activeNetworks.filter { [.bitcoinSS, .liquidSS, .testnetSS, .testnetLiquidSS].contains($0) }.count > 0
            let hasMultisig = activeNetworks.filter { [.bitcoinMS, .liquidMS, .testnetMS, .testnetLiquidMS].contains($0) }.count > 0
            let hasLightning = activeNetworks.filter { [.lightning, .testnetLightning].contains($0) }.count > 0
            var security = [SecTypeDescriptor]()
            if hasSinglesig {
                security += [hasMultisig || hasLightning ? .single : .singlesig]
            }
            if hasMultisig {
                security += [hasSinglesig || hasLightning ? .multi : .multisig]
            }
            if hasLightning {
                security += [hasSinglesig || hasMultisig ? .light : .lightning]
            }
            return security
        }
        return nil
    }

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

    public func setupSession(session: GDKSession?) {
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
        GdkSettings.read()?.tor ?? false ? hostOnion : host
    }

    private func getSessionConfiguration(session: Session?) -> URLSessionConfiguration {
        let configuration = URLSessionConfiguration.ephemeral
        let settings = GdkSettings.read()
        // set explicit proxy
        if settings?.proxy ?? false {
            configuration.connectionProxyDictionary = [
                kCFStreamPropertySOCKSProxyHost: settings?.socks5Hostname ?? "",
                kCFStreamPropertySOCKSProxyPort: settings?.socks5Port ?? ""
            ]
        }
        // set implicit tor proxy
        if settings?.tor ?? false {
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
        let accounts = AccountsRepository.shared.swAccounts

        let bitcoin_wallets = accounts.filter { !$0.gdkNetwork.liquid }
        let liquid_wallets = accounts.filter { $0.gdkNetwork.liquid }

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
            let exception = NSException(name: NSExceptionName(rawValue: msg), reason: msg)
//            guard consent == .authorized else { return }
//            Countly.sharedInstance().recordHandledException(exception)
            Countly.sharedInstance().record(exception,
                                            isFatal: false,
                                            stackTrace: Thread.callStackSymbols,
                                            segmentation:nil)
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

    func startTrace(_ key: AnalyticsEventName) {
        guard consent == .authorized else { return }
        Countly.sharedInstance().startCustomTrace(key.rawValue)
    }

    func endTrace(_ key: AnalyticsEventName) {
        guard consent == .authorized else { return }
        Countly.sharedInstance().endCustomTrace(key.rawValue, metrics: [:])
    }

    func cancelTrace(_ key: AnalyticsEventName) {
        guard consent == .authorized else { return }
        Countly.sharedInstance().cancelCustomTrace(key.rawValue)
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
