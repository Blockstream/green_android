import Foundation
import UIKit

protocol DialogCountlyViewControllerDelegate: AnyObject {
    func didChangeConsent()
}

enum DialogCountlyAction {
    case cancel
    case more
    case deny
    case allow
}

class DialogCountlyViewController: UIViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
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

    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    weak var delegate: DialogCountlyViewControllerDelegate?

    var disableControls = false

    var expandText: String {
        return self.detailsCard.isHidden ? "id_show_details".localized : "id_hide_details".localized
    }

    lazy var blurredView: UIView = {
        let containerView = UIView()
        let blurEffect = UIBlurEffect(style: .dark)
        let customBlurEffectView = CustomVisualEffectView(effect: blurEffect, intensity: 0.4)
        customBlurEffectView.frame = self.view.bounds

        let dimmedView = UIView()
        dimmedView.backgroundColor = .black.withAlphaComponent(0.3)
        dimmedView.frame = self.view.bounds
        containerView.addSubview(customBlurEffectView)
        containerView.addSubview(dimmedView)
        return containerView
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        detailsCard.isHidden = true
        setContent()
        setStyle()

        view.addSubview(blurredView)
        view.sendSubviewToBack(blurredView)

        view.alpha = 0.0
        anchorBottom.constant = -cardView.frame.size.height

        let swipeDown = UISwipeGestureRecognizer(target: self, action: #selector(didSwipe))
            swipeDown.direction = .down
            self.view.addGestureRecognizer(swipeDown)
        let tapToClose = UITapGestureRecognizer(target: self, action: #selector(didTap))
            tappableBg.addGestureRecognizer(tapToClose)

        if disableControls == true {
            btnDeny.isHidden = true
            btnAllow.isHidden = true
        }
        btnDebugID.isHidden = true

#if DEBUG
        if disableControls == true {
            btnDebugID.isHidden = false
        }
#endif

        detailsExpand.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didPressExpandDetails)))
    }

    @objc func didSwipe(gesture: UIGestureRecognizer) {

        if let swipeGesture = gesture as? UISwipeGestureRecognizer {
            switch swipeGesture.direction {
            case .down:
                dismiss(.cancel)
            default:
                break
            }
        }
    }

    @objc func didTap(gesture: UIGestureRecognizer) {

        dismiss(.cancel)
    }

    @objc func didPressExpandDetails() {
        UIView.animate(withDuration: 0.25) {
            self.detailsCard.isHidden = !self.detailsCard.isHidden
            self.lblExpand.text = self.expandText
        }
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

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        handle.cornerRadius = 1.5
        detailsContainer.layer.cornerRadius = 5.0
        btnDeny.setStyle(.outlinedWhite)
        btnAllow.setStyle(.primary)
        btnMore.setStyle(.inline)
        lblHint.textColor = .white.withAlphaComponent(0.6)

        lblTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        [lblHint, lblCollectHint, lblNotCollectHint].forEach {
            $0.font = UIFont.systemFont(ofSize: 12.0, weight: .regular)
        }
        [lblCollectTitle, lblNotCollectTitle, lblExpand].forEach {
            $0.font = UIFont.systemFont(ofSize: 14.0, weight: .semibold)
        }
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        anchorBottom.constant = 0
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
            self.view.layoutIfNeeded()
        }
    }

    func dismiss(_ action: DialogCountlyAction) {
        anchorBottom.constant = -cardView.frame.size.height
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
            self.view.layoutIfNeeded()
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
