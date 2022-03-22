import Foundation
import UIKit
import PromiseKit

protocol DialogCustomFeeViewControllerDelegate: AnyObject {
    func didSave(fee: UInt64?)
}

enum SuctomFeeAction {
    case save
    case cancel
}

class DialogCustomFeeViewController: KeyboardViewController {

    @IBOutlet weak var lblCustomFeeTitle: UILabel!
    @IBOutlet weak var lblCustomFeeHint: UILabel!
    @IBOutlet weak var feeTextField: UITextField!
    @IBOutlet weak var btnSave: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogCustomFeeViewControllerDelegate?
    var storedFeeRate: UInt64?
    var buttonConstraint: NSLayoutConstraint?

    private var minFeeRate: UInt64 = {
        guard let estimates = getFeeEstimates() else {
            let defaultMinFee = AccountsManager.shared.current?.gdkNetwork?.liquid ?? false ? 100 : 1000
            return UInt64(defaultMinFee)
        }
        return estimates[0]
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        lblCustomFeeTitle.text = NSLocalizedString("id_set_custom_fee_rate", comment: "")
        lblCustomFeeHint.text = "satoshi / byte"
        btnSave.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)
        btnSave.cornerRadius = 4.0
        feeTextField.placeholder = ""
        feeTextField.setLeftPaddingPoints(10.0)
        feeTextField.setRightPaddingPoints(10.0)

        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]

        view.alpha = 0.0

        feeTextField.keyboardType = .decimalPad
        feeTextField.attributedPlaceholder = NSAttributedString(string: String(Double(storedFeeRate ?? 1000) / 1000),
                                                                attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        updateUI()

        view.accessibilityIdentifier = AccessibilityIdentifiers.DialogCustomFeeScreen.view
        feeTextField.accessibilityIdentifier = AccessibilityIdentifiers.DialogCustomFeeScreen.feeField
        btnSave.accessibilityIdentifier = AccessibilityIdentifiers.DialogCustomFeeScreen.saveBtn
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
        feeTextField.becomeFirstResponder()
    }

    func updateUI() {
        if feeTextField.text?.count ?? 0 > 0 {
            btnSave.isEnabled = true
            btnSave.backgroundColor = UIColor.customMatrixGreen()
            btnSave.setTitleColor(.white, for: .normal)
        } else {
            btnSave.isEnabled = false
            btnSave.backgroundColor = UIColor.customBtnOff()
            btnSave.setTitleColor(UIColor.customGrayLight(), for: .normal)
        }
    }

    func validate() {

        var feeRate: UInt64
        if let storedFeeRate = storedFeeRate {
            feeRate = storedFeeRate
        } else if let settings = SessionsManager.current?.settings {
            feeRate = UInt64(settings.customFeeRate ?? self.minFeeRate)
        } else {
            feeRate = self.minFeeRate
        }
        guard var amountText = feeTextField.text else { return }
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else { return }
        if 1000 * number >= Double(UInt64.max) { return }
        feeRate = UInt64(1000 * number)
        if feeRate < self.minFeeRate {
            DropAlert().warning(message: String(format: NSLocalizedString("id_fee_rate_must_be_at_least_s", comment: ""), String(self.minFeeRate)))
            return
        }
        dismiss(.save, feeRate: feeRate)
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.buttonConstraint = self.btnSave.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height)
            self.buttonConstraint?.isActive = true
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.buttonConstraint?.isActive = false
        })
    }

    func dismiss(_ action: WalletNameAction, feeRate: UInt64?) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                break
            case .save:
                self.delegate?.didSave(fee: feeRate)
            }
        })
    }

    @IBAction func feeDidChange(_ sender: Any) {
        updateUI()
    }

    @IBAction func btnSave(_ sender: Any) {
        validate()
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel, feeRate: nil)
    }

}
