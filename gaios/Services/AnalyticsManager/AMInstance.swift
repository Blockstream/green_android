import Foundation
import Countly

class AnalyticsManager {

    private static var instance: AnalyticsManager?
    static var shared: AnalyticsManager {
        guard let instance = instance else {
            self.instance = AnalyticsManager()
            return self.instance!
        }
        return instance
    }

    init() {
        let config: CountlyConfig = CountlyConfig()
        config.appKey = AnalyticsManager.appKey
        config.host = AnalyticsManager.host
        config.features = [.crashReporting, .autoViewTracking]
        config.enablePerformanceMonitoring = true
        config.enableDebug = true
        config.requiresConsent = true

        Countly.sharedInstance().giveConsent(forFeatures: [
            CLYConsent.sessions,
            CLYConsent.events,
            CLYConsent.crashReporting,
            CLYConsent.viewTracking
        ])
        Countly.sharedInstance().start(with: config)
    }

    func appLoadingFinished() {
        Countly.sharedInstance().appLoadingFinished()
    }

    func recordEvent(_ key: AnalyticsEventKey, segmentation: [String: String]) {
        Countly.sharedInstance().recordEvent(key.rawValue, segmentation: segmentation, count: 1, sum: 0.0)
    }

    func cancelEvent(_ key: AnalyticsEventKey) {
        Countly.sharedInstance().cancelEvent(key.rawValue)
    }

    func startEvent(_ key: AnalyticsEventKey) {
        Countly.sharedInstance().startEvent(key.rawValue)
    }

    func endEvent(_ key: AnalyticsEventKey, segmentation: [String: String]) {
        Countly.sharedInstance().endEvent(key.rawValue, segmentation: segmentation, count: 1, sum: 0.0)
    }
}
