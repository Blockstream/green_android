import Foundation
import UIKit
import PromiseKit

protocol DialogWalletDeleteViewControllerDelegate: class {
    func didDelete()
    func didCancel()
}

enum WalletDeleteAction {
    case delete
    case cancel
}

class DialogWalletDeleteViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblDesc: UILabel!

    @IBOutlet weak var btnDelete: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogWalletDeleteViewControllerDelegate?

    var buttonConstraint: NSLayoutConstraint?
    var preDeleteFlag = false

    override func viewDidLoad() {
        super.viewDidLoad()

        lblTitle.text = NSLocalizedString("id_remove_wallet", comment: "")
        lblHint.text = NSLocalizedString("id_do_you_have_the_backup", comment: "")
        lblDesc.text = NSLocalizedString("id_be_sure_your_recovery_phrase_is", comment: "")
        btnDelete.setTitle(NSLocalizedString("id_remove_wallet", comment: ""), for: .normal)
        btnDelete.cornerRadius = 4.0
        btnDelete.setTitleColor(UIColor.customDestructiveRed(), for: .normal)
        btnDelete.borderWidth = 2.0
        btnDelete.borderColor = UIColor.customDestructiveRed()

        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]

        view.alpha = 0.0

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
                self.delegate?.didDelete()
            }
        })
    }

    @IBAction func btnDelete(_ sender: Any) {
        if preDeleteFlag {
            dismiss(.delete)
        } else {
            preDeleteFlag = true
            btnDelete.backgroundColor = UIColor.customDestructiveRed()
            btnDelete.setTitleColor(.white, for: .normal)
        }
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

}
