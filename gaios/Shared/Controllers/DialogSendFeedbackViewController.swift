import Foundation
import UIKit
import PromiseKit

protocol DialogSendFeedbackViewControllerDelegate: AnyObject {
    func didSend(rating: Int, email: String?, comment: String)
    func didCancel()
}

enum FeedbackAction {
    case send
    case cancel
}

class DialogSendFeedbackViewController: KeyboardViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var btnsStack: UIStackView!
    @IBOutlet weak var btnDismiss: UIButton!

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblRateHint: UILabel!
    @IBOutlet weak var lblFeedback: UILabel!
    @IBOutlet weak var lblCounter: UILabel!

    @IBOutlet weak var emailField: UITextField!
    @IBOutlet weak var messageTextView: UITextView!
    @IBOutlet weak var btnSend: UIButton!
    @IBOutlet weak var segment: UISegmentedControl!
    @IBOutlet weak var stackBottom: NSLayoutConstraint!

    weak var delegate: DialogSendFeedbackViewControllerDelegate?
    let limit = 1000
    var buttonConstraint: NSLayoutConstraint?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        view.alpha = 0.0

        messageTextView.delegate = self
        refreshUI()
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_give_us_your_feedback", comment: "")
        btnSend.setTitle(NSLocalizedString("id_send", comment: ""), for: .normal)
        emailField.attributedPlaceholder = NSAttributedString(
            string: "\(NSLocalizedString("id_email", comment: "")) (\(NSLocalizedString("id_optional", comment: "").lowercased()))",
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])
        lblRateHint.text = NSLocalizedString("id_rate_your_experience", comment: "")
        lblFeedback.text = NSLocalizedString("id_feedback", comment: "")
    }

    func setStyle() {
        btnSend.setStyle(.primaryDisabled)
        emailField.setLeftPaddingPoints(10.0)
        emailField.setRightPaddingPoints(10.0)
        cardView.layer.cornerRadius = 20
        cardView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        segment.setTitleTextAttributes([NSAttributedString.Key.foregroundColor: UIColor.white], for: .selected)
        segment.setTitleTextAttributes([NSAttributedString.Key.foregroundColor: UIColor.lightGray], for: .normal)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
        emailField.becomeFirstResponder()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        refreshUI()
    }

    func refreshUI() {
        lblCounter.text = "\(messageTextView.text.count)/\(limit)"

        if messageTextView.text.count > 3,
           isValidEmail(emailField.text ?? ""),
           segment.selectedSegmentIndex != -1 {
            btnSend.setStyle(.primary)
        } else {
            btnSend.setStyle(.primaryDisabled)
        }
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)

        let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
        stackBottom.constant = keyboardFrame.height
        UIView.animate(withDuration: 0.5, animations: { [weak self] in
            self?.view.layoutIfNeeded()
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        stackBottom.constant = 36.0
        UIView.animate(withDuration: 0.5, animations: { [weak self] in
            self?.view.layoutIfNeeded()
        })
    }

    func dismiss(_ action: FeedbackAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
            switch action {
            case .cancel:
                self.delegate?.didCancel()
            case .send:
                if self.segment.selectedSegmentIndex > -1,
                   let comment = self.messageTextView.text {
                    self.delegate?.didSend(rating: self.segment.selectedSegmentIndex + 1,
                                           email: self.emailField.text,
                                           comment: comment)
                }
            }
        })
    }

    func isValidEmail(_ email: String) -> Bool {
        if email == "" {
            return true
        }
        let emailRegEx = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPred = NSPredicate(format: "SELF MATCHES %@", emailRegEx)
        return emailPred.evaluate(with: email)
    }

    @IBAction func emailDidChange(_ sender: Any) {
        refreshUI()
    }

    @IBAction func segment(_ sender: Any) {
        refreshUI()
    }

    @IBAction func btnSend(_ sender: Any) {
        dismiss(.send)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.cancel)
    }
}

extension DialogSendFeedbackViewController: UITextViewDelegate {

    func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        let newText = (textView.text as NSString).replacingCharacters(in: range, with: text)
        let numberOfChars = newText.count
        return numberOfChars <= limit
    }

    func textViewDidChange(_ textView: UITextView) {
        refreshUI()
    }
}
