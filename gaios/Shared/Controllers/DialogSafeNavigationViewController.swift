import Foundation
import UIKit
import PromiseKit

enum SafeNavigationAction {
    case authorize
    case cancel
    case copy
}

class DialogSafeNavigationViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var btnContinue: UIButton!
    @IBOutlet weak var btnCancel: UIButton!
    @IBOutlet weak var btnCopy: UIButton!

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    var onSelect: ((SafeNavigationAction) -> Void)?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0
    }

    func setContent() {

        lblTitle.text = NSLocalizedString("id_tor", comment: "")
        lblHint.text = NSLocalizedString("id_you_have_tor_enabled_are_you", comment: "")

        btnContinue.setTitle(NSLocalizedString("id_continue", comment: ""), for: .normal)
        btnCancel.setTitle(NSLocalizedString("id_cancel", comment: ""), for: .normal)
        btnCopy.setTitle(NSLocalizedString("id_copy_to_clipboard", comment: ""), for: .normal)
    }

    func setStyle() {
        btnContinue.setStyle(.primary)
        btnCancel.setStyle(.outlined)
        btnCopy.setStyle(.outlined)
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func dismiss(_ action: SafeNavigationAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: {
                self.onSelect?(action)
            })
        })
    }

    @IBAction func btnContinue(_ sender: Any) {
        dismiss(.authorize)
    }

    @IBAction func btnCancel(_ sender: Any) {
        dismiss(.cancel)
    }

    @IBAction func btnCopy(_ sender: Any) {
        dismiss(.copy)
    }
}
