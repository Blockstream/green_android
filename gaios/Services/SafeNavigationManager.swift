import Foundation
import UIKit

class SafeNavigationManager {

    static let shared = SafeNavigationManager()

    func navigate(_ urlString: String?) {
        guard let urlString = urlString, let url = URL(string: urlString) else {
            return
        }

        let networkSettings = getUserNetworkSettings()
        if let tor = networkSettings["tor"] as? Bool, tor { } else {
            UIApplication.shared.open( url )
            return
        }

        let appDelegate = UIApplication.shared.delegate as? AppDelegate

        appDelegate?.navigateWindow = UIWindow(frame: UIScreen.main.bounds)
        appDelegate?.navigateWindow?.windowLevel = .alert
        appDelegate?.navigateWindow?.tag = 999

        if let con = UIStoryboard(name: "Shared", bundle: .main)
            .instantiateViewController(
                withIdentifier: "DialogSafeNavigationViewController") as? DialogSafeNavigationViewController {
            con.onSelect = { (action: SafeNavigationAction) in
                switch action {
                case .authorize:
                    UIApplication.shared.open( url )
                case .cancel:
                    break
                case .copy:
                    UIPasteboard.general.string = urlString
                    DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 1.0)
                    UINotificationFeedbackGenerator().notificationOccurred(.success)
                }
                appDelegate?.navigateWindow = nil
            }
            appDelegate?.navigateWindow?.rootViewController = con
        }
        appDelegate?.navigateWindow?.makeKeyAndVisible()
    }
}
