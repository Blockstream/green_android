import Foundation
import UIKit
import NVActivityIndicatorView
import PromiseKit

class WatchOnlySignIn: KeyboardViewController {
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
    @IBOutlet var content: WatchOnlySignInView!
    var buttonConstraint: NSLayoutConstraint?
    private var network = getGdkNetwork(getNetwork())

    override func viewDidLoad() {
        super.viewDidLoad()
        let logoName = network.liquid ? "btc_liquid" : network.icon
        content.networkLogoImageView.image = UIImage(named: logoName!)
        content.networkNameLabel.text = network.name
        content.titlelabel.text = NSLocalizedString("id_log_in_via_watchonly_to_receive", comment: "")
        content.rememberTitle.text = NSLocalizedString("id_remember_me", comment: "")
        content.warningLabel.text = NSLocalizedString("id_watchonly_mode_can_be_activated", comment: "")
        content.cancelButton.addTarget(self, action: #selector(WatchOnlySignIn.dismissModal), for: .touchUpInside)
        content.rememberSwitch.addTarget(self, action: #selector(rememberSwitch), for: .valueChanged)
        content.loginButton.setTitle(NSLocalizedString("id_log_in", comment: ""), for: .normal)
        content.loginButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.loginButton.setGradient(true)
        let attributes = [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()]
        content.usernameTextField.attributedPlaceholder = NSAttributedString(string: NSLocalizedString("id_username", comment: ""), attributes: attributes)
        content.passwordTextField.attributedPlaceholder = NSAttributedString(string: NSLocalizedString("id_password", comment: ""), attributes: attributes)
        let height = content.usernameTextField.frame.height
        content.usernameTextField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: height))
        content.passwordTextField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: height))
        content.usernameTextField.leftViewMode = .always
        content.passwordTextField.leftViewMode = .always
        if let username = username {
            content.usernameTextField.text = username
            content.rememberSwitch.isOn = true
        }
        if let password = password {
            content.passwordTextField.text = password
        }
        if UIScreen.main.nativeBounds.height <= 1136 {
            content.titlelabel.font = UIFont(name: content.titlelabel.font.fontName, size: 22)
        }
    }

    @objc func dismissModal() {
        self.dismiss(animated: true, completion: nil)
    }

    @objc func rememberSwitch(_ sender: UISwitch) {
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
            content.usernameTextField.text = ""
            content.passwordTextField.text = ""
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        NotificationCenter.default.addObserver(self, selector: #selector(progress), name: NSNotification.Name(rawValue: EventType.Tor.rawValue), object: nil)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.Tor.rawValue), object: nil)
    }

    @objc func progress(_ notification: NSNotification) {
        do {
            let json = try JSONSerialization.data(withJSONObject: notification.userInfo!, options: [])
            let tor = try JSONDecoder().decode(Tor.self, from: json)
            let text = NSLocalizedString("id_tor_status", comment: "") + " \(tor.progress)%"
            NVActivityIndicatorPresenter.sharedInstance.setMessage(text)
        } catch {
            print(error.localizedDescription)
        }
    }

    override func keyboardWillShow(notification: NSNotification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.buttonConstraint = self.content.loginButton.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height)
            self.buttonConstraint?.isActive = true
            if UIScreen.main.nativeBounds.height <= 1334 {
                self.content.titlelabel.isHidden = true
                self.content.greenBlockView.isHidden = true
            }
        })
    }

    override func keyboardWillHide(notification: NSNotification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            if UIScreen.main.nativeBounds.height <= 1334 {
                self.content.titlelabel.isHidden = false
                self.content.greenBlockView.isHidden = false
            }
        })
    }

    @objc func click(_ sender: Any) {
        let bgq = DispatchQueue.global(qos: .background)
        let appDelegate = getAppDelegate()!

        firstly {
            dismissKeyboard()
            self.startAnimating(message: NSLocalizedString("id_logging_in", comment: ""))
            return Guarantee()
        }.compactMap(on: bgq) {
            appDelegate.disconnect()
        }.compactMap(on: bgq) {
            try appDelegate.connect()
        }.compactMap {
            if let username = self.content.usernameTextField.text,
                let password = self.content.passwordTextField.text {
                return (username, password)
            } else {
                return (nil, nil)
            }
        }.compactMap(on: bgq) { (username, password) in
            try getSession().loginWatchOnly(username: username ?? "",
                                            password: password ?? "")
        }.ensure {
            self.stopAnimating()
        }.done {
            if self.content!.rememberSwitch.isOn,
                let username = self.content.usernameTextField.text,
                let password = self.content.passwordTextField.text {
                self.username = username
                self.password = password
            }
            getGAService().isWatchOnly = true
            appDelegate.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        }.catch { error in
            let message: String
            if let err = error as? GaError, err != GaError.GenericError {
                message = NSLocalizedString("id_you_are_not_connected_to_the", comment: "")
            } else {
                message = NSLocalizedString("id_login_failed", comment: "")
            }
            Toast.show(message, timeout: Toast.SHORT)
        }
    }
}

@IBDesignable
class WatchOnlySignInView: UIView {
    @IBOutlet weak var titlelabel: UILabel!
    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!
    @IBOutlet weak var rememberSwitch: UISwitch!
    @IBOutlet weak var rememberTitle: UILabel!
    @IBOutlet weak var loginButton: UIButton!
    @IBOutlet weak var warningLabel: UILabel!
    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var networkLogoImageView: UIImageView!
    @IBOutlet weak var networkNameLabel: UILabel!
    @IBOutlet weak var greenBlockView: UIView!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        loginButton.updateGradientLayerFrame()
    }
}
