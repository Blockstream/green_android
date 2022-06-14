import Foundation
import UIKit
import PromiseKit

protocol DialogWatchOnlySetUpViewControllerDelegate: AnyObject {
    func watchOnlyDidUpdate(_ action: WatchOnlySetUpAction)
}

enum WatchOnlySetUpAction {
    case save
    case delete
    case cancel
}

class DialogWatchOnlySetUpViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var usernameField: UITextField!
    @IBOutlet weak var passwordField: UITextField!
    @IBOutlet weak var btnSave: UIButton!
    @IBOutlet weak var btnDelete: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var btnsStack: UIStackView!
    @IBOutlet weak var lblError: UILabel!

    @IBOutlet weak var lblUsernameError: UILabel!
    @IBOutlet weak var lblPasswordError: UILabel!

    weak var delegate: DialogWatchOnlySetUpViewControllerDelegate?

    var account = { AccountsManager.shared.current }()
    var buttonConstraint: NSLayoutConstraint?
    var wallet: WalletItem?
    var username: String?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0

        if let username = username, username != "" {
            btnSave.setTitle(NSLocalizedString("id_update", comment: ""), for: .normal)
            usernameField.text = username
        } else {
            btnSave.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)
            btnDelete.isHidden = true
        }
        validate()
        lblError.isHidden = true
    }

    func setContent() {
        lblTitle.text = "Watch-only credentials"
        lblHint.text = NSLocalizedString("id_allows_you_to_quickly_check", comment: "")
        btnDelete.setTitle("Delete credentials", for: .normal)
        usernameField.attributedPlaceholder = NSAttributedString(string: NSLocalizedString("id_username", comment: ""), attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        passwordField.attributedPlaceholder = NSAttributedString(string: NSLocalizedString("id_password", comment: ""), attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        lblUsernameError.text = NSLocalizedString("id_at_least_8_character_required", comment: "")
        lblPasswordError.text = NSLocalizedString("id_at_least_8_character_required", comment: "")
    }

    func setStyle() {
        btnDelete.setStyle(.destructiveOutlined)
        usernameField.setLeftPaddingPoints(10.0)
        usernameField.setRightPaddingPoints(10.0)
        passwordField.setLeftPaddingPoints(10.0)
        passwordField.setRightPaddingPoints(10.0)
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
        usernameField.becomeFirstResponder()
    }

    func updateWatchOnly(username: String, password: String, action: WatchOnlySetUpAction) {

        self.lblError.isHidden = true
        self.lblError.text = ""
        let bgq = DispatchQueue.global(qos: .background)
        guard let session = SessionsManager.current else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try session.setWatchOnly(username: username, password: password)
            try self.load()
        }.ensure {
            self.stopAnimating()
        }.done { _ in

            self.dismiss(action)
        }.catch { error in

            print(error.localizedDescription)
            self.lblError.isHidden = false
            self.lblError.text = NSLocalizedString("id_error", comment: "")
        }
    }

    func load() throws {
        if let session = SessionsManager.current {
            if let settings = try session.getSettings() {
                SessionsManager.current?.settings = Settings.from(settings)
            }
            if let account = account, let network = account.gdkNetwork,
               !(account.isSingleSig ?? false) && !network.liquid {
                // watchonly available on multisig for not liquid networks
                    self.username = try session.getWatchOnlyUsername()
            }
        }
    }
    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.buttonConstraint = self.btnsStack.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height - 14.0)
            self.buttonConstraint?.isActive = true
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
        })
    }

    func dismiss(_ action: WatchOnlySetUpAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .save, .delete:
                self.delegate?.watchOnlyDidUpdate(action)
            case .cancel:
                break
            }
        })
    }

    func validate() {

        lblUsernameError.isHidden = true
        lblPasswordError.isHidden = true

        btnSave.setStyle(.primaryDisabled)
        btnSave.isEnabled = false

        if let username = usernameField.text, let password = passwordField.text {

            if usernameField.isFirstResponder {
                if username.count < 8 {
                    lblUsernameError.isHidden = false
                }
            }

            if passwordField.isFirstResponder {
                if password.count < 8 {
                    lblPasswordError.isHidden = false
                }
            }

            if username.count >= 8, password.count >= 8 {
                btnSave.setStyle(.primary)
                btnSave.isEnabled = true
            }
        }
    }

    @IBAction func btnSave(_ sender: Any) {

        if let username = usernameField.text, let password = passwordField.text {
            // first validate
            if username.isEmpty {
                self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: NSLocalizedString("id_enter_a_valid_username", comment: ""))
                return
            } else if password.isEmpty {
                self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: NSLocalizedString("id_the_password_cant_be_empty", comment: ""))
                return
            }
            updateWatchOnly(username: username, password: password, action: .save)
        }
    }

    @IBAction func btnDelete(_ sender: Any) {

        updateWatchOnly(username: "", password: "", action: .delete)
    }

    @IBAction func btnDismiss(_ sender: Any) {

        dismiss(.cancel)
    }

    @IBAction func usernameDidChange(_ sender: Any) {
        validate()
    }

    @IBAction func passwordDidChange(_ sender: Any) {
        validate()
    }

}
