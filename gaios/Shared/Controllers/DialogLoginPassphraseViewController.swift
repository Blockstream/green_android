import Foundation
import UIKit
import PromiseKit

protocol DialogLoginPassphraseViewControllerDelegate: AnyObject {
    func didConfirm(passphrase: String, alwaysAsk: Bool)
}

enum LoginPassphraseAction {
    case confirm
    case cancel
}

class DialogLoginPassphraseViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var fieldPassphrase: UITextField!
    @IBOutlet weak var lblHint1: UILabel!
    @IBOutlet weak var lblHint2: UILabel!
    @IBOutlet weak var btnLearn: UIButton!
    @IBOutlet weak var lblAskTitle: UILabel!
    @IBOutlet weak var lblAskHint: UILabel!
    @IBOutlet weak var switchAsk: UISwitch!
    @IBOutlet weak var btnClear: UIButton!
    @IBOutlet weak var btnConfirm: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var btnsStack: UIStackView!

    weak var delegate: DialogLoginPassphraseViewControllerDelegate?

    var buttonConstraint: NSLayoutConstraint?
    var isAlwaysAsk: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0
        switchAsk.isOn = isAlwaysAsk
    }

    func setContent() {

        lblTitle.text = "Login with BIP39 Passphrase"
        fieldPassphrase.attributedPlaceholder = NSAttributedString(string: "Passphrase", attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        lblHint1.text = "Each different passphrase you use will generate a new wallet"
        lblHint2.text = "Remomber you can only restore this wallet with both your seed and the passphrase"
        btnLearn.setTitle("Learn More", for: .normal)
        lblAskTitle.text = "Always ask"
        lblAskHint.text = "By turning this off you won't be asked for a passphrase on your next login. This wallet will still be accessible through your menu on your PIN screen"
        btnClear.setTitle(NSLocalizedString("id_clear", comment: ""), for: .normal)
        btnConfirm.setTitle(NSLocalizedString("id_ok", comment: ""), for: .normal)
    }

    func setStyle() {
        btnClear.setStyle(.outlined)
        btnConfirm.setStyle(.primaryDisabled)
        fieldPassphrase.setLeftPaddingPoints(10.0)
        fieldPassphrase.setRightPaddingPoints(10.0)
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        btnLearn.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
        fieldPassphrase.becomeFirstResponder()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        dismissDisabled = true
        btnConfirm.setStyle(.primaryDisabled)
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.buttonConstraint = self.btnsStack.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height)
            self.buttonConstraint?.isActive = true
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
        })
    }

    func dismiss(_ action: LoginPassphraseAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                break
            case .confirm:
                if let passphrase = self.fieldPassphrase.text {
                    self.delegate?.didConfirm(passphrase: passphrase, alwaysAsk: self.switchAsk.isOn)
                }
            }
        })
    }

    @IBAction func btnLearnMore(_ sender: Any) {
        UIApplication.shared.open(ExternalUrls.passphraseReadMore, options: [:], completionHandler: nil)
    }

    @IBAction func passphraseDidChange(_ sender: Any) {
        if let passphrase = fieldPassphrase.text,
           passphrase.count > 0 &&
            passphrase.count <= 100 &&
            passphrase.first != " " &&
            passphrase.last != " " {
            btnConfirm.setStyle(.primary)
        }
    }

    @IBAction func askDidChange(_ sender: Any) {
        btnConfirm.setStyle(.primary)
    }

    @IBAction func btnClear(_ sender: Any) {
        fieldPassphrase.text = ""
        switchAsk.isOn = isAlwaysAsk
        btnConfirm.setStyle(.primaryDisabled)
    }

    @IBAction func btnConfirm(_ sender: Any) {
        dismiss(.confirm)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

}
