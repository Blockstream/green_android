import Foundation
import Countly

enum AnalyticsConsent: Int {
    case notDetermined
    case denied
    case authorized
}

class AMan { // AnalyticsManager

    private static var instance: AMan?
    static var S: AMan {
        guard let instance = instance else {
            self.instance = AMan()
            return self.instance!
        }
        return instance
    }

    var isProduction: Bool {
#if DEBUG
        return false
#else
        return true
#endif
    }

    var consent: AnalyticsConsent {
        get {
            return AnalyticsConsent(rawValue: UserDefaults.standard.integer(forKey: AppStorage.userAnalyticsPreference)) ?? .notDetermined
        }
        set {
            let prev = AnalyticsConsent(rawValue: UserDefaults.standard.integer(forKey: AppStorage.userAnalyticsPreference)) ?? .notDetermined
            UserDefaults.standard.set(newValue.rawValue, forKey: AppStorage.userAnalyticsPreference)
            if prev == .notDetermined {
                //only once
                countlyStart()
            } else {
                giveConsent()
            }
        }
    }

    let authorizedGroup = [CLYConsent.sessions,
                           CLYConsent.events,
                           CLYConsent.crashReporting,
                           CLYConsent.viewTracking,
                           CLYConsent.userDetails]
    let deniedGroup = [CLYConsent.crashReporting]

    func countlyStart() {
        guard consent != .notDetermined else {
            print("SKIP countly init, wait user to allow/deny")
            return
        }

        let config: CountlyConfig = CountlyConfig()

        if isProduction {
            config.appKey = AMan.appKeyDev // USE PROD KEY WHEN READY!
            config.host = AMan.host
        } else {
            config.appKey = AMan.appKeyDev
            config.host = AMan.host
        }

        config.features = [.crashReporting]
        config.enablePerformanceMonitoring = true
        config.enableDebug = true
        config.requiresConsent = true

        Countly.sharedInstance().start(with: config)
        Countly.sharedInstance().disableLocationInfo()

        giveConsent()
    }

    private func giveConsent() {

        print("giving consent: \(consent)")

        switch consent {
        case .notDetermined:
            break
        case .denied:
            Countly.sharedInstance().cancelConsentForAllFeatures()
            Countly.sharedInstance().giveConsent(forFeatures: deniedGroup)
            updateUserProperties()
        case .authorized:
            Countly.sharedInstance().cancelConsentForAllFeatures()
            Countly.sharedInstance().giveConsent(forFeatures: authorizedGroup)
            updateUserProperties()
        }
    }

    private func updateUserProperties() {
        let accounts = AccountsManager.shared.swAccounts

        let bitcoin_wallets = accounts.filter { $0.network == "mainnet"}
        let liquid_wallets = accounts.filter { $0.network == "liquid"}

        let bitcoin_multisig_wallets = accounts.filter { $0.network == "mainnet" && $0.isSingleSig == false}
        let bitcoin_singlesig_wallets = accounts.filter { $0.network == "mainnet" && $0.isSingleSig == true}
        let liquid_multisig_wallets = accounts.filter { $0.network == "liquid" && $0.isSingleSig == false}
        let liquid_singlesig_wallets = accounts.filter { $0.network == "liquid" && $0.isSingleSig == true}

        var props: [String: String] = [:]
        props[AMan.strUserPropertyTotalWallets] = "\((bitcoin_wallets + liquid_wallets).count)"

        props[AMan.strUserPropertyBitcoinWallets] = "\(bitcoin_wallets.count)"
        props[AMan.strUserPropertyBitcoinSinglesigWallets] = "\(bitcoin_singlesig_wallets.count)"
        props[AMan.strUserPropertyBitcoinMultisigWallets] = "\(bitcoin_multisig_wallets.count)"

        props[AMan.strUserPropertyLiquidWallets] = "\(liquid_wallets.count)"
        props[AMan.strUserPropertyLiquidSinglesigWallets] = "\(liquid_singlesig_wallets.count)"
        props[AMan.strUserPropertyLiquidMultisigWallets] = "\(liquid_multisig_wallets.count)"

        Countly.user().custom = props as CountlyUserDetailsNullableDictionary
        Countly.user().save()
    }

//    func appLoadingFinished() {
//        Countly.sharedInstance().appLoadingFinished()
//    }

    func userPropertiesDidChange() {
        guard consent != .notDetermined else { return }
        updateUserProperties()
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
}
