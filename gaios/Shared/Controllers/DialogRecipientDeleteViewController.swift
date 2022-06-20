import Foundation
import UIKit

protocol DialogRecipientDeleteViewControllerDelegate: AnyObject {
    func didDelete(_ index: Int)
    func didCancel()
}

enum RecipientDeleteAction {
    case delete
    case cancel
}

class DialogRecipientDeleteViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var btnDelete: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogRecipientDeleteViewControllerDelegate?

    var buttonConstraint: NSLayoutConstraint?
    var index: Int?

    override func viewDidLoad() {
        super.viewDidLoad()

        lblTitle.text = "Remove recipient"
        lblHint.text = NSLocalizedString("id_are_you_sure_you_want_to_remove", comment: "")
        btnDelete.setTitle(NSLocalizedString("id_remove", comment: ""), for: .normal)
        btnDelete.cornerRadius = 4.0
        btnDelete.backgroundColor = UIColor.customDestructiveRed()
        btnDelete.setTitleColor(.white, for: .normal)

        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]

        view.alpha = 0.0
        view.accessibilityIdentifier = AccessibilityIdentifiers.DialogWalletDeleteScreen.view
        btnDelete.accessibilityIdentifier = AccessibilityIdentifiers.DialogWalletDeleteScreen.deleteBtn
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func dismiss(_ action: WalletDeleteAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                self.delegate?.didCancel()
            case .delete:
                if let rec = self.index {
                    self.delegate?.didDelete(rec)
                }
            }
        })
    }

    @IBAction func btnDelete(_ sender: Any) {
        dismiss(.delete)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

}
