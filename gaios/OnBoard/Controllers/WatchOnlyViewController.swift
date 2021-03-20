import Foundation
import UIKit
import PromiseKit

class WatchOnlyViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!
    @IBOutlet weak var rememberSwitch: UISwitch!
    @IBOutlet weak var rememberTitle: UILabel!
    @IBOutlet weak var loginButton: UIButton!
    @IBOutlet weak var warningLabel: UILabel!
    @IBOutlet weak var lblTestnet: UILabel!
    @IBOutlet weak var testnetSwitch: UISwitch!
    @IBOutlet weak var btnSettings: UIButton!

    var buttonConstraint: NSLayoutConstraint?

    var username: String? {
        get {
            return UserDefaults.standard.string(forKey: getNetwork() + "_username")
        }
        set {
            UserDefaults.standard.set(newValue, forKey: getNetwork() + "_username")
        }
    }
    var password: String? {
        get {
            return UserDefaults.standard.string(forKey: getNetwork() + "_password")
        }
        set {
            UserDefaults.standard.set(newValue, forKey: getNetwork() + "_password")
        }
    }

    private var network = getGdkNetwork(getNetwork())
    private var progressToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()

        lblTitle.text = "Login"
        lblHint.text = NSLocalizedString("id_log_in_via_watchonly_to_receive", comment: "")
        rememberTitle.text = NSLocalizedString("id_remember_me", comment: "")
        warningLabel.text = NSLocalizedString("id_watchonly_mode_can_be_activated", comment: "")
        lblTestnet.text = "Testnet"
        rememberSwitch.addTarget(self, action: #selector(rememberSwitchChange), for: .valueChanged)
        testnetSwitch.addTarget(self, action: #selector(testnetSwitchChange), for: .valueChanged)
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
        if let username = username {
            usernameTextField.text = username
            rememberSwitch.isOn = true
        }
        if let password = password {
            passwordTextField.text = password
        }
        progressIndicator?.message = NSLocalizedString("id_logging_in", comment: "")
    }

    @objc func rememberSwitchChange(_ sender: UISwitch) {
        if sender.isOn {
            let alert = UIAlertController(title: NSLocalizedString("id_warning_watchonly_credentials", comment: ""), message: NSLocalizedString("id_your_watchonly_username_and", comment: ""), preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in
                sender.isOn = false
            })
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_ok", comment: ""), style: .default) { _ in
                sender.isOn = true
            })
            DispatchQueue.main.async {
                self.present(alert, animated: true, completion: nil)
            }
        } else {
            self.username = nil
            self.password = nil
            usernameTextField.text = ""
            passwordTextField.text = ""
        }
    }

    @objc func testnetSwitchChange(_ sender: UISwitch) {
        print(sender.isOn)
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

    func progress(_ notification: Notification) {
        Guarantee().map(on: DispatchQueue.global(qos: .background)) { () -> UInt32 in
            let json = try JSONSerialization.data(withJSONObject: notification.userInfo!, options: [])
            let tor = try JSONDecoder().decode(Tor.self, from: json)
            return tor.progress
        }.done { progress in
            var text = NSLocalizedString("id_tor_status", comment: "") + " \(progress)%"
            if progress == 100 {
                text = NSLocalizedString("id_logging_in", comment: "")
            }
            self.progressIndicator?.message = text
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.buttonConstraint = self.loginButton.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height)
            self.buttonConstraint?.isActive = true
            if UIScreen.main.nativeBounds.height <= 1334 {
                self.lblHint.isHidden = true
            }
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            if UIScreen.main.nativeBounds.height <= 1334 {
                self.lblHint.isHidden = false
            }
        })
    }

    func dummyLoginAndNext() {

        startLoader(message: "Loggin in...")
        view.endEditing(true)

        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            self.stopLoader()

            self.navigationController?.popToRootViewController(animated: true)
        }

    }

    @objc func click(_ sender: Any) {

        dummyLoginAndNext()

        return

        let bgq = DispatchQueue.global(qos: .background)
        let appDelegate = getAppDelegate()!

        /*firstly {
            dismissKeyboard()
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            appDelegate.disconnect()
        }.compactMap(on: bgq) {
            try appDelegate.connect()
        }.compactMap {
            if let username = self.usernameTextField.text,
                let password = self.passwordTextField.text {
                return (username, password)
            } else {
                return (nil, nil)
            }
        }.compactMap(on: bgq) { (username, password) in
            try getSession().loginWatchOnly(username: username ?? "",
                                            password: password ?? "")
        }.then { _ in
            Registry.shared.refresh().recover { _ in Guarantee() }
        }.ensure {
            self.stopAnimating()
        }.done {
            if self.rememberSwitch.isOn,
                let username = self.usernameTextField.text,
                let password = self.passwordTextField.text {
                self.username = username
                self.password = password
            }
            getGAService().isWatchOnly = true
            appDelegate.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        }.catch { error in
            let message: String
            if let err = error as? GaError, err != GaError.GenericError {
                message = NSLocalizedString("id_connection_failed", comment: "")
            } else {
                message = NSLocalizedString("id_login_failed", comment: "")
            }
            DropAlert().error(message: message)
        }*/
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController")
        present(vc, animated: true) {
        }
    }

}
