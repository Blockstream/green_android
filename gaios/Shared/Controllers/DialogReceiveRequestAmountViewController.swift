import Foundation
import UIKit
import PromiseKit

protocol DialogReceiveRequestAmountViewControllerDelegate: AnyObject {
    func didConfirm(_ amount: String)
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

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0

        updateUI()
    }

    func setContent() {
        lblTitle.text = "Request Amount"
        lblHint.text = "Amount in BTC"
        btnClear.setTitle("Clear", for: .normal)
        btnConfirm.setTitle("OK", for: .normal)
        amountTextField.attributedPlaceholder = NSAttributedString(
            string: "Receive Amount",
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
    }

    func setStyle() {
        btnClear.setStyle(.outlined)
        btnConfirm.setStyle(.primaryDisabled)
        amountTextField.setLeftPaddingPoints(10.0)
        amountTextField.setRightPaddingPoints(10.0)
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
        amountTextField.becomeFirstResponder()
    }

    func updateUI() {
        amountTextField.text?.count ?? 0 > 2 ? btnConfirm.setStyle(.primary) : btnConfirm.setStyle(.primaryDisabled)
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
                self.delegate?.didConfirm(self.amountTextField.text ?? "")
            }
        })
    }

    @IBAction func amountDidChange(_ sender: Any) {
        updateUI()
    }

    @IBAction func btnFiat(_ sender: Any) {
        print("to do")
    }

    @IBAction func btnClear(_ sender: Any) {
        self.amountTextField.text = ""
    }

    @IBAction func btnConfirm(_ sender: Any) {
        dismiss(.confirm)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

}
