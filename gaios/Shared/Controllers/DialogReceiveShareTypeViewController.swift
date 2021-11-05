import Foundation
import UIKit
import PromiseKit

protocol DialogReceiveShareTypeViewControllerDelegate: AnyObject {
    func didSelect(_ option: ReceiveShareOption)
}

enum ReceiveShareOption {
    case address
    case qr
    case cancel
}

class DialogReceiveShareTypeViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var btnShareAddress: UIButton!
    @IBOutlet weak var btnShareQR: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    var isLiquid = false

    weak var delegate: DialogReceiveShareTypeViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_share", comment: "")
        btnShareAddress.setTitle(NSLocalizedString("id_address", comment: ""), for: .normal)
        btnShareQR.setTitle(NSLocalizedString("id_qr_code", comment: ""), for: .normal)
        btnShareQR.isHidden = true
        if #available(iOS 13.0, *) {
            btnShareQR.isHidden = false
        }
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

    func dismiss(_ option: ReceiveShareOption) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            self.delegate?.didSelect(option)
        })
    }

    @IBAction func btnShareAddress(_ sender: Any) {
        dismiss(.address)
    }

    @IBAction func btnShareQR(_ sender: Any) {
        dismiss(.qr)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

}
