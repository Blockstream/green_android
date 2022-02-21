import UIKit
import PromiseKit

class AccountCreateSetNameViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var fieldName: UITextField!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var containerViewAccountType: UIView!
    @IBOutlet weak var lblAccountTypeTitle: UILabel!
    @IBOutlet weak var lblAccountTypeHint: UILabel!

    @IBOutlet weak var containerViewRecoveryKeyType: UIView!
    @IBOutlet weak var lblRecoveryKeyTypeTitle: UILabel!
    @IBOutlet weak var lblRecoveryKeyTypeHint: UILabel!

    @IBOutlet weak var btnNext: UIButton!

    var accountType: AccountType!
    var recoveryKeyType: RecoveryKeyType?

    override func viewDidLoad() {
        super.viewDidLoad()

        fieldName.delegate = self
        setContent()
        setStyle()
        updateUI()
        hideKeyboardWhenTappedAround()
        fieldName.becomeFirstResponder()

        view.accessibilityIdentifier = AccessibilityIdentifiers.AccountCreateSetNameScreen.view
        fieldName.accessibilityIdentifier = AccessibilityIdentifiers.AccountCreateSetNameScreen.nameField
        btnNext.accessibilityIdentifier = AccessibilityIdentifiers.AccountCreateSetNameScreen.nextBtn
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_review_account_information", comment: "")
        lblHint.text = NSLocalizedString("id_account_name", comment: "")
        lblAccountTypeTitle.text = NSLocalizedString("id_account_type", comment: "").uppercased()
        lblAccountTypeHint.text = accountType.name
        lblRecoveryKeyTypeTitle.text = "Recovery key type".uppercased()
        btnNext.setTitle(NSLocalizedString("id_add_new_account", comment: ""), for: .normal)
        containerViewRecoveryKeyType.isHidden = true
        if accountType == .twoOfThree, let recoveryKeyType = recoveryKeyType {
            containerViewRecoveryKeyType.isHidden = false
            switch recoveryKeyType {
            case .hw:
                lblRecoveryKeyTypeHint.text = "Hardware Wallet"
            case .newPhrase:
                lblRecoveryKeyTypeHint.text = "New Phrase"
            case .existingPhrase:
                lblRecoveryKeyTypeHint.text = "Existing Phrase"
            case .publicKey:
                lblRecoveryKeyTypeHint.text = "Public Key"
            }
        }
    }

    func setStyle() {
        fieldName.setLeftPaddingPoints(10.0)
        fieldName.setRightPaddingPoints(10.0)
    }

    func updateUI() {
        if fieldName.text?.count ?? 0 > 2 {
            btnNext.setStyle(.primary)
        } else {
            btnNext.setStyle(.primaryDisabled)
        }
    }

    func next() {
        if let name = fieldName.text {
            createAccount(name: name, type: accountType)
        }
    }

    func dismiss() {
        DispatchQueue.main.async {
            for controller in self.navigationController!.viewControllers as Array {
                if controller.isKind(of: OverviewViewController.self) {
                    if let vc = controller as? OverviewViewController {
                        vc.onAccountChange()
                    }
                    self.navigationController!.popToViewController(controller, animated: true)
                    break
                }
            }
        }
    }

    func createAccount(name: String, type: AccountType) {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try SessionsManager.current.createSubaccount(details: ["name": name, "type": type.rawValue])
        }.then(on: bgq) { call in
            call.resolve()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.dismiss()
        }.catch { e in
            DropAlert().error(message: e.localizedDescription)
            print(e.localizedDescription)
        }
    }

    @IBAction func nameDidChange(_ sender: Any) {
        updateUI()
    }

    @IBAction func btnNext(_ sender: Any) {
        next()
    }

}

extension AccountCreateSetNameViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
}
