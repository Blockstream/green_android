import Foundation
import UIKit

protocol LTInvitationViewControllerDelegate: AnyObject {
    func didConfirm(txt: String)
    func didCancel()
}

enum DialogInvitationAction {
    case confirm
    case cancel
}

class LTInvitationViewController: KeyboardViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var txtTextField: UITextField!

    @IBOutlet weak var btnClear: UIButton!
    @IBOutlet weak var btnPaste: UIButton!
    @IBOutlet weak var btnConfirm: UIButton!
    @IBOutlet weak var btnsStack: UIStackView!
    @IBOutlet weak var bgField: UIView!
    @IBOutlet weak var stackBottom: NSLayoutConstraint!

    weak var delegate: LTInvitationViewControllerDelegate?

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

        txtTextField.attributedPlaceholder = NSAttributedString(string: "Enter your code", attributes: [NSAttributedString.Key.foregroundColor: UIColor.gW60()])

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
        txtTextField.becomeFirstResponder()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        reload()
    }

    func reload() {
        let isEmpty = txtTextField.text?.isEmpty ?? true
        btnClear.isHidden = isEmpty
        btnPaste.isHidden = !isEmpty
        btnConfirm.setStyle(isEmpty ? .primaryDisabled : .primary)
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
        lblTitle.text = "Enable Experimental Feature".localized
        lblHint.text = "Paste or type your code"
        btnConfirm.setTitle("Confirm", for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        handle.cornerRadius = 1.5
        
        lblTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        lblHint.setStyle(.txtCard)
        bgField.layer.cornerRadius = 5.0
        btnConfirm.setStyle(.primaryDisabled)
    }

    func dismiss(_ action: DialogAmountAction) {
        anchorBottom.constant = -cardView.frame.size.height
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
            self.view.layoutIfNeeded()
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .confirm:
                self.delegate?.didConfirm(txt: self.txtTextField.text ?? "")
            case .cancel:
                self.delegate?.didCancel()
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

    @IBAction func txtDidChange(_ sender: Any) {
        reload()
    }

    @IBAction func btnClear(_ sender: Any) {
        txtTextField.text = ""
        reload()
    }

    @IBAction func btnPaste(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            txtTextField.text = txt
            reload()
        }
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }

    @IBAction func btnConfirm(_ sender: Any) {
        dismiss(.confirm)
    }
}
