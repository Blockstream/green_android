import Foundation
import UIKit


protocol DialogDeleteViewControllerDelegate: AnyObject {
    func didDelete(_ index: String?)
    func didCancel()
}

enum WalletDeleteAction {
    case delete
    case cancel
}

class DialogDeleteViewController: UIViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblDesc: UILabel!
    @IBOutlet weak var btnDelete: UIButton!

    var preDeleteFlag = false
    var index: String?

    weak var delegate: DialogDeleteViewControllerDelegate?

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

        setContent()
        setStyle()

        view.addSubview(blurredView)
        view.sendSubviewToBack(blurredView)

        view.alpha = 0.0
        anchorBottom.constant = -cardView.frame.size.height

        let swipeDown = UISwipeGestureRecognizer(target: self, action: #selector(didSwipe))
            swipeDown.direction = .down
            self.view.addGestureRecognizer(swipeDown)
        let tapToClose = UITapGestureRecognizer(target: self, action: #selector(didTapToClose))
            tappableBg.addGestureRecognizer(tapToClose)

        AnalyticsManager.shared.recordView(.deleteWallet)
    }

    deinit {
        print("deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        anchorBottom.constant = 0
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
            self.view.layoutIfNeeded()
        }
    }

    @objc func didTapToClose(gesture: UIGestureRecognizer) {
        dismiss(.cancel)
    }

    func setContent() {
        lblTitle.text = "id_remove_wallet".localized
        lblHint.text = "id_do_you_have_the_backup".localized
        lblDesc.text = "id_be_sure_your_recovery_phrase_is".localized
        btnDelete.setTitle("id_remove_wallet".localized, for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        handle.cornerRadius = 1.5

        lblTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        [lblHint, lblDesc].forEach {
            $0.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        }

        btnDelete.cornerRadius = 4.0
        btnDelete.setTitleColor(UIColor.customDestructiveRed(), for: .normal)
        btnDelete.borderWidth = 2.0
        btnDelete.borderColor = UIColor.customDestructiveRed()
    }

    func dismiss(_ action: WalletDeleteAction) {
        anchorBottom.constant = -cardView.frame.size.height
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
            self.view.layoutIfNeeded()
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                self.delegate?.didCancel()
            case .delete:
                self.delegate?.didDelete(self.index)
            }
        })
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

    @IBAction func btnDelete(_ sender: Any) {
        if preDeleteFlag {
            dismiss(.delete)
        } else {
            preDeleteFlag = true
            btnDelete.backgroundColor = UIColor.customDestructiveRed()
            btnDelete.setTitleColor(.white, for: .normal)
        }
    }
}
