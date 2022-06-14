import Foundation
import UIKit
import PromiseKit

protocol DialogNoteViewControllerDelegate: AnyObject {
    func didSave(_ note: String)
    func didCancel()
}

enum NoteAction {
    case save
    case cancel
}

class DialogNoteViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var nameTextField: UITextField!

    @IBOutlet weak var btnSave: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogNoteViewControllerDelegate?

    var buttonConstraint: NSLayoutConstraint?
    var isAccountRename = false
    var prefill: String?

    override func viewDidLoad() {
        super.viewDidLoad()

        lblTitle.text = NSLocalizedString("id_my_notes", comment: "")

        btnSave.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)
        btnSave.cornerRadius = 4.0
        nameTextField.placeholder = ""
        nameTextField.setLeftPaddingPoints(10.0)
        nameTextField.setRightPaddingPoints(10.0)

        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]

        view.alpha = 0.0

        updateUI()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        nameTextField.text = prefill
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
        nameTextField.becomeFirstResponder()
    }

    func updateUI() {
        if nameTextField.text?.count ?? 0 > 2 {
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
                self.delegate?.didCancel()
            case .save:
                self.delegate?.didSave(self.nameTextField.text ?? "")
            }
        })
    }

    @IBAction func noteDidChange(_ sender: Any) {
        updateUI()
    }

    @IBAction func btnSave(_ sender: Any) {
        dismiss(.save)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

}
