import Foundation
import UIKit


protocol DialogFeedbackViewControllerDelegate: AnyObject {
    func didSend(rating: Int, email: String?, comment: String)
    func didCancel()
}

enum DialogFeedbackAction {
    case send
    case cancel
}

class DialogFeedbackViewController: KeyboardViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var btnsStack: UIStackView!

    @IBOutlet weak var lblRateHint: UILabel!
    @IBOutlet weak var lblFeedback: UILabel!
    @IBOutlet weak var lblCounter: UILabel!

    @IBOutlet weak var emailField: UITextField!
    @IBOutlet weak var messageTextView: UITextView!
    @IBOutlet weak var btnCopy: UIButton!
    @IBOutlet weak var btnSend: UIButton!
    @IBOutlet weak var segment: UISegmentedControl!
    @IBOutlet weak var stackBottom: NSLayoutConstraint!
    
    weak var delegate: DialogFeedbackViewControllerDelegate?

    let limit = 1000
    var isLightningScope = false
    var nodeId: String?
    var breezErrStr: String?

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

        if isLightningScope {
            lblRateHint.isHidden = true
            segment.isHidden = true
            btnCopy.isHidden = false
            segment.selectedSegmentIndex = 1
        } else {
            btnCopy.isHidden = true
        }
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

        messageTextView.delegate = self
        refreshUI()
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
        emailField.becomeFirstResponder()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        refreshUI()
    }

    @objc func didTap(gesture: UIGestureRecognizer) {

        dismiss(.cancel)
    }

    func setContent() {
        lblTitle.text = isLightningScope ? "Send Error Report" : "id_give_us_your_feedback".localized
        btnSend.setTitle("id_send".localized, for: .normal)
        btnCopy.setTitle("id_copy".localized, for: .normal)
        emailField.attributedPlaceholder = NSAttributedString(
            string: "\(NSLocalizedString("id_email", comment: "")) (\(NSLocalizedString("id_optional", comment: "").lowercased()))",
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        lblRateHint.text = NSLocalizedString("id_rate_your_experience", comment: "")
        lblFeedback.text = NSLocalizedString("id_feedback", comment: "")
    }

    func setStyle() {
        btnSend.setStyle(.primaryDisabled)
        btnCopy.setStyle(.outlinedWhite)
        emailField.setLeftPaddingPoints(10.0)
        emailField.setRightPaddingPoints(10.0)
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        segment.setTitleTextAttributes([NSAttributedString.Key.foregroundColor: UIColor.white], for: .selected)
        segment.setTitleTextAttributes([NSAttributedString.Key.foregroundColor: UIColor.lightGray], for: .normal)
        emailField.cornerRadius = 5.0
        messageTextView.cornerRadius = 5.0
    }

    func refreshUI() {
        lblCounter.text = "\(messageTextView.text.count)/\(limit)"

        if messageTextView.text.count > 3,
           isValidEmail(emailField.text ?? ""),
           segment.selectedSegmentIndex != -1 {
            btnSend.setStyle(.primary)
        } else {
            btnSend.setStyle( isLightningScope ? .primary : .primaryDisabled)
        }
    }

    func isValidEmail(_ email: String) -> Bool {
        if email == "" {
            return true
        }
        return email.isValidEmailAddr()
    }


    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)

        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
            self.stackBottom.constant = keyboardFrame.height
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillHide(notification: notification)
        UIView.animate(withDuration: 0.5, animations: { [unowned self] in
            self.stackBottom.constant = 36.0
        })
    }

    func dismiss(_ action: DialogFeedbackAction) {
        anchorBottom.constant = -cardView.frame.size.height
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
            self.view.layoutIfNeeded()
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                self.delegate?.didCancel()
            case .send:
                if self.segment.selectedSegmentIndex > -1,
                   var comment = self.messageTextView.text {
                    if let nodeId = self.nodeId, let breezErrStr = self.breezErrStr {
                        comment += "\n" + "nodeId: \(nodeId)" + "\n" + breezErrStr
                    }
                    self.delegate?.didSend(rating: self.segment.selectedSegmentIndex + 1,
                                           email: self.emailField.text,
                                           comment: comment)
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

    @IBAction func emailDidChange(_ sender: Any) {
        refreshUI()
    }

    @IBAction func segment(_ sender: Any) {
        refreshUI()
    }

    @IBAction func btnCopy(_ sender: Any) {
        if let nodeId =  nodeId, let breezErrStr = breezErrStr {
            let msg = breezErrStr + ", {\"nodeId\": \(nodeId)}"
            UIPasteboard.general.string = msg
            DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 2.0)
        }
    }

    @IBAction func btnSend(_ sender: Any) {
        dismiss(.send)
    }
}

extension DialogFeedbackViewController: UITextViewDelegate {

    func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        let newText = (textView.text as NSString).replacingCharacters(in: range, with: text)
        let numberOfChars = newText.count
        return numberOfChars <= limit
    }

    func textViewDidChange(_ textView: UITextView) {
        refreshUI()
    }
}
