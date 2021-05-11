import Foundation
import UIKit
import PromiseKit

protocol DialogRegistryFailViewControllerDelegate: class {
    func didContinue()
}

class DialogRegistryFailViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnCheck: CheckButton!
    @IBOutlet weak var lblUnderstand: UILabel!

    @IBOutlet weak var btnContinue: UIButton!

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogRegistryFailViewControllerDelegate?

    var buttonConstraint: NSLayoutConstraint?

    override func viewDidLoad() {
        super.viewDidLoad()

        lblTitle.text = "Failed to Load Asset Registry"
        lblHint.text = NSLocalizedString("id_the_asset_registry_is_currently", comment: "")
        lblUnderstand.text = "I understand amounts could be incorrect"

        btnContinue.setTitle("Continue", for: .normal)
        btnContinue.cornerRadius = 4.0
        btnContinue.borderWidth = 2.0

        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]

        view.alpha = 0.0
        updateUI()

    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func updateUI() {
        let isOn = btnCheck.isSelected
        btnContinue.isEnabled = isOn

        if isOn {
            btnContinue.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
            btnContinue.borderColor = UIColor.customMatrixGreen()
        } else {
            btnContinue.setTitleColor(UIColor.customGrayLight(), for: .normal)
            btnContinue.borderColor = UIColor.customGrayLight()
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
        updateUI()
    }

    @IBAction func btnContinue(_ sender: Any) {
        dismiss()
    }

}
