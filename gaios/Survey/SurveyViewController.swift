import Foundation
import UIKit
import PromiseKit

enum SurveyAction {
    case close
    case notnow
    case submit
}

class SurveyViewController: KeyboardViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var contentStack: UIStackView!
    @IBOutlet weak var typeRating: UIView!
    @IBOutlet weak var typeText: UIView!
    @IBOutlet weak var typeBtns: UIView!

    @IBOutlet weak var btnDismiss: UIButton!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblFeedbackHint: UILabel!
    @IBOutlet weak var segment: UISegmentedControl!
    @IBOutlet weak var lblSegmentLeft: UILabel!
    @IBOutlet weak var lblSegmentRight: UILabel!
    @IBOutlet weak var messageTextView: UITextView!
    @IBOutlet weak var lblCounter: UILabel!

    @IBOutlet weak var btnLeft: UIButton!
    @IBOutlet weak var btnRight: UIButton!

    @IBOutlet weak var cardBottom: NSLayoutConstraint!

    let limit = 1000

    var cardBottomSpace: CGFloat = .zero
    var widget: CountlyWidget?
    var step: Int = 0

    override func viewDidLoad() {
        super.viewDidLoad()

        setStyle()
        view.alpha = 0.0
        messageTextView.delegate = self

        updateState()
    }

    func updateState() {
        guard let widget = widget, widget.wType != .undefined else {
            dismiss(.notnow)
            return
        }
        btnLeft.setStyle(.inline)
        lblCounter.text = "\(messageTextView.text.count)/\(limit)"
        switch widget.wType {
        case .nps:
            btnLeft.setTitle("Not Now", for: .normal)
            btnRight.setTitle(widget.appearance?.submit?.htmlDecoded ?? "Submit", for: .normal)
            lblSegmentLeft.text = widget.appearance?.notLikely?.htmlDecoded ?? ""
            lblSegmentRight.text = widget.appearance?.likely?.htmlDecoded ?? ""
            lblHint.isHidden = true
            lblFeedbackHint.isHidden = true
            btnRight.setStyle(.primary)
            switch step {
            case 0:
                lblTitle.text = widget.msg?.mainQuestion?.htmlDecoded ?? ""
                typeText.isHidden = true
                btnRight.alpha = 0.0
            case 1:
                if widget.wFollowUpType == .none {
                    dismiss(.submit)
                }
                switch widget.wFollowUpType {
                case .score:
                    switch segment.selectedSegmentIndex {
                    case 0, 1, 2, 3:
                        lblTitle.text = widget.msg?.followUpDetractor?.htmlDecoded ?? ""
                    case 4:
                        lblTitle.text = widget.msg?.followUpPassive?.htmlDecoded ?? ""
                    case 5:
                        lblTitle.text = widget.msg?.followUpPromoter?.htmlDecoded ?? ""
                    default:
                        lblTitle.text = ""
                    }
                case .one:
                    lblTitle.text = widget.msg?.followUpAll?.htmlDecoded ?? ""
                case .none:
                    break
                }
                typeRating.isHidden = true
                typeText.isHidden = false
                btnRight.alpha = 1.0
            default:
                break
            }
        case .survey:
            if segment.numberOfSegments > 5 {
                segment.removeSegment(at: 0, animated: false)
            }
            lblTitle.text = widget.name?.htmlDecoded ?? ""
            btnLeft.setTitle("Not Now", for: .normal)
            btnRight.setTitle(widget.appearance?.submit?.htmlDecoded ?? "Submit", for: .normal)

            let qRating: WidgetQuestion? = (widget.questions?.filter { $0.qType == .rating })?.first
            let qText: WidgetQuestion? = (widget.questions?.filter { $0.qType == .text })?.first

            if let qRating = qRating {
                lblHint.isHidden = false
                lblHint.text = qRating.question?.htmlDecoded
                lblSegmentLeft.text = qRating.notLikely?.htmlDecoded ?? ""
                lblSegmentRight.text = qRating.likely?.htmlDecoded ?? ""
            } else {
                typeRating.isHidden = true
            }
            if let qText = qText {
                lblFeedbackHint.isHidden = false
                lblFeedbackHint.text = qText.question?.htmlDecoded
            } else {
                typeText.isHidden = true
            }

            /// validation
            if qRating != nil && qText != nil {
                btnRight.setStyle(.primaryDisabled)
                if segment.selectedSegmentIndex >= 0 {
                    if qRating?.required == false {
                       btnRight.setStyle(.primary)
                    } else {
                        if messageTextView.text.count > 0 {
                            btnRight.setStyle(.primary)
                        }
                    }
                }
            } else if qRating == nil && qText != nil {
                btnRight.setStyle(.primaryDisabled)
                if qRating?.required == false {
                   btnRight.setStyle(.primary)
                } else {
                    if messageTextView.text.count > 0 {
                        btnRight.setStyle(.primary)
                    }
                }
            } else if qRating != nil && qText == nil {
                btnRight.setStyle(.primaryDisabled)
                if segment.selectedSegmentIndex >= 0 {
                    btnRight.setStyle(.primary)
                }
            }
        default:
            break
        }

        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.1) {
            if !self.messageTextView.isFirstResponder {
                self.cardBottomSpace = (self.view.frame.size.height - self.cardView.frame.size.height) / 2.0
                self.cardBottom.constant = self.cardBottomSpace
                UIView.animate(withDuration: 0.1, animations: { [weak self] in
                    self?.view.layoutIfNeeded()
                })
            }
        }
    }

    func setStyle() {
        cardView.layer.cornerRadius = 20
        segment.setTitleTextAttributes([NSAttributedString.Key.foregroundColor: UIColor.white], for: .selected)
        segment.setTitleTextAttributes([NSAttributedString.Key.foregroundColor: UIColor.lightGray], for: .normal)
    }

    override func viewDidAppear(_ animated: Bool) {

        updateState()

        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)

        let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
        if keyboardFrame.height > cardBottomSpace {
            cardBottom.constant = keyboardFrame.height - 44.0
        }
        UIView.animate(withDuration: 0.5, animations: { [weak self] in
            self?.view.layoutIfNeeded()
        })
    }

    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        cardBottom.constant = cardBottomSpace
        UIView.animate(withDuration: 0.5, animations: { [weak self] in
            self?.view.layoutIfNeeded()
        })
    }

    func dismiss(_ action: SurveyAction) {

        if let widget = widget {

            switch action {
            case .close:
                AnalyticsManager.shared.submitExclude()
            case .notnow:
                // nothing to do
                break
            case .submit:
                switch widget.wType {
                case .nps:
                    var data: [AnyHashable: Any] = [:]
                    data["rating"] = formattedRating()
                    data["comment"] = messageTextView.text ?? ""
                    AnalyticsManager.shared.submitNPS(data)
                    DropAlert().success(message: widget.msg?.thanks ?? "")
                case .survey:
                    let qRatingId = (widget.questions?.filter { $0.qType == .rating })?.first?.id
                    let qTextId = (widget.questions?.filter { $0.qType == .text })?.first?.id
                    var data: [AnyHashable: Any] = [:]
                    if qRatingId != nil {
                        data["answ-" + qRatingId!] = segment.selectedSegmentIndex + 1
                    }
                    if qTextId != nil {
                        data["answ-" + qTextId!] = messageTextView.text ?? ""
                    }
                    AnalyticsManager.shared.submitSurvey(data)
                    DropAlert().success(message: widget.msg?.thanks ?? "")
                case .undefined:
                    break
                }
            }
        }
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: nil)
        })
    }

    func formattedRating() -> Int {
        switch segment.selectedSegmentIndex {
        case 0:
            return 0
        case 1:
            return 2
        case 2:
            return 4
        case 3:
            return 6
        case 4:
            return 8
        case 5:
            return 10
        default:
            return -1
        }
    }

    @IBAction func segment(_ sender: Any) {
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.3) {
            self.step += 1
            self.updateState()
        }
    }

    @IBAction func btnLeft(_ sender: Any) {
        dismiss(.notnow)
    }

    @IBAction func btnRight(_ sender: Any) {
        dismiss(.submit)
    }

    @IBAction func btnDismiss(_ sender: Any) {
        dismiss(.close)
    }
}

extension SurveyViewController: UITextViewDelegate {

    func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        let newText = (textView.text as NSString).replacingCharacters(in: range, with: text)
        let numberOfChars = newText.count
        return numberOfChars <= limit
    }

    func textViewDidChange(_ textView: UITextView) {
        lblCounter.text = "\(messageTextView.text.count)/\(limit)"
        self.updateState()
    }
}
