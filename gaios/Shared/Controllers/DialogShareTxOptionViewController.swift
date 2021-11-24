import Foundation
import UIKit
import PromiseKit

protocol DialogShareTxOptionViewControllerDelegate: AnyObject {
    func didSelect(_ option: TxShareOption)
}

enum TxShareOption {
    case confidential
    case nonConfidential
    case unblindingData
}

class DialogShareTxOptionViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var btnConfidential: UIButton!
    @IBOutlet weak var btnNonConfidential: UIButton!
    @IBOutlet weak var btnUnblindingData: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogShareTxOptionViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_share", comment: "")
        btnConfidential.setTitle(NSLocalizedString("id_confidential_transaction", comment: ""), for: .normal)
        btnNonConfidential.setTitle(NSLocalizedString("id_non_confidential_transaction", comment: ""), for: .normal)
        btnUnblindingData.setTitle(NSLocalizedString("id_unblinding_data", comment: ""), for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func dismiss(_ option: TxShareOption?) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            if let opt = option {
                self.delegate?.didSelect(opt)
            }
        })
    }

    @IBAction func btnConfidential(_ sender: Any) {
        dismiss(.confidential)
    }

    @IBAction func btnNonConfidential(_ sender: Any) {
        dismiss(.nonConfidential)
    }

    @IBAction func btnUnblindingData(_ sender: Any) {
        dismiss(.unblindingData)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(nil)
    }

}
