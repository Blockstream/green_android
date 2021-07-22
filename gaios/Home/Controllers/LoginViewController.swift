import Foundation
import UIKit
import PromiseKit

class LoginViewController: UIViewController {

    @IBOutlet weak var cardEnterPin: UIView!
    @IBOutlet weak var cardWalletLock: UIView!

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var attempts: UILabel!
    @IBOutlet weak var connectionSettingsButton: UIButton!

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

    private var networkSettings: [String: Any] {
        get {
            UserDefaults.standard.value(forKey: "network_settings") as? [String: Any] ?? [:]
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.title = account?.name ?? ""
        navigationItem.setHidesBackButton(true, animated: false)
        navigationItem.leftBarButtonItems =
            [UIBarButtonItem(image: UIImage.init(named: "backarrow"), style: UIBarButtonItem.Style.plain, target: self, action: #selector(LoginViewController.back)),
             UIBarButtonItem(image: (account?.icon ?? UIImage(named: ""))?.withRenderingMode(.alwaysOriginal), style: UIBarButtonItem.Style.plain, target: self, action: #selector(LoginViewController.back))
            ]
        menuButton.setImage(UIImage(named: "ellipses"), for: .normal)
        menuButton.addTarget(self, action: #selector(menuButtonTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: menuButton)

        setContent()
        setStyle()

        view.accessibilityIdentifier = AccessibilityIdentifiers.LoginScreen.view
        navigationItem.leftBarButtonItem?.accessibilityIdentifier = AccessibilityIdentifiers.LoginScreen.backBtn
        menuButton.accessibilityIdentifier = AccessibilityIdentifiers.LoginScreen.menuBtn
        keyButton![0].accessibilityIdentifier = AccessibilityIdentifiers.LoginScreen.btn1
        keyButton![1].accessibilityIdentifier = AccessibilityIdentifiers.LoginScreen.btn2
        keyButton![2].accessibilityIdentifier = AccessibilityIdentifiers.LoginScreen.btn3
        attempts.accessibilityIdentifier = AccessibilityIdentifiers.LoginScreen.attemptsLbl
        connectionSettingsButton.accessibilityIdentifier = AccessibilityIdentifiers.LoginScreen.settingsBtn
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_enter_pin", comment: "")
        lblWalletLockHint1.text = NSLocalizedString("id_youve_entered_an_invalid_pin", comment: "")
        lblWalletLockHint2.isHidden = true
        btnWalletLock.setTitle(NSLocalizedString("id_restore_with_recovery_phrase", comment: ""), for: .normal)
        connectionSettingsButton.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)
        cancelButton.setTitle(NSLocalizedString("id_cancel", comment: ""), for: .normal)
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

    func presentDialogTorUnavailable() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogTorSingleSigViewController") as? DialogTorSingleSigViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                self.present(vc, animated: false, completion: nil)
            }
        }
    }

    override func viewDidAppear(_ animated: Bool) {

        if networkSettings["tor"] as? Bool ?? false &&
            account?.isSingleSig ?? false &&
            !UserDefaults.standard.bool(forKey: AppStorage.dontShowTorAlert) {
            presentDialogTorUnavailable()
        } else {
            torCheckDone()
        }
    }

    func torCheckDone() {
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
            try appDelegate.connect(self.account?.networkName ?? "mainnet")
            let jsonData = try JSONSerialization.data(withJSONObject: data)
            let pin = withPIN ?? data["plaintext_biometric"] as? String
            let pinData = try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: Any]
            let resolver = try getSession().loginUser(details: ["pin": pin!, "pin_data": pinData!])
            return resolver.resolve()
        }.then { _ -> Promise<Void> in
            if self.account?.network == "liquid" {
                return Registry.shared.load()
            }
            return Promise<Void>()
        }.compactMap { _ in
            self.startLoader(message: NSLocalizedString("id_loading_wallet", comment: ""))
        }.then(on: bgq) { _ -> Promise<WalletItem> in
            let pointerKey = String(format: "%@_wallet_pointer", self.account?.id ?? "")
            let pointer = UserDefaults.standard.integer(forKey: pointerKey)
            return getSubaccount(UInt32(pointer))
                .recover {_ in
                    getSubaccount(0)
                }
        }.get { _ in
            if withPIN != nil {
                self.account?.attempts = 0
            }
            AccountsManager.shared.current = self.account
        }.done { wallet in
            let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
            let nav = storyboard.instantiateViewController(withIdentifier: "TabViewController") as? UINavigationController
            if let vc = nav?.topViewController as? ContainerViewController {
                vc.presentingWallet = wallet
            }
            self.stopLoader()
            UIApplication.shared.keyWindow?.rootViewController = nav
        }.catch { error in
            self.stopLoader()
            switch error {
            case AuthenticationTypeHandler.AuthError.CanceledByUser:
                return
            case AuthenticationTypeHandler.AuthError.SecurityError, AuthenticationTypeHandler.AuthError.KeychainError:
                return self.onBioAuthError(error.localizedDescription)
            case AuthenticationTypeHandler.AuthError.ConnectionFailed:
                DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
            case is AuthenticationTypeHandler.AuthError:
                DropAlert().error(message: NSLocalizedString("id_login_failed", comment: ""))
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
        AccountsManager.shared.current = self.account
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
        if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
            vc.delegate = self
            present(vc, animated: true) {}
        }
    }

    @IBAction func btnWalletLock(_ sender: Any) {
        LandingViewController.flowType = .restore
        OnBoardManager.shared.params = OnBoardParams(network: account?.network, walletName: account?.name, accountId: account?.id ?? UUID().uuidString)
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryPhraseViewController")
        navigationController?.pushViewController(vc, animated: true)
    }

}

extension LoginViewController: DialogWalletNameViewControllerDelegate, DialogWalletDeleteViewControllerDelegate {
    func didSave(_ name: String) {
        self.account?.name = name
        if let account = self.account {
            AccountsManager.shared.current = account
            navigationItem.title = account.name
        }
    }
    func didDelete() {
        if let account = self.account {
            AccountsManager.shared.remove(account)
            navigationController?.popViewController(animated: true)
        }
    }
    func didCancel() {
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

extension LoginViewController: DialogTorSingleSigViewControllerDelegate {
    func didContinue() {
        torCheckDone()
    }
}

extension LoginViewController: WalletSettingsViewControllerDelegate {
    func didSet(tor: Bool) {
        if tor == true && account?.isSingleSig ?? false {
            DispatchQueue.main.async {
                self.presentDialogTorUnavailable()
            }
        }
    }
}
