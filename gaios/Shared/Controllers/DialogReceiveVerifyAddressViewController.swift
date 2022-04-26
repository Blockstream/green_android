import Foundation
import UIKit
import PromiseKit

class DialogReceiveVerifyAddressViewController: UIViewController {

    @IBOutlet weak var icArrow: UIImageView!
    @IBOutlet weak var icWallet: UIImageView!
    @IBOutlet weak var lblVerify: UILabel!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    var isLedger = false

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0

        if isLedger {
            icWallet.image = UIImage(named: "ic_hww_ledger")
        } else {
            icWallet.image = UIImage(named: "ic_hww_jade")
        }
    }

    func setContent() {
        lblVerify.text = NSLocalizedString("id_please_verify_that_the_address", comment: "")
        icArrow.image = UIImage(named: "ic_hww_arrow")!.maskWithColor(color: UIColor.customMatrixGreen())
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

    func dismiss() {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
        })
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss()
    }
}
