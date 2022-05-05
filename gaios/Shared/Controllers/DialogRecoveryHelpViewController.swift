import Foundation
import UIKit
import PromiseKit

protocol DialogRecoveryHelpViewControllerDelegate: class {
    func didTapHelpCenter()
    func didCancel()
}

enum DialogRecoveryAction {
    case helpCenter
    case cancel
}

class DialogRecoveryHelpViewController: KeyboardViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblDesc1: UILabel!

    @IBOutlet weak var btnHelpCenter: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogRecoveryHelpViewControllerDelegate?

    var buttonConstraint: NSLayoutConstraint?

    override func viewDidLoad() {
        super.viewDidLoad()

        view.alpha = 0.0

        setContent()
        setStyle()

        AMan.S.recordView(.help)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]

        btnHelpCenter.cornerRadius = 4.0
        btnHelpCenter.backgroundColor = UIColor.customMatrixGreen()
        btnHelpCenter.setTitleColor(.white, for: .normal)
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_help", comment: "")
        lblHint.text = NSLocalizedString("id_i_typed_all_my_recovery_phrase", comment: "")
        btnHelpCenter.setTitle(NSLocalizedString("id_visit_the_blockstream_help", comment: ""), for: .normal)
        lblDesc1.text = NSLocalizedString("id_1_double_check_all_of_your", comment: "")
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func dismiss(_ action: DialogRecoveryAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                self.delegate?.didCancel()
            case .helpCenter:
                self.delegate?.didTapHelpCenter()
            }
        })
    }

    @IBAction func btnHelpCenter(_ sender: Any) {
        dismiss(.helpCenter)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

}
