import Foundation
import UIKit

import gdk

class WOLoginViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblUsername: UILabel!
    @IBOutlet weak var lblPassword: UILabel!
    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!
    @IBOutlet weak var loginMSButton: UIButton!
    @IBOutlet weak var loginSSButton: UIButton!
    @IBOutlet weak var btnSettings: UIButton!
    @IBOutlet weak var ssModeView: UIView!
    @IBOutlet weak var msModeView: UIView!

    var account: Account!
    private var buttonConstraint: NSLayoutConstraint?
    private var progressToken: NSObjectProtocol?
    private let viewModel = WOViewModel()
    let menuButton = UIButton(type: .system)
    var isSS: Bool { account.gdkNetwork.electrum }

    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.title = account?.name ?? ""

        ssModeView.isHidden = !isSS
        msModeView.isHidden = isSS

        setContent()
        setStyle()

        menuButton.setImage(UIImage(named: "ellipses"), for: .normal)
        menuButton.addTarget(self, action: #selector(menuButtonTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: menuButton)

        loginMSButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        loginSSButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        usernameTextField.addDoneButtonToKeyboard(myAction: #selector(self.usernameTextField.resignFirstResponder))
        passwordTextField.addDoneButtonToKeyboard(myAction: #selector(self.usernameTextField.resignFirstResponder))

        if let username = account?.username {
            usernameTextField.text = username
        }
        if let password = account?.password {
            passwordTextField.text = password
        }
    }

    func setContent() {
        lblTitle.text = "id_log_in_via_watchonly_to_receive".localized
        lblHint.text = ""
        lblUsername.text = "id_username".localized
        lblPassword.text = "id_password".localized
        loginMSButton.setTitle(NSLocalizedString("id_log_in", comment: ""), for: .normal)
        loginSSButton.setTitle(NSLocalizedString("id_log_in", comment: ""), for: .normal)
        btnSettings.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)
    }

    func setStyle() {
        lblTitle.setStyle(.title)
        lblHint.setStyle(.txt)
        lblUsername.setStyle(.sectionTitle)
        lblPassword.setStyle(.sectionTitle)
        loginMSButton.setStyle(.primary)
        loginSSButton.setStyle(.primary)

        usernameTextField.setLeftPaddingPoints(10.0)
        usernameTextField.setRightPaddingPoints(10.0)
        passwordTextField.setLeftPaddingPoints(10.0)
        passwordTextField.setRightPaddingPoints(10.0)
        usernameTextField.layer.cornerRadius = 5.0
        passwordTextField.layer.cornerRadius = 5.0
        usernameTextField.leftViewMode = .always
        passwordTextField.leftViewMode = .always
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

    @objc func menuButtonTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {

            vc.viewModel = DialogListViewModel(title: "More Options", type: .loginPrefs, items: LoginPrefs.getItems(isWatchOnly: true))
            vc.delegate = self
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
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
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
    }

    func walletDelete() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogDeleteViewController") as? DialogDeleteViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    func walletRename() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogRenameViewController") as? DialogRenameViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.index = nil
            vc.delegate = self
            vc.prefill = account.name
            present(vc, animated: false, completion: nil)
        }
    }

    func login() {
        let password = self.passwordTextField.text ?? ""
        dismissKeyboard()
        startLoader(message: NSLocalizedString("id_logging_in", comment: ""))
        Task {
            do {
                if self.isSS {
                    try await self.viewModel.loginSinglesig(for: self.account)
                } else {
                    try await self.viewModel.loginMultisig(for: self.account, password: password)
                }
                success()
            } catch {
                failure(error)
            }
            stopLoader()
        }
    }

    @MainActor
    func success() {
        stopLoader()
        AccountNavigator.goLogged(nv: self.navigationController)
    }
    
    @MainActor
    func failure(_ error: Error) {
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
        stopLoader()
        DropAlert().error(message: NSLocalizedString(prettyError, comment: ""))
        AnalyticsManager.shared.failedWalletLogin(account: self.account, error: error, prettyError: prettyError)
        WalletsRepository.shared.delete(for: self.account)
    }
    
    @objc func click(_ sender: Any) {
        view.endEditing(true)
        login()
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController")
        navigationController?.pushViewController(vc, animated: true)
    }

}

extension WOLoginViewController: DialogRenameViewControllerDelegate, DialogDeleteViewControllerDelegate {
    func didRename(name: String, index: String?) {
        account?.name = name
        if let account = self.account {
            AccountsRepository.shared.current = account
            navigationItem.title = account.name
            AnalyticsManager.shared.renameWallet()
        }
    }
    func didDelete(_ index: String?) {
        if let account = self.account {
            AccountsRepository.shared.remove(account)
            navigationController?.popViewController(animated: true)
            AnalyticsManager.shared.deleteWallet()
        }
    }
    func didCancel() {
    }
}

extension WOLoginViewController: UIPopoverPresentationControllerDelegate {

    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }

    func presentationController(_ controller: UIPresentationController, viewControllerForAdaptivePresentationStyle style: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: controller.presentedViewController)
    }
}

extension WOLoginViewController: DialogListViewControllerDelegate {
    func didSelectIndex(_ index: Int, with type: DialogType) {
        switch type {
        case .loginPrefs:
            switch index {
            case 0:
                walletRename()
            case 1:
                walletDelete()
            default:
                break
            }
        default:
            break
        }
    }
}
