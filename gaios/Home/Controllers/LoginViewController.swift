import Foundation
import UIKit
import PromiseKit

class LoginViewController: UIViewController {

    @IBOutlet weak var cardEnterPin: UIView!
    @IBOutlet weak var cardWalletLock: UIView!

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var attempts: UILabel!

    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var deleteButton: UIButton!
    @IBOutlet var keyButton: [UIButton]?
    @IBOutlet var pinLabel: [UILabel]?
    let menuButton = UIButton(type: .system)

    @IBOutlet weak var lblWalletLockHint1: UILabel!
    @IBOutlet weak var lblWalletLockHint2: UILabel!
    @IBOutlet weak var btnWalletLock: UIButton!
    var account: Account?

    private var pinCode = ""
    private let MAXATTEMPTS = 3

    override func viewDidLoad() {
        super.viewDidLoad()
        let navigationBarHeight: CGFloat =  navigationController!.navigationBar.frame.height
        let imageView = UIImageView(frame: CGRect(x: 0, y: 0, width: navigationBarHeight, height: navigationBarHeight))
        imageView.contentMode = .scaleAspectFit
        imageView.image = account?.icon
        navigationItem.titleView = imageView
        navigationItem.setHidesBackButton(true, animated: false)
        navigationItem.leftBarButtonItem = UIBarButtonItem(image: UIImage.init(named: "backarrow"), style: UIBarButtonItem.Style.plain, target: self, action: #selector(PinLoginViewController.back))
        menuButton.setImage(UIImage(named: "ellipses"), for: .normal)
        menuButton.addTarget(self, action: #selector(menuButtonTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: menuButton)

        progressIndicator?.message = NSLocalizedString("id_logging_in", comment: "")

        setContent()
        setStyle()
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_enter_pin", comment: "")
        lblWalletLockHint1.text = "You've entered an invalid PIN, you don't have any attempts left."
        lblWalletLockHint2.text = "Get your recovery phrase to restore this wallet"
        btnWalletLock.setTitle("Restore with recovery phrase", for: .normal)
    }

    func setStyle() {
        btnWalletLock.setStyle(.primary)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        ScreenLocker.shared.stopObserving()
        NotificationCenter.default.addObserver(self, selector: #selector(progress), name: NSNotification.Name(rawValue: EventType.Tor.rawValue), object: nil)

        cancelButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        deleteButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        for button in keyButton!.enumerated() {
            button.element.addTarget(self, action: #selector(keyClick(sender:)), for: .touchUpInside)
        }
        updateAttemptsLabel()
        reload()
    }

    override func viewDidAppear(_ animated: Bool) {
        if account?.hasBioPin ?? false {
            loginWithPin(usingAuth: AuthenticationTypeHandler.AuthKeyBiometric, withPIN: nil)
        } else if account?.attempts == self.MAXATTEMPTS  || account?.hasPin == false {
            showLock()
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        ScreenLocker.shared.startObserving()
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.Tor.rawValue), object: nil)

        cancelButton.removeTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        deleteButton.removeTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        for button in keyButton!.enumerated() {
            button.element.removeTarget(self, action: #selector(keyClick(sender:)), for: .touchUpInside)
        }
    }

    @objc func menuButtonTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "PopoverMenu", bundle: nil)
        if let popover  = storyboard.instantiateViewController(withIdentifier: "PopoverMenuWalletViewController") as? PopoverMenuWalletViewController {
            popover.delegate = self
            popover.modalPresentationStyle = .popover
            let popoverPresentationController = popover.popoverPresentationController
            popoverPresentationController?.backgroundColor = UIColor.customModalDark()
            popoverPresentationController?.delegate = self
            popoverPresentationController?.sourceView = self.menuButton
            popoverPresentationController?.sourceRect = self.menuButton.bounds
            self.present(popover, animated: true)
        }
    }

    @objc func progress(_ notification: NSNotification) {
        if let json = try? JSONSerialization.data(withJSONObject: notification.userInfo!, options: []),
           let tor = try? JSONDecoder().decode(Tor.self, from: json) {
            var text = NSLocalizedString("id_tor_status", comment: "") + " \(tor.progress)%"
            if tor.progress == 100 {
                text = NSLocalizedString("id_logging_in", comment: "")
            }
            DispatchQueue.main.async {
                self.startLoader(message: text)
            }
        }
    }

    fileprivate func loginWithPin(usingAuth: String, withPIN: String?) {
        let bgq = DispatchQueue.global(qos: .background)
        let appDelegate = getAppDelegate()!

        firstly {
            return Guarantee()
        }.compactMap {
            try self.account?.auth(usingAuth)
        }.get { _ in
            self.startLoader(message: NSLocalizedString("id_logging_in", comment: ""))
        }.then(on: bgq) { data -> Promise<[String: Any]> in
            appDelegate.disconnect()
            try appDelegate.connect(self.account?.network ?? "mainnet")
            let jsonData = try JSONSerialization.data(withJSONObject: data)
            let pin = withPIN ?? data["plaintext_biometric"] as? String
            let pinData = try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: Any]
            let resolver = try getSession().loginWithPin(pin: pin!, pin_data: pinData!)
            return resolver.resolve()
        }.then { _ -> Promise<Void> in
            if self.account?.network == "liquid" {
                return Registry.shared.load()
            }
            return Promise<Void>()
        }.ensure {
            self.stopLoader()
        }.done {
            self.account?.attempts = 0
            AccountsManager.shared.update(self.account!)
            AccountsManager.shared.current = self.account
            appDelegate.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        }.catch { error in
            switch error {
            case AuthenticationTypeHandler.AuthError.CanceledByUser:
                return
            case AuthenticationTypeHandler.AuthError.SecurityError, AuthenticationTypeHandler.AuthError.KeychainError:
                return self.onBioAuthError(error.localizedDescription)
            case is AuthenticationTypeHandler.AuthError:
                DropAlert().error(message: error.localizedDescription)
            case TwoFactorCallError.failure(let desc):
                if desc.contains(":login failed:") && withPIN != nil {
                    self.wrongPin(usingAuth)
                }
            default:
                DropAlert().error(message: NSLocalizedString("id_login_failed", comment: ""))
            }
        }
    }

    func wrongPin(_ usingAuth: String) {
        account?.attempts += 1
        AccountsManager.shared.update(self.account!)
        if account?.attempts == self.MAXATTEMPTS {
            showLock()
        } else {
            self.pinCode = ""
            self.updateAttemptsLabel()
            self.reload()
        }
    }

    func onBioAuthError(_ message: String) {
        let text = String(format: NSLocalizedString("id_syou_need_ton1_reset_greens", comment: ""), message)
        let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: text, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .default) { _ in })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_reset", comment: ""), style: .destructive) { _ in
            self.account?.removeBioKeychainData()
        })
        DispatchQueue.main.async {
            self.present(alert, animated: true, completion: nil)
        }
    }

    func showLock() {
        cardEnterPin.isHidden = true
        lblTitle.isHidden = true
        cardWalletLock.isHidden = false
    }

    func updateAttemptsLabel() {
        let pinattempts = account?.attempts ?? 0
        if pinattempts == MAXATTEMPTS {
            showLock()
        } else if MAXATTEMPTS - pinattempts == 1 {
            attempts.text = NSLocalizedString("id_last_attempt_if_failed_you_will", comment: "")
        } else {
            attempts.text = String(format: NSLocalizedString("id_attempts_remaining_d", comment: ""), MAXATTEMPTS - pinattempts)
        }
        attempts.isHidden = pinattempts == 0
    }

    @objc func keyClick(sender: UIButton) {
        pinCode += (sender.titleLabel?.text)!
        reload()
        guard pinCode.count == 6 else {
            return
        }
        loginWithPin(usingAuth: AuthenticationTypeHandler.AuthKeyPIN, withPIN: self.pinCode)
    }

    func reload() {
        pinLabel?.enumerated().forEach {(index, label) in
            if index < pinCode.count {
                label.textColor = UIColor.customMatrixGreen()
            } else {
                label.textColor = UIColor.black
            }
        }
    }

    @objc func back(sender: UIBarButtonItem) {
        navigationController?.popViewController(animated: true)
    }

    @objc func click(sender: UIButton) {
        if sender == deleteButton {
            if pinCode.count > 0 {
                pinCode.removeLast()
            }
        } else if sender == cancelButton {
            pinCode = ""
        }
        reload()
    }

    func walletDelete() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogWalletDeleteViewController") as? DialogWalletDeleteViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    func walletRename() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogWalletNameViewController") as? DialogWalletNameViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnFaceID(_ sender: Any) {
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController")
        present(vc, animated: true) {
        }
    }

    @IBAction func btnWalletLock(_ sender: Any) {
        LandingViewController.flowType = .restore
        OnBoardManager.shared.params = OnBoardParams(network: account?.network, walletName: account?.name)
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryPhraseViewController")
        navigationController?.pushViewController(vc, animated: true)
    }

}

extension LoginViewController: DialogWalletNameViewControllerDelegate, DialogWalletDeleteViewControllerDelegate {
    func didSave(_ name: String) {
        if var account = self.account {
            AccountsManager.shared.remove(account)
            account.name = name
            AccountsManager.shared.add(account)
        }
    }
    func didDelete() {
        if let account = self.account {
            AccountsManager.shared.remove(account)
            navigationController?.popViewController(animated: true)
        }
    }
    func didCancel() {
        print("Cancel")
    }
}

extension LoginViewController: PopoverMenuWalletDelegate {
    func didSelectionMenuOption(_ menuOption: MenuWalletOption) {
        switch menuOption {
        case .edit:
            walletRename()
        case .delete:
            walletDelete()
        }
    }
}

extension LoginViewController: UIPopoverPresentationControllerDelegate {

    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }

    func presentationController(_ controller: UIPresentationController, viewControllerForAdaptivePresentationStyle style: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: controller.presentedViewController)
    }
}
