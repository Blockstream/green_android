import Foundation
import UIKit
import PromiseKit

protocol DialogCountlyConsentViewControllerDelegate: AnyObject {
    func didClose()
}

enum DialogCountlyConsentAction {
    case cancel
    case more
    case deny
    case allow
}

class DialogCountlyConsentViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var detailsContainer: UIStackView!
    @IBOutlet weak var detailsExpand: UIView!
    @IBOutlet weak var expandArrow: UIImageView!
    @IBOutlet weak var lblExpand: UILabel!
    @IBOutlet weak var detailsCard: UIView!

    @IBOutlet weak var lblCollectTitle: UILabel!
    @IBOutlet weak var lblCollectHint: UILabel!
    @IBOutlet weak var lblNotCollectTitle: UILabel!
    @IBOutlet weak var lblNotCollectHint: UILabel!
    @IBOutlet weak var btnMore: UIButton!

    @IBOutlet weak var btnDeny: UIButton!
    @IBOutlet weak var btnAllow: UIButton!

    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogCountlyConsentViewControllerDelegate?

    var expandText: String {
        return self.detailsCard.isHidden ? "Show details" : "Hide details"
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        view.alpha = 0.0

        detailsCard.isHidden = true
        setContent()
        setStyle()

        detailsExpand.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressExpandDetails)))
    }

    @objc func didPressExpandDetails() {
        let clock = self.detailsCard.isHidden ? 1 : -1
        UIView.animate(withDuration: 0.25) {
            self.detailsCard.isHidden = !self.detailsCard.isHidden

            self.expandArrow.transform = self.expandArrow.transform.rotated(by: CGFloat(clock) * .pi / 2)
            self.lblExpand.text = self.expandText
        }
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        detailsContainer.layer.cornerRadius = 5.0
        btnDeny.setStyle(.outlined)
        btnAllow.setStyle(.primary)
        btnMore.setStyle(.inline)
    }

    func setContent() {
        lblTitle.text = "Help Green improve"
        lblHint.text = "If you agree, Green will collect limited usage data to optimize the user experience. No sensitive user or wallet info is collected."
        btnDeny.setTitle("Don’t collect data", for: .normal)
        btnAllow.setTitle("Allow data collection", for: .normal)
        lblExpand.text = self.expandText

        lblCollectTitle.text = "What's collected"

        lblCollectHint.text = "Pseudonymous identifier, country\nPage visits, button presses, general app configuration\nOS & app version, loading times, crashes"
        lblNotCollectTitle.text = "What's NOT collected"
        lblNotCollectHint.text = "Recovery phrases, key material, addresses, balances\nUser contact info, IP address, detailed location"
        btnMore.setTitle("Learn more", for: .normal)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func dismiss(_ action: DialogCountlyConsentAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                print("cancel")
            case .more:
                print("more")
            case .deny:
                if AMan.S.consent != .denied {
                    AMan.S.consent = .denied
                }
            case .allow:
                if AMan.S.consent != .authorized {
                    AMan.S.consent = .authorized
                }
            }
        })
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }

    @IBAction func btnMore(_ sender: Any) {
        UIApplication.shared.open(ExternalUrls.analyticsReadMore, options: [:], completionHandler: nil)
    }

    @IBAction func btnDeny(_ sender: Any) {
        dismiss(.deny)
    }

    @IBAction func btnAllow(_ sender: Any) {
        dismiss(.allow)
    }

}
