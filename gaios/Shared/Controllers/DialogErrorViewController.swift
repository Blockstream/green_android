import Foundation
import UIKit
import gdk

class DialogErrorRequest {
    var error: String?
    var throwable: String?
    var network: NetworkSecurityCase?
    var hw: String?
    var subject: String?
    var timestamp = Date().timeIntervalSince1970

    init(account: Account?, networkType: NetworkSecurityCase?, error: String?, screenName: String?) {
        self.network = networkType
        self.hw = account?.isJade ?? false ? "jade" : account?.isLedger ?? false ? "ledger" : nil
        self.error = error
        self.throwable = Thread.callStackSymbols.joined(separator: "\n")
        self.subject = "iOS Error Report"
        if let screenName = screenName {
            self.subject = "iOS Issue in \(screenName)"
        }
    }
}

enum DialogErrorFeedbackAction {
    case send
    case cancel
}

class DialogErrorViewController: DialogViewController {

    @IBOutlet weak var tappableBg: UIView!
    @IBOutlet weak var handle: UIView!
    @IBOutlet weak var anchorBottom: NSLayoutConstraint!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblFeedback: UILabel!
    @IBOutlet weak var lblCounter: UILabel!

    @IBOutlet weak var emailField: UITextField!
    @IBOutlet weak var messageTextView: UITextView!
    @IBOutlet weak var btnCopy: UIButton!
    @IBOutlet weak var btnSend: UIButton!
    @IBOutlet weak var stackBottom: NSLayoutConstraint!
    
    let limit = 1000
    var request: DialogErrorRequest? = nil

    override func viewDidLoad() {
        super.viewDidLoad()
        setContent()
        setStyle()

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
        lblTitle.text = "Send Error Report"
        btnSend.setTitle("id_send".localized, for: .normal)
        btnCopy.setTitle("id_copy".localized, for: .normal)
        emailField.attributedPlaceholder = NSAttributedString(
            string: "\("id_email".localized) (\("id_optional".localized.lowercased()))",
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        lblFeedback.text = NSLocalizedString("id_feedback", comment: "")
    }

    func setStyle() {
        btnSend.setStyle(.primary)
        btnCopy.setStyle(.outlinedWhite)
        emailField.setLeftPaddingPoints(10.0)
        emailField.setRightPaddingPoints(10.0)
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        emailField.cornerRadius = 5.0
        messageTextView.cornerRadius = 5.0
    }

    func dismiss(_ action: DialogErrorFeedbackAction) {
        anchorBottom.constant = -cardView.frame.size.height
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
            self.view.layoutIfNeeded()
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
        })
    }

    func refreshUI() {
        lblCounter.text = "\(messageTextView.text.count)/\(limit)"
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

    @IBAction func btnCopy(_ sender: Any) {
        if let msg = request?.error {
            UIPasteboard.general.string = msg
            DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 2.0)
        }
    }

    @IBAction func btnSend(_ sender: Any) {
        var errorString = request?.error ?? ""
        if let nodeId = WalletManager.current?.lightningSession?.nodeState?.id, let timestamp = request?.timestamp{
            errorString += " NodeId: \(nodeId)" + " Timestamp: \(Int(timestamp))"
        }
        ZendeskSdk.shared.submitNewTicket(
            subject: request?.subject,
            email: emailField.text,
            message: messageTextView.text,
            error: errorString,
            network: request?.network,
            hw: request?.hw)
        dismiss(.send)
    }
}

extension DialogErrorViewController: UITextViewDelegate {

    func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        let newText = (textView.text as NSString).replacingCharacters(in: range, with: text)
        let numberOfChars = newText.count
        return numberOfChars <= limit
    }

    func textViewDidChange(_ textView: UITextView) {
        refreshUI()
    }
}
