import UIKit
import PromiseKit
import RxBluetoothKit

func getAppDelegate() -> AppDelegate? {
    return UIApplication.shared.delegate as? AppDelegate
}

func getNetwork() -> String {
    AccountsManager.shared.current?.network ?? "mainnet"
}

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: EventWindow?

    func instantiateViewControllerAsRoot(storyboard: String, identifier: String) {
        let storyboard = UIStoryboard(name: storyboard, bundle: nil)
        let firstVC = storyboard.instantiateViewController(withIdentifier: identifier)
        window?.rootViewController?.navigationController?.popToRootViewController(animated: true)
        window?.rootViewController?.dismiss(animated: false, completion: nil)
        window?.rootViewController = firstVC
        window?.makeKeyAndVisible()
    }

    func lock(with pin: Bool) {
        window?.endEditing(true)
        instantiateViewControllerAsRoot(storyboard: "Home", identifier: "HomeViewController")
    }

    func logout(with pin: Bool) {
        if let account = AccountsManager.shared.current,
           account.isJade || account.isLedger {
            Ledger.shared.clear()
            Jade.shared.clear()
            BLEManager.shared.dispose()
        }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            window?.rootViewController?.startAnimating()
            return Guarantee()
        }.map(on: bgq) {
            SessionManager.shared.disconnect()
        }.ensure {
            self.window?.rootViewController?.stopAnimating()
        }.done {
            self.lock(with: pin)
        }.catch { _ in
            fatalError("disconnection error never happens")
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
        AccountsManager.shared.onFirstInitialization()

        // Set screen lock
        lock(with: false)
        ScreenLockWindow.shared.setup()
        ScreenLocker.shared.startObserving()

        #if targetEnvironment(simulator)
        // Disable hardware keyboards.
        let setHardwareLayout = NSSelectorFromString("setHardwareLayout:")
        UITextInputMode.activeInputModes
            .filter({ $0.responds(to: setHardwareLayout) })
            .forEach { $0.perform(setHardwareLayout, with: nil) }
        #endif

        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
        try? SessionManager.shared.reconnectHint(hint: ["tor_sleep_hint": "sleep", "hint": "disable"])
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
        DispatchQueue.global(qos: .background).async {
            try? SessionManager.shared.reconnectHint(hint: ["tor_sleep_hint": "wakeup", "hint": "now"])
        }
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
