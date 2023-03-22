import Foundation
import UIKit

class AccountNavigator {

    // open the account if just logged or redirect to login
    static func goLogin(account: Account, nv: UINavigationController?) {
        nv?.popToRootViewController(animated: true)
        nv?.dismiss(animated: false, completion: nil)
        let nv = nv ?? UINavigationController()

        let vcHome: HomeViewController? = instantiateViewController(storyboard: "Home", identifier: "Home")
        let vcLogin: LoginViewController? = instantiateViewController(storyboard: "Home", identifier: "LoginViewController")
        let vcConnect: ConnectViewController? = instantiateViewController(storyboard: "HWFlow", identifier: "ConnectViewController")
        let vcWatch: WatchOnlyLoginViewController? = instantiateViewController(storyboard: "OnBoard", identifier: "WatchOnlyLoginViewController")

        // switch on selected active session
        if WalletsRepository.shared.get(for: account.id)?.activeSessions.isEmpty == false {
            goLogged(account: account, nv: nv)
        } else if account.isHW {
            vcConnect?.account = account
            nv.setViewControllers([vcHome!, vcConnect!], animated: true)
        } else if account.isWatchonly {
            vcWatch?.account = account
            nv.setViewControllers([vcHome!, vcWatch!], animated: true)
            return
        } else {
            vcLogin?.account = account
            nv.setViewControllers([vcHome!, vcLogin!], animated: true)
        }
    }

    static func goLogged(account: Account, nv: UINavigationController?) {
        AccountsRepository.shared.current = account
        nv?.popToRootViewController(animated: true)
        nv?.dismiss(animated: false, completion: nil)
        let nv = nv ?? UINavigationController()
        let vcContainer: ContainerViewController? = instantiateViewController(storyboard: "Wallet", identifier: "Container")
        nv.setNavigationBarHidden(true, animated: false)
        nv.setViewControllers([vcContainer!], animated: true)
    }

    static func goLogout(account: Account, nv: UINavigationController?) {
        WalletsRepository.shared.get(for: account.id)?.disconnect()
        goLogin(account: account, nv: nv)
    }

    static func goFirstPage(nv: UINavigationController?) {
        nv?.popToRootViewController(animated: true)
        nv?.dismiss(animated: false, completion: nil)
        let nv = nv ?? UINavigationController()
        if AccountsRepository.shared.accounts.isEmpty {
            let onboard: SelectOnBoardTypeViewController? = instantiateViewController(storyboard: "OnBoard", identifier: "SelectOnBoardTypeViewController")
            nv.setViewControllers([onboard!], animated: true)
        } else {
            let home: HomeViewController? = instantiateViewController(storyboard: "Home", identifier: "Home")
            nv.setViewControllers([home!], animated: true)
        }
        let appDelegate = UIApplication.shared.delegate
        appDelegate?.window??.rootViewController = nv
    }

    static func goAddWallet(nv: UINavigationController?) {
        AnalyticsManager.shared.addWallet()
        nv?.popToRootViewController(animated: true)
        nv?.dismiss(animated: false, completion: nil)
        let home: HomeViewController? = instantiateViewController(storyboard: "Home", identifier: "Home")
        let onboard: SelectOnBoardTypeViewController? = instantiateViewController(storyboard: "OnBoard", identifier: "SelectOnBoardTypeViewController")
        nv?.setViewControllers([home!, onboard!], animated: true)
    }

    static func instantiateViewController<K>(storyboard: String, identifier: String) -> K? {
        let storyboard = UIStoryboard(name: storyboard, bundle: nil)
        return storyboard.instantiateViewController(withIdentifier: identifier) as? K
    }
}
