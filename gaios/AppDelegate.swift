import UIKit
import PromiseKit

func getAppDelegate() -> AppDelegate? {
    return UIApplication.shared.delegate as? AppDelegate
}

func getGAService() -> GreenAddressService {
    return AppDelegate.getService()
}

func getSession() -> Session {
    return getGAService().getSession()
}

func getNetwork() -> String {
    let defaults = getUserNetworkSettings()
    return (defaults?["network"] as? String ?? "Mainnet").lowercased()
}

func getGdkNetwork(_ network: String) throws -> [String: Any]? {
    var result = try! getNetworks()
    if !(result?.keys.contains(network))! {
        throw GaError.GenericError
    }
    return result![network] as? [String: Any]
}

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: EventWindow?
    static let service = GreenAddressService()

    static func getService() -> GreenAddressService {
        return service
    }

    func instantiateViewControllerAsRoot(identifier: String) {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let firstVC = storyboard.instantiateViewController(withIdentifier: identifier)
        guard let window = self.window else { return }
        if window.rootViewController != nil {
            window.rootViewController!.navigationController?.popToRootViewController(animated: true)
        }
        window.rootViewController = firstVC
        window.makeKeyAndVisible()
    }

    func connect() throws {
        let networkSettings = getUserNetworkSettings()
        let networkName = ((networkSettings?["network"] as? String) ?? "mainnet").lowercased()
        let useProxy = networkSettings?["proxy"] as? Bool ?? false
        let socks5Hostname = useProxy ? networkSettings?["socks5_hostname"] as? String ?? "" : ""
        let socks5Port = useProxy ? networkSettings?["socks5_port"] as? String ?? "" : ""
        let useTor = networkSettings?["tor"] as? Bool ?? false
        let proxyURI = useProxy ? String(format: "socks5://%@:%@/", socks5Hostname, socks5Port) : ""
        try getSession().connectWithProxy(network: Network(rawValue: networkName)!, proxy_uri: proxyURI, use_tor: useTor)
    }

    func disconnect() {
        try! getSession().disconnect()
    }

    func lock() {
        window?.endEditing(true)
        if isPinEnabled(network: getNetwork()) {
            instantiateViewControllerAsRoot(identifier: "PinLoginNavigationController")
        } else {
            instantiateViewControllerAsRoot(identifier: "InitialViewController")
        }
    }

    func setupAppearance() {
        UINavigationBar.appearance().barTintColor = UIColor.customTitaniumDark()
        UINavigationBar.appearance().tintColor = UIColor.white
        UINavigationBar.appearance().titleTextAttributes = [NSAttributedString.Key.foregroundColor: UIColor.white]
        UINavigationBar.appearance().isTranslucent = false
        UITextField.appearance().keyboardAppearance = .dark
        UITextField.appearance().tintColor = UIColor.customMatrixGreen()
        //To hide the bottom line of the navigation bar.
        UINavigationBar.appearance().setBackgroundImage(UIImage(), for: .any, barMetrics: .default)
        UINavigationBar.appearance().shadowImage = UIImage()
        //Hide the top line of the tab bar
        UITabBar.appearance().shadowImage = UIImage()
        UITabBar.appearance().backgroundImage = UIImage()
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        setupAppearance()

        // Load custom window to handle touches event
        window = EventWindow.init(frame: UIScreen.main.bounds)
        window?.startObserving()

        // Initialize network settings
        onFirstInitialization(network: getNetwork())
        AppDelegate.getService().reset()

        // Set screen lock
        lock()
        ScreenLockWindow.shared.setup()
        ScreenLocker.shared.startObserving()
        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
        ScreenLocker.shared.stopObserving()
        window?.stopObserving()
    }

}
