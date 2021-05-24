import Foundation
import UIKit
import PromiseKit

protocol DialogTorSingleSigViewControllerDelegate: class {
    func didContinue()
}

class DialogTorSingleSigViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnCheck: CheckButton!
    @IBOutlet weak var lblDontShow: UILabel!

    @IBOutlet weak var btnContinue: UIButton!

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogTorSingleSigViewControllerDelegate?

    var buttonConstraint: NSLayoutConstraint?

    override func viewDidLoad() {
        super.viewDidLoad()

        lblTitle.text = "Tor"
        lblHint.text = "Tor is not available yet for singlesig wallets."
        lblDontShow.text = "Don't show this again"

        btnContinue.setTitle("Continue", for: .normal)
        btnContinue.cornerRadius = 4.0
        btnContinue.borderWidth = 2.0
        btnContinue.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
        btnContinue.borderColor = UIColor.customMatrixGreen()

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

    func dismiss() {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            self.delegate?.didContinue()
        })
    }

    @IBAction func btnCheck(_ sender: Any) {
    }

    @IBAction func btnContinue(_ sender: Any) {

        if btnCheck.isSelected {
            UserDefaults.standard.set(true, forKey: AppStorage.dontShowTorAlert)
        }
        dismiss()
    }

}
