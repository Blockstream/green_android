import Foundation
import UIKit
import gdk

enum TwoFAInfoAction {
    case retry
    case support
    case cancel
}

class TwoFAInfoViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    
    @IBOutlet weak var btnRetry: UIButton!
    @IBOutlet weak var btnSupport: UIButton!
    @IBOutlet weak var btnCancel: UIButton!
    
    var onRetry: (() -> Void)?
    var onSupport: (() -> Void)?
    var onCancel: (() -> Void)?
    
    override func viewDidLoad() {
        super.viewDidLoad()

        setStyle()
        setContent()
        view.alpha = 0.0
        btnRetry.isHidden = true
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func setContent() {
        lblTitle.text = "Are you not receiving your 2FA code?".localized
        lblHint.text = "Try again, using another 2FA method.".localized
        btnRetry.setTitle("Select a different method".localized, for: .normal)
        btnSupport.setTitle("Contact Support".localized, for: .normal)
        btnCancel.setTitle("id_cancel".localized, for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        cardView.borderWidth = 1.0
        cardView.borderColor = .white.withAlphaComponent(0.05)
        lblTitle.setStyle(.txtBigger)
        lblHint.setStyle(.txtCard)
        btnRetry.setStyle(.primary)
        btnSupport.setStyle(.outlinedWhite)
        btnCancel.setStyle(.inline)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func dismiss(_ action: TwoFAInfoAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: {
                switch action {
                case .retry:
                    self.onRetry?()
                case .support:
                    self.onSupport?()
                case .cancel:
                    self.onCancel?()
                }
            })
        })
    }

    @IBAction func btnRetry(_ sender: Any) {
        dismiss(.retry)
    }

    @IBAction func btnSupport(_ sender: Any) {
        dismiss(.support)
    }

    @IBAction func btnCancel(_ sender: Any) {
        dismiss(.cancel)
    }
}
