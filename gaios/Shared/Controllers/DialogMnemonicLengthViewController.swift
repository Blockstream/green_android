import Foundation
import UIKit
import PromiseKit

protocol DialogMnemonicLengthViewControllerDelegate: AnyObject {
    func didSelect(_ option: MnemonicLegthOption)
}

enum MnemonicLegthOption: Int {
    case _12 = 12
    case _24 = 24
}

class DialogMnemonicLengthViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnLenght12: UIButton!
    @IBOutlet weak var btnLength24: UIButton!
    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogMnemonicLengthViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_new_recovery_phrase", comment: "")
        lblHint.text = NSLocalizedString("id_choose_recovery_phrase_length", comment: "")
        btnLenght12.setTitle(String(format: NSLocalizedString("id_d_words", comment: ""), 12), for: .normal)
        btnLength24.setTitle(String(format: NSLocalizedString("id_d_words", comment: ""), 24), for: .normal)
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

    func dismiss(_ value: MnemonicLegthOption?) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            if let value = value {
                self.delegate?.didSelect(value)
            }
        })
    }

    @IBAction func btnLenght12(_ sender: Any) {
        dismiss(._12)
    }

    @IBAction func btnLength24(_ sender: Any) {
        dismiss(._24)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(nil)
    }

}
