import Foundation
import UIKit
import PromiseKit

protocol DialogPassphraseViewControllerDelegate: AnyObject {
    func didConfirm(passphrase: String, alwaysAsk: Bool)
}

enum PassphraseAction {
    case confirm
    case cancel
}

class DialogPassphraseViewController: KeyboardViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var fieldPassphrase: UITextField!
    @IBOutlet weak var lblHint1: UILabel!
    @IBOutlet weak var lblAskTitle: UILabel!
//    @IBOutlet weak var switchAsk: UISwitch!
    @IBOutlet weak var btnConfirm: UIButton!
    @IBOutlet weak var btnClear: UIButton!
    @IBOutlet weak var btnsStack: UIStackView!
    @IBOutlet weak var askView: UIView!
    @IBOutlet weak var iconAsk: UIImageView!
    @IBOutlet weak var btnAlwaysAsk: UIButton!
    @IBOutlet weak var stackBottom: NSLayoutConstraint!

    weak var delegate: DialogPassphraseViewControllerDelegate?
    var isAlwaysAsk: Bool = false

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

//        switchAsk.isOn = isAlwaysAsk
        updateAsk()

        let swipeDown = UISwipeGestureRecognizer(target: self, action: #selector(didSwipe))
            swipeDown.direction = .down
            self.view.addGestureRecognizer(swipeDown)
        let tapToClose = UITapGestureRecognizer(target: self, action: #selector(didTap))
            tappableBg.addGestureRecognizer(tapToClose)

//        AnalyticsManager.shared.recordView(.requestAmount, sgmt: AnalyticsManager.shared.subAccSeg(AccountsRepository.shared.current, walletItem: wallet?.type))
    }

    deinit {
        print("deinit")
    }

    func updateAsk() {
        iconAsk.image = isAlwaysAsk ? UIImage(named: "ic_checkbox_on")! : UIImage(named: "ic_checkbox_off")!
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        anchorBottom.constant = 0
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
            self.view.layoutIfNeeded()
        }
        fieldPassphrase.becomeFirstResponder()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    @objc func didTap(gesture: UIGestureRecognizer) {

        dismiss(.cancel)
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.stackBottom.constant = keyboardFrame.height
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.stackBottom.constant = 36.0
        })
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_login_with_bip39_passphrase", comment: "")
        let hint = NSLocalizedString("id_bip39_passphrase", comment: "")
        fieldPassphrase.attributedPlaceholder = NSAttributedString(string: hint, attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        lblHint1.text = NSLocalizedString("id_different_passphrases_generate", comment: "")
        lblAskTitle.text = NSLocalizedString("id_always_ask", comment: "")
        btnClear.setTitle("Clear Passphrase", for: .normal)
        btnConfirm.setTitle(NSLocalizedString("id_submit", comment: ""), for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        handle.cornerRadius = 1.5
        lblTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        askView.borderWidth = 1.0
        askView.borderColor = .white
        askView.layer.cornerRadius = 3.0
        fieldPassphrase.layer.cornerRadius = 5.0
        fieldPassphrase.setLeftPaddingPoints(15.0)
        fieldPassphrase.setRightPaddingPoints(15.0)
        fieldPassphrase.leftViewMode = .always
        btnConfirm.setStyle(.primary)
        btnClear.setStyle(.inline)
    }

    func dismiss(_ action: PassphraseAction) {
        anchorBottom.constant = -cardView.frame.size.height
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
            self.view.layoutIfNeeded()
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                break
            case .confirm:
                if let passphrase = self.fieldPassphrase.text {
                    self.delegate?.didConfirm(passphrase: passphrase, alwaysAsk: self.isAlwaysAsk)
                }
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

    @IBAction func passphraseDidChange(_ sender: Any) {
        if let passphrase = fieldPassphrase.text,
           passphrase.count > 0 &&
            passphrase.count <= 100 &&
            passphrase.first != " " &&
            passphrase.last != " " {
        }
    }

    @IBAction func btnConfirm(_ sender: Any) {
        dismiss(.confirm)
    }

    @IBAction func btnClear(_ sender: Any) {
        fieldPassphrase.text = ""
        dismiss(.confirm)
    }

    @IBAction func btnAlwaysAsk(_ sender: Any) {
        isAlwaysAsk = !isAlwaysAsk
        updateAsk()
    }
}
