import UIKit
import PromiseKit
import RxBluetoothKit
import gdk
import lightning

func getAppDelegate() -> AppDelegate? {
    return UIApplication.shared.delegate as? AppDelegate
}

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?
    var navigateWindow: UIWindow?

    func setupAppearance() {
        if #available(iOS 15.0, *) {
            let appearance = UINavigationBarAppearance()
            appearance.backgroundColor = UIColor.customTitaniumDark()
            appearance.titleTextAttributes = [NSAttributedString.Key.foregroundColor: UIColor.white]
            appearance.shadowImage = UIImage.imageWithColor(color: UIColor.customTitaniumDark())
            appearance.backgroundImage = UIImage()
            UINavigationBar.appearance().standardAppearance = appearance
            UINavigationBar.appearance().scrollEdgeAppearance = appearance
            UINavigationBar.appearance().isTranslucent = false
        }
        UINavigationBar.appearance().barTintColor = UIColor.customTitaniumDark()
        UINavigationBar.appearance().tintColor = UIColor.white
        UINavigationBar.appearance().titleTextAttributes = [NSAttributedString.Key.foregroundColor: UIColor.white]
        UINavigationBar.appearance().isTranslucent = false
        UITextField.appearance().keyboardAppearance = .dark
        UITextField.appearance().tintColor = UIColor.customMatrixGreen()
        // To hide the bottom line of the navigation bar.
        UINavigationBar.appearance().setBackgroundImage(UIImage(), for: .any, barMetrics: .default)
        UINavigationBar.appearance().shadowImage = UIImage()
        // Hide the top line of the tab bar
        UITabBar.appearance().shadowImage = UIImage()
        UITabBar.appearance().backgroundImage = UIImage()
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        setupAppearance()

        // Load custom window to handle touches event
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.endEditing(true)

        // Initialize gdk
        GdkInit.defaults().run()
        LightningBridge.configure()

        // Checking for migrations
        MigratorManager.shared.migrate()

        // Set screen lock
        AccountNavigator.goFirstPage(nv: nil)
        ScreenLockWindow.shared.setup()
        ScreenLocker.shared.startObserving()

        #if targetEnvironment(simulator)
        // Disable hardware keyboards.
        let setHardwareLayout = NSSelectorFromString("setHardwareLayout:")
        UITextInputMode.activeInputModes
            .filter({ $0.responds(to: setHardwareLayout) })
            .forEach { $0.perform(setHardwareLayout, with: nil) }
        #endif

        AnalyticsManager.shared.countlyStart()
        AnalyticsManager.shared.setupSession(session: nil)

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
    }

}
