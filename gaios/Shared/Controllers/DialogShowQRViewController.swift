import Foundation
import UIKit

class DialogShowQRViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var qrImgView: UIImageView!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    var qrDialogInfo: QRDialogInfo?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0
    }

    func setContent() { }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
    }

    override func viewDidAppear(_ animated: Bool) {
        lblTitle.text = qrDialogInfo?.title ?? ""
        lblHint.text = qrDialogInfo?.hint ?? ""
        qrImgView.image = QRImageGenerator.imageForTextWhite(text: qrDialogInfo?.item ?? "", frame: qrImgView.frame)
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
