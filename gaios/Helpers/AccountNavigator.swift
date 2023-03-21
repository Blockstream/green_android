import Foundation
import UIKit

class AccountNavigator {

    // redirect to create/restore flow
    static func goCreateRestore(navigationController: UINavigationController?) {
        guard let navigationController = navigationController else { return }

        AnalyticsManager.shared.addWallet()
        let homeS = UIStoryboard(name: "Home", bundle: nil)
        let onBoardS = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vcHome = homeS.instantiateViewController(withIdentifier: "Home") as? HomeViewController,
            let vcSelect = onBoardS.instantiateViewController(withIdentifier: "SelectOnBoardTypeViewController") as? SelectOnBoardTypeViewController {
            navigationController.setViewControllers([vcHome, vcSelect], animated: true)
        }
    }

    // open the account if just logged or redirect to login
    static func goLogin(account: Account, navigationController: UINavigationController?) {
        // switch on selected active session
        if WalletsRepository.shared.get(for: account.id)?.activeSessions.isEmpty == false {
            AccountsRepository.shared.current = account
            let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
            if let vcContainer = storyboard.instantiateViewController(withIdentifier: "Container") as? ContainerViewController {
                navigationController?.setNavigationBarHidden(true, animated: false)
                navigationController?.setViewControllers([vcContainer], animated: true)
            }
            return
        }
        let homeS = UIStoryboard(name: "Home", bundle: nil)
        let onBoardS = UIStoryboard(name: "OnBoard", bundle: nil)
        let hwflow = UIStoryboard(name: "HWFlow", bundle: nil)
        if account.isHW {
            if let vcHome = homeS.instantiateViewController(withIdentifier: "Home") as? HomeViewController,
               let vcConnect = hwflow.instantiateViewController(withIdentifier: "ConnectViewController") as? ConnectViewController {
                vcConnect.account = account
                navigationController?.setViewControllers([vcHome, vcConnect], animated: true)
                return
            }
        }
        // switch on pin view of selected account
        if account.isWatchonly {
            if let vcHome = homeS.instantiateViewController(withIdentifier: "Home") as? HomeViewController,
                let vcWatch = onBoardS.instantiateViewController(withIdentifier: "WatchOnlyLoginViewController") as? WatchOnlyLoginViewController {
                vcWatch.account = account
                navigationController?.setViewControllers([vcHome, vcWatch], animated: true)
            }
            return
        }
        if let vcHome = homeS.instantiateViewController(withIdentifier: "Home") as? HomeViewController,
            let vcLogin = homeS.instantiateViewController(withIdentifier: "LoginViewController") as? LoginViewController {
            vcLogin.account = account
            navigationController?.setViewControllers([vcHome, vcLogin], animated: true)
        }
    }
}
