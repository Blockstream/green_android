import Foundation
import UIKit

class AccountNavigator {

    // redirect to create/restore flow
    static func goCreateRestore() {
        let homeS = UIStoryboard(name: "Home", bundle: nil)
        let onBoardS = UIStoryboard(name: "OnBoard", bundle: nil)
        if let nav = homeS.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController,
            let vc = onBoardS.instantiateViewController(withIdentifier: "LandingViewController") as? LandingViewController {
            UIApplication.shared.keyWindow?.rootViewController = nav
            nav.pushViewController(vc, animated: false)
        }
    }

    // open the account if just logged or redirect to login
    static func goLogin(account: Account) {
        // switch on selected active session
        if WalletManager.get(for: account.id)?.activeSessions.isEmpty == false {
            AccountDao.shared.current = account
            let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
            let nav = storyboard.instantiateViewController(withIdentifier: "TabViewController") as? UINavigationController
            UIApplication.shared.keyWindow?.rootViewController = nav
           return
        }
        // switch on pin view of selected account
        let homeS = UIStoryboard(name: "Home", bundle: nil)
        let onBoardS = UIStoryboard(name: "OnBoard", bundle: nil)
        if account.isWatchonly {
            if let nav = homeS.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController,
                let vc = onBoardS.instantiateViewController(withIdentifier: "WatchOnlyLoginViewController") as? WatchOnlyLoginViewController {
                    vc.account = account
                    UIApplication.shared.keyWindow?.rootViewController = nav
                    nav.pushViewController(vc, animated: false)
            }
            return
        }
        if let nav = homeS.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController,
            let vc = homeS.instantiateViewController(withIdentifier: "LoginViewController") as? LoginViewController {
                vc.account = account
                UIApplication.shared.keyWindow?.rootViewController = nav
                nav.pushViewController(vc, animated: false)
        }
    }

    // redirect to hw scanner
    static func goHWLogin(isJade: Bool) {
        let storyboardHome = UIStoryboard(name: "Home", bundle: nil)
        let storyboardHW = UIStoryboard(name: "HWW", bundle: nil)
        let nav = storyboardHome.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController
        if let vc = storyboardHW.instantiateViewController(withIdentifier: "HWWScanViewController") as? HWWScanViewController {
            vc.jade = isJade
            UIApplication.shared.keyWindow?.rootViewController = nav
            nav?.pushViewController(vc, animated: false)
        }
    }
}
