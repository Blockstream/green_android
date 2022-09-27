import Foundation
import UIKit
import PromiseKit

protocol DialogReceiveRequestAmountViewControllerDelegate: AnyObject {
    func didConfirm(satoshi: Int64?)
    func didCancel()
}

enum ReceiveAmountAction {
    case confirm
    case cancel
}

class DialogReceiveRequestAmountViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var amountTextField: UITextField!
    @IBOutlet weak var btnFiat: UIButton!
    @IBOutlet weak var btnClear: UIButton!
    @IBOutlet weak var btnConfirm: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var btnsStack: UIStackView!

    weak var delegate: DialogReceiveRequestAmountViewControllerDelegate?

    var buttonConstraint: NSLayoutConstraint?
    var isAccountRename = false
    var selectedType = TransactionBaseType.BTC
    var prefill: Int64?
    var wallet: WalletItem?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0
        amountTextField.attributedPlaceholder = NSAttributedString(string: "0.00".localeFormattedString(2), attributes: [NSAttributedString.Key.foregroundColor: UIColor.white])
        if let satoshi = prefill {
            if let (amount, _) = Balance.fromSatoshi(satoshi)?.toValue() {
                amountTextField.text = "\(amount)"
            }
        }

        view.accessibilityIdentifier = AccessibilityIdentifiers.DialogReceiveRequestAmountScreen.view
        amountTextField.accessibilityIdentifier = AccessibilityIdentifiers.DialogReceiveRequestAmountScreen.amountField
        btnConfirm.accessibilityIdentifier = AccessibilityIdentifiers.DialogReceiveRequestAmountScreen.confirmBtn

        AnalyticsManager.shared.recordView(.requestAmount, sgmt: AnalyticsManager.shared.subAccSeg(AccountsManager.shared.current, walletType: wallet?.type))
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_request_amount", comment: "")
        lblHint.text = ""
        btnClear.setTitle(NSLocalizedString("id_clear", comment: ""), for: .normal)
        btnConfirm.setTitle(NSLocalizedString("id_ok", comment: ""), for: .normal)
        amountTextField.attributedPlaceholder = NSAttributedString(
            string: NSLocalizedString("id_amount", comment: ""),
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
    }

    func setStyle() {
        btnClear.setStyle(.outlined)
        btnConfirm.setStyle(.primaryDisabled)
        amountTextField.setLeftPaddingPoints(10.0)
        amountTextField.setRightPaddingPoints(10.0)
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        btnFiat.layer.cornerRadius = 4.0
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
        amountTextField.becomeFirstResponder()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        reload()
    }

    func reload() {
        getSatoshi() != nil ? btnConfirm.setStyle(.primary) : btnConfirm.setStyle(.primaryDisabled)
        setButton()
        updateEstimate()
    }

    func updateEstimate() {
        let satoshi = getSatoshi() ?? 0
        let tag = selectedType == TransactionBaseType.BTC ? "fiat": "btc"
        if selectedType == TransactionBaseType.BTC {
            let (amount, denom) = Balance.fromSatoshi(satoshi)?.toFiat() ?? ("", "")
            lblHint.text = "≈ \(amount) \(denom)"
        } else {
            let (amount, denom) = Balance.fromSatoshi(satoshi)?.toValue() ?? ("", "")
            lblHint.text = "≈ \(amount) \(denom)"
        }
    }

    func setButton() {
        guard let settings = SessionsManager.current?.settings else {
            return
        }
        if selectedType == TransactionBaseType.BTC {
            btnFiat.setTitle(settings.denomination.string, for: UIControl.State.normal)
            btnFiat.backgroundColor = UIColor.customMatrixGreen()
            btnFiat.setTitleColor(UIColor.white, for: UIControl.State.normal)
        } else {
            let isMainnet = AccountsManager.shared.current?.gdkNetwork?.mainnet ?? true
            btnFiat.setTitle(isMainnet ? settings.getCurrency() : "FIAT", for: UIControl.State.normal)
            btnFiat.backgroundColor = UIColor.clear
            btnFiat.setTitleColor(UIColor.white, for: UIControl.State.normal)
        }
    }

    func getSatoshi() -> Int64? {
        var amountText = amountTextField.text!
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else { return nil }
        if selectedType == TransactionBaseType.BTC {
            return Balance.fromDenomination(amountText)?.satoshi
        } else {
            return Balance.fromFiat(amountText)?.satoshi
        }
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

    func dismiss(_ action: ReceiveAmountAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                self.delegate?.didCancel()
            case .confirm:
                self.delegate?.didConfirm(satoshi: self.getSatoshi())
            }
        })
    }

    @IBAction func amountDidChange(_ sender: Any) {
        reload()
    }

    @IBAction func btnFiat(_ sender: Any) {
        let satoshi = getSatoshi() ?? 0
        if let balance = Balance.fromSatoshi(satoshi) {
            let (amount, _) = selectedType == TransactionBaseType.BTC ? balance.toDenom() : balance.toFiat()
            if amount == nil {
                showError(NSLocalizedString("id_your_favourite_exchange_rate_is", comment: ""))
                return
            }
        }
        if selectedType == TransactionBaseType.BTC {
            selectedType = TransactionBaseType.FIAT
        } else {
            selectedType = TransactionBaseType.BTC
        }
        if let balance = Balance.fromSatoshi(satoshi) {
            let (amountStr, _) = selectedType == TransactionBaseType.BTC ? balance.toDenom() : balance.toFiat()
            let amount = Double(amountStr)
            amountTextField.text = amountStr
            if amount == 0.0 {
                amountTextField.text = ""
            }
        }
        reload()
    }

    @IBAction func btnClear(_ sender: Any) {
        amountTextField.text = ""
        dismiss(.confirm)
    }

    @IBAction func btnConfirm(_ sender: Any) {
        dismiss(.confirm)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

}
