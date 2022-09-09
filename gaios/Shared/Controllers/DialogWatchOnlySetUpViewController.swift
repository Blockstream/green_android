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
    @IBOutlet weak var lblUsernameError: UILabel!
    @IBOutlet weak var lblPasswordError: UILabel!
    @IBOutlet weak var btnSecure: UIButton!

    weak var delegate: DialogWatchOnlySetUpViewControllerDelegate?

    var account = { AccountDao.shared.current }()
    var buttonConstraint: NSLayoutConstraint?
    var wallet: WalletItem?
    var username: String?
    var preDeleteFlag = false

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
        passwordField.isSecureTextEntry = true
        updateSecureBtn()

        view.accessibilityIdentifier = AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.view
        usernameField.accessibilityIdentifier = AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.usernameField
        passwordField.accessibilityIdentifier = AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.passwordField
        btnSave.accessibilityIdentifier = AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.saveBtn
        btnDelete.accessibilityIdentifier = AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.deleteBtn
        btnDismiss.accessibilityIdentifier = AccessibilityIdentifiers.DialogWatchOnlySetUpScreen.dismissBtn

        AnalyticsManager.shared.recordView(.watchOnlyCredentials)
    }

    func setContent() {
        lblTitle.text = "Watch-only credentials"
        lblHint.text = NSLocalizedString("id_allows_you_to_quickly_check", comment: "")
        btnDelete.setTitle("Delete credentials", for: .normal)
        usernameField.attributedPlaceholder = NSAttributedString(string: NSLocalizedString("id_username", comment: ""), attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        passwordField.attributedPlaceholder = NSAttributedString(string: NSLocalizedString("id_password", comment: ""), attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        lblUsernameError.text = NSLocalizedString("id_at_least_8_characters_required", comment: "")
        lblPasswordError.text = NSLocalizedString("id_at_least_8_characters_required", comment: "")
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

    func updateSecureBtn() {
        let img = passwordField.isSecureTextEntry == true ? UIImage(named: "ic_eye")!.maskWithColor(color: UIColor.customMatrixGreen()) : UIImage(named: "ic_hide")!.maskWithColor(color: UIColor.customMatrixGreen())
        btnSecure.setImage(img, for: .normal)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
        usernameField.becomeFirstResponder()
    }

    func updateWatchOnly(username: String, password: String, action: WatchOnlySetUpAction) {

        let bgq = DispatchQueue.global(qos: .background)
        guard let session = WalletManager.current?.currentSession else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            session.setWatchOnly(username: username, password: password)
        }.compactMap(on: bgq) {
            try self.load()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.dismiss(action)
        }.catch { err in
            switch err {
            case GaError.ReconnectError(let msg),
                GaError.TimeoutError(let msg),
                GaError.SessionLost(let msg),
                GaError.GenericError(let msg):
                self.showError(msg ?? "id_error")
            default:
                self.showError(err.localizedDescription)
            }
        }
    }

    func load() throws {
        if let session = WalletManager.current?.currentSession {
            if let settings = try session.session?.getSettings() {
                WalletManager.current?.currentSession?.settings = Settings.from(settings)
            }
            if let account = account, let network = account.gdkNetwork,
               !(account.isSingleSig ?? false) && !network.liquid {
                // watchonly available on multisig for not liquid networks
                session.getWatchOnlyUsername().done {
                    self.username = $0
                }
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

        if preDeleteFlag {
            updateWatchOnly(username: "", password: "", action: .delete)
        } else {
            preDeleteFlag = true
            btnDelete.backgroundColor = UIColor.customDestructiveRed()
            btnDelete.setTitleColor(.white, for: .normal)
        }
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

    @IBAction func btnSecure(_ sender: Any) {
        passwordField.isSecureTextEntry.toggle()
        updateSecureBtn()
    }
}
