import Foundation
import UIKit
import PromiseKit

class WatchOnlyLoginViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!
    @IBOutlet weak var loginButton: UIButton!
    @IBOutlet weak var btnSettings: UIButton!

    var account: Account!
    private var buttonConstraint: NSLayoutConstraint?
    private var progressToken: NSObjectProtocol?
    let menuButton = UIButton(type: .system)

    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.title = account?.name ?? ""
        navigationItem.setHidesBackButton(true, animated: false)

        let ntwBtn = UIButton(type: .system)
        let img = account?.icon ?? UIImage()
        ntwBtn.setImage(img.withRenderingMode(.alwaysOriginal), for: .normal)
        ntwBtn.imageView?.contentMode = .scaleAspectFit
        ntwBtn.addTarget(self, action: #selector(WatchOnlyLoginViewController.back), for: .touchUpInside)
        ntwBtn.contentEdgeInsets = UIEdgeInsets(top: 9, left: -16, bottom: 9, right: 0)

        navigationItem.leftBarButtonItems = [UIBarButtonItem(image: UIImage.init(named: "backarrow"), style: UIBarButtonItem.Style.plain, target: self, action: #selector(WatchOnlyLoginViewController.back)), UIBarButtonItem(customView: ntwBtn)]
        menuButton.setImage(UIImage(named: "ellipses"), for: .normal)
        menuButton.addTarget(self, action: #selector(menuButtonTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: menuButton)

        lblTitle.text = NSLocalizedString("id_log_in_via_watchonly_to_receive", comment: "")
        loginButton.setTitle(NSLocalizedString("id_log_in", comment: ""), for: .normal)
        loginButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        loginButton.setStyle(.primary)
        usernameTextField.attributedPlaceholder = NSAttributedString(
            string: NSLocalizedString("id_username", comment: ""),
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        passwordTextField.attributedPlaceholder = NSAttributedString(
            string: NSLocalizedString("id_password", comment: ""),
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        btnSettings.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)

        usernameTextField.setLeftPaddingPoints(10.0)
        usernameTextField.setRightPaddingPoints(10.0)
        passwordTextField.setLeftPaddingPoints(10.0)
        passwordTextField.setRightPaddingPoints(10.0)

        usernameTextField.leftViewMode = .always
        passwordTextField.leftViewMode = .always
        if let username = account?.username {
            usernameTextField.text = username
        }
        if let password = account?.password {
            passwordTextField.text = password
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        progressToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Tor.rawValue), object: nil, queue: .main, using: progress)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = progressToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    @objc func back(sender: UIBarButtonItem) {
        navigationController?.popViewController(animated: true)
    }

    @objc func menuButtonTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "PopoverMenu", bundle: nil)
        if let popover  = storyboard.instantiateViewController(withIdentifier: "PopoverMenuWalletViewController") as? PopoverMenuWalletViewController {
            popover.delegate = self
            popover.menuOptions = [.edit, .delete]
            popover.modalPresentationStyle = .popover
            let popoverPresentationController = popover.popoverPresentationController
            popoverPresentationController?.backgroundColor = UIColor.customModalDark()
            popoverPresentationController?.delegate = self
            popoverPresentationController?.sourceView = self.menuButton
            popoverPresentationController?.sourceRect = self.menuButton.bounds
            self.present(popover, animated: true)
        }
    }

    @objc func progress(_ notification: Notification) {
        if let json = try? JSONSerialization.data(withJSONObject: notification.userInfo!, options: []),
           let tor = try? JSONDecoder().decode(TorNotification.self, from: json) {
            var text = NSLocalizedString("id_tor_status", comment: "") + " \(tor.progress)%"
            if tor.progress == 100 {
                text = NSLocalizedString("id_logging_in", comment: "")
            }
            DispatchQueue.main.async {
                self.startLoader(message: text)
            }
        }
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.buttonConstraint = self.loginButton.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height)
            self.buttonConstraint?.isActive = true
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
        })
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
            vc.index = nil
            vc.delegate = self
            vc.prefill = account.name
            present(vc, animated: false, completion: nil)
        }
    }

    @objc func click(_ sender: Any) {
        view.endEditing(true)

        let username = self.account?.username ?? ""
        let password = self.passwordTextField.text ?? ""
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            view.endEditing(true)
            self.startLoader(message: NSLocalizedString("id_logging_in", comment: ""))
            return Guarantee()
        }.compactMap {
            WalletManager.getOrAdd(for: self.account)
        }.then(on: bgq) {
            $0.login(Credentials(username: username, password: password))
        }.ensure {
            self.stopLoader()
        }.done { _ in
            AccountsManager.shared.current = self.account
            AnalyticsManager.shared.loginWallet(loginType: .watchOnly, ephemeralBip39: false, account: AccountsManager.shared.current)
            let appDelegate = UIApplication.shared.delegate as? AppDelegate
            appDelegate!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        }.catch { error in
            var prettyError = "id_login_failed"
            switch error {
            case TwoFactorCallError.failure(let localizedDescription):
                prettyError = localizedDescription
            case LoginError.connectionFailed:
                prettyError = "id_connection_failed"
            case LoginError.failed:
                prettyError = "id_login_failed"
            default:
                break
            }
            DropAlert().error(message: NSLocalizedString(prettyError, comment: ""))
            AnalyticsManager.shared.failedWalletLogin(account: self.account, error: error, prettyError: prettyError)
            WalletManager.delete(for: self.account)
        }
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController")
        present(vc, animated: true) {
        }
    }

}

extension WatchOnlyLoginViewController: DialogWalletNameViewControllerDelegate, DialogWalletDeleteViewControllerDelegate {
    func didRename(name: String, index: Int?) {
        account?.name = name
        if let account = self.account {
            AccountsManager.shared.current = account
            navigationItem.title = account.name
            AnalyticsManager.shared.renameWallet()
        }
    }
    func didDelete() {
        if let account = self.account {
            AccountsManager.shared.remove(account)
            navigationController?.popViewController(animated: true)
            AnalyticsManager.shared.deleteWallet()
        }
    }
    func didCancel() {
    }
}

extension WatchOnlyLoginViewController: PopoverMenuWalletDelegate {
    func didSelectionMenuOption(_ menuOption: MenuWalletOption) {
        switch menuOption {
        case .emergency:
            break
        case .passphrase:
            break
        case .edit:
            walletRename()
        case .delete:
            walletDelete()
        }
    }
}

extension WatchOnlyLoginViewController: UIPopoverPresentationControllerDelegate {

    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }

    func presentationController(_ controller: UIPresentationController, viewControllerForAdaptivePresentationStyle style: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: controller.presentedViewController)
    }
}
