import Foundation
import UIKit
import PromiseKit

class WatchOnlyLoginViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!
    @IBOutlet weak var loginButton: UIButton!
    @IBOutlet weak var btnSettings: UIButton!

    var account: Account?
    private var buttonConstraint: NSLayoutConstraint?
    private var progressToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()

        lblTitle.text = NSLocalizedString("id_log_in_via_watchonly_to_receive", comment: "")
        loginButton.setTitle(NSLocalizedString("id_log_in", comment: ""), for: .normal)
        loginButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        loginButton.setStyle(.primary)
        usernameTextField.placeholder =  NSLocalizedString("id_username", comment: "")
        passwordTextField.placeholder = NSLocalizedString("id_password", comment: "")

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

    @objc func progress(_ notification: Notification) {
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

    @objc func click(_ sender: Any) {
        view.endEditing(true)
        let bgq = DispatchQueue.global(qos: .background)
        let appDelegate = getAppDelegate()!

        firstly {
            view.endEditing(true)
            self.startLoader(message: "Loggin in...")
            return Guarantee()
        }.compactMap(on: bgq) { [self] in
            appDelegate.disconnect()
            try appDelegate.connect(account?.network ?? "mainnet")
            try getSession().loginWatchOnly(username: account?.username ?? "",
                                            password: account?.password ?? "")
        }.then { _ in
            Registry.shared.load()
        }.ensure {
            self.stopLoader()
        }.done {
            AccountsManager.shared.current = self.account
            getGAService().isWatchOnly = true
            appDelegate.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        }.catch { error in
            if let err = error as? GaError, err != GaError.GenericError {
                DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
            } else {
                DropAlert().error(message: NSLocalizedString("id_login_failed", comment: ""))
            }
        }
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController")
        present(vc, animated: true) {
        }
    }

}
