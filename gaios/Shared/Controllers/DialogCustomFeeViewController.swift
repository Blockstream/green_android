import Foundation
import UIKit
import PromiseKit

protocol DialogCustomFeeViewControllerDelegate: AnyObject {
    func didSave(fee: String, index: Int?)
}

enum SuctomFeeAction {
    case save
    case cancel
}

class DialogCustomFeeViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var feeTextField: UITextField!
    @IBOutlet weak var btnSave: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogCustomFeeViewControllerDelegate?
    var index: Int?

    var buttonConstraint: NSLayoutConstraint?

    override func viewDidLoad() {
        super.viewDidLoad()

        lblTitle.text = "Custom Fee"

        btnSave.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)
        btnSave.cornerRadius = 4.0
        feeTextField.placeholder = ""
        feeTextField.setLeftPaddingPoints(10.0)
        feeTextField.setRightPaddingPoints(10.0)

        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]

        view.alpha = 0.0

        updateUI()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
        feeTextField.becomeFirstResponder()
    }

    func updateUI() {
        if feeTextField.text?.count ?? 0 > 2 {
            btnSave.isEnabled = true
            btnSave.backgroundColor = UIColor.customMatrixGreen()
            btnSave.setTitleColor(.white, for: .normal)
        } else {
            btnSave.isEnabled = false
            btnSave.backgroundColor = UIColor.customBtnOff()
            btnSave.setTitleColor(UIColor.customGrayLight(), for: .normal)
        }
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

    func dismiss(_ action: WalletNameAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                break
            case .save:
                self.delegate?.didSave(fee: self.feeTextField.text ?? "", index: self.index)
            }
        })
    }

    @IBAction func feeDidChange(_ sender: Any) {
        updateUI()
    }

    @IBAction func btnSave(_ sender: Any) {
        dismiss(.save)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

}
