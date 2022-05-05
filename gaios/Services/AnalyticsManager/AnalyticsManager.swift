import Foundation
import Countly

class AMan { // AnalyticsManager

    private static var instance: AMan?
    static var S: AMan {
        guard let instance = instance else {
            self.instance = AMan()
            return self.instance!
        }
        return instance
    }

    init() {
        let config: CountlyConfig = CountlyConfig()

        // handle to switch to production
        config.appKey = AMan.appKeyDev
        config.host = AMan.host
        config.features = [.crashReporting]
        config.enablePerformanceMonitoring = true
        config.enableDebug = true
        config.requiresConsent = true

        Countly.sharedInstance().start(with: config)
    }

    func updateConsent() {
        Countly.sharedInstance().giveConsent(forFeatures: [
            CLYConsent.sessions,
            CLYConsent.events,
            CLYConsent.crashReporting,
            CLYConsent.viewTracking
        ])
    }

    func appLoadingFinished() {
        Countly.sharedInstance().appLoadingFinished()
    }

    func recordEvent(_ key: AnalyticsEventName) {
        Countly.sharedInstance().recordEvent(key.rawValue)
    }

    func recordEvent(_ key: AnalyticsEventName, sgmt: [String: String]) {
        Countly.sharedInstance().recordEvent(key.rawValue, segmentation: sgmt, count: 1, sum: 0.0)
    }

    func cancelEvent(_ key: AnalyticsEventName) {
        Countly.sharedInstance().cancelEvent(key.rawValue)
    }

    func startEvent(_ key: AnalyticsEventName) {
        Countly.sharedInstance().startEvent(key.rawValue)
    }

    func endEvent(_ key: AnalyticsEventName, sgmt: [String: String]) {
        Countly.sharedInstance().endEvent(key.rawValue, segmentation: sgmt, count: 1, sum: 0.0)
    }

    func recordView(_ name: AnalyticsViewName) {
        Countly.sharedInstance().recordView(name.rawValue)
    }

    func recordView(_ name: AnalyticsViewName, sgmt: [String: String]?) {
        guard let s = sgmt else { return }
        Countly.sharedInstance().recordView(name.rawValue, segmentation: s)
    }
}
