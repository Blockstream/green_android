import Foundation
import UIKit
import PromiseKit

protocol DialogReceiveMoreOptionsViewControllerDelegate: AnyObject {
    func didSelect(_ action: ReceiveOptionAction)
}

enum ReceiveOptionAction {
    case requestAmount
    case sweep
    case cancel
}

class DialogReceiveMoreOptionsViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var btnRequestAmount: UIButton!
    @IBOutlet weak var btnSweep: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    var isLiquid = false

    weak var delegate: DialogReceiveMoreOptionsViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0
    }

    func setContent() {
        lblTitle.text = "More Options"
        btnRequestAmount.setTitle("Request Amount", for: .normal)
        btnSweep.setTitle("Sweep from Paper Wallet", for: .normal)
        btnSweep.isHidden = isLiquid
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

    func dismiss(_ action: ReceiveOptionAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            self.delegate?.didSelect(action)
        })
    }

    @IBAction func btnRequestAmount(_ sender: Any) {
        dismiss(.requestAmount)
    }

    @IBAction func btnSweep(_ sender: Any) {
        dismiss(.sweep)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

}
