import Foundation
import UIKit
import AsyncBluetooth

class AccountNavigator {

    // open the account if just logged or redirect to login
    static func goLogin(account: Account, nv: UINavigationController?) -> UINavigationController {
        nv?.popToRootViewController(animated: false)
        nv?.dismiss(animated: false, completion: nil)
        let nv = nv ?? UINavigationController()

        let vcHome: HomeViewController? = instantiateViewController(storyboard: "Home", identifier: "Home")
        let vcLogin: LoginViewController? = instantiateViewController(storyboard: "Home", identifier: "LoginViewController")
        let vcConnect: ConnectViewController? = instantiateViewController(storyboard: "HWFlow", identifier: "ConnectViewController")
        let vcWatch: WOLoginViewController? = instantiateViewController(storyboard: "WOFlow", identifier: "WOLoginViewController")

        // switch on selected active session
        if WalletsRepository.shared.get(for: account.id)?.activeSessions.isEmpty == false {
            return goLogged(account: account, nv: nv)
        } else if account.isHW {
            vcConnect?.account = account
            vcConnect?.bleViewModel = BleViewModel.shared
            vcConnect?.scanViewModel = ScanViewModel()
            nv.setViewControllers([vcHome!, vcConnect!], animated: true)
        } else if account.isWatchonly {
            vcWatch?.account = account
            nv.setViewControllers([vcHome!, vcWatch!], animated: true)
        } else {
            vcLogin?.account = account
            nv.setViewControllers([vcHome!, vcLogin!], animated: true)
        }
        return nv
    }

    static func goLogged(account: Account, nv: UINavigationController?) -> UINavigationController {
        AccountsRepository.shared.current = account
        nv?.popToRootViewController(animated: false)
        nv?.dismiss(animated: false, completion: nil)
        let nv = nv ?? UINavigationController()
        let vcContainer: ContainerViewController? = instantiateViewController(storyboard: "Wallet", identifier: "Container")
        nv.setNavigationBarHidden(true, animated: false)
        nv.setViewControllers([vcContainer!], animated: false)
        return nv
    }

    static func goLogout(account: Account, nv: UINavigationController?) -> UINavigationController {
        //WalletsRepository.shared.get(for: account.id)?.disconnect()
        let appDelegate = UIApplication.shared.delegate
        let nv = goLogin(account: account, nv: nv)
        appDelegate?.window??.rootViewController = nv
        return nv
    }

    static func goFirstPage(nv: UINavigationController?) -> UINavigationController {
        nv?.popToRootViewController(animated: false)
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
        return nv
    }

    static func goAddWallet(nv: UINavigationController?) -> UINavigationController {
        nv?.popToRootViewController(animated: false)
        nv?.dismiss(animated: false, completion: nil)
        let nv = nv ?? UINavigationController()
        let home: HomeViewController? = instantiateViewController(storyboard: "Home", identifier: "Home")
        let onboard: SelectOnBoardTypeViewController? = instantiateViewController(storyboard: "OnBoard", identifier: "SelectOnBoardTypeViewController")
        nv.setViewControllers([home!, onboard!], animated: true)
        return nv
    }

    static func instantiateViewController<K>(storyboard: String, identifier: String) -> K? {
        let storyboard = UIStoryboard(name: storyboard, bundle: nil)
        return storyboard.instantiateViewController(withIdentifier: identifier) as? K
    }
}
