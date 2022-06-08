import Foundation
import UIKit
import PromiseKit

protocol DialogCountlyConsentViewControllerDelegate: AnyObject {
    func didChangeConsent()
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
    @IBOutlet weak var btnDebugID: UIButton!

    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogCountlyConsentViewControllerDelegate?

    var disableControls = false

    var expandText: String {
        return self.detailsCard.isHidden ? NSLocalizedString("id_show_details", comment: "") : NSLocalizedString("id_hide_details", comment: "")
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        view.alpha = 0.0

        detailsCard.isHidden = true
        setContent()
        setStyle()

        if disableControls == true {
            btnDeny.isHidden = true
            btnAllow.isHidden = true
            btnDismiss.isHidden = false
        }
        btnDebugID.isHidden = true

#if DEBUG
        if disableControls == true {
            btnDebugID.isHidden = false
        }
#endif

        detailsExpand.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressExpandDetails)))

        view.accessibilityIdentifier = AccessibilityIdentifiers.DialogAnalyticsConsentScreen.view
        btnDeny.accessibilityIdentifier = AccessibilityIdentifiers.DialogAnalyticsConsentScreen.denyBtn
        btnAllow.accessibilityIdentifier = AccessibilityIdentifiers.DialogAnalyticsConsentScreen.allowBtn
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
        lblTitle.text = NSLocalizedString("id_help_green_improve", comment: "")
        lblHint.text = NSLocalizedString("id_if_you_agree_green_will_collect", comment: "")
        btnDeny.setTitle(NSLocalizedString("id_dont_collect_data", comment: ""), for: .normal)
        btnAllow.setTitle(NSLocalizedString("id_allow_data_collection", comment: ""), for: .normal)
        lblExpand.text = self.expandText

        lblCollectTitle.text = NSLocalizedString("id_whats_collected", comment: "")

        let collectStr = NSLocalizedString("id_pseudonymous_identifier_country", comment: "") + "\n" + NSLocalizedString("id_page_visits_button_presses", comment: "") + "\n" + NSLocalizedString("id_os__app_version_loading_times", comment: "")

        lblCollectHint.text = collectStr

        lblNotCollectTitle.text = NSLocalizedString("id_whats_not_collected", comment: "")

        let notCollectStr = NSLocalizedString("id_recovery_phrases_key_material", comment: "") + "\n" + NSLocalizedString("id_user_contact_info_ip_address", comment: "")

        lblNotCollectHint.text = notCollectStr
        btnMore.setTitle(NSLocalizedString("id_learn_more", comment: ""), for: .normal)
        btnDebugID.setTitle(NSLocalizedString("id_copy_device_id", comment: ""), for: .normal)
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
                if AnalyticsManager.shared.consent != .denied {
                    AnalyticsManager.shared.consent = .denied
                    self.delegate?.didChangeConsent()
                }
            case .allow:
                if AnalyticsManager.shared.consent != .authorized {
                    AnalyticsManager.shared.consent = .authorized
                    self.delegate?.didChangeConsent()
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

    @IBAction func btnDebugID(_ sender: Any) {
        var msg = "ID not available"
        if let uuid = UserDefaults.standard.string(forKey: AppStorage.analyticsUUID) {
            UIPasteboard.general.string = uuid
            msg = NSLocalizedString("UUID copied to clipboard", comment: "")
        }
        DropAlert().info(message: msg, delay: 1.0)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }
}
