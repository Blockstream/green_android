import Foundation
import UIKit

class TwoFAViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet var boxes: [UIView]!
    
    @IBOutlet var placeholders: [UIView]!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblAttempts: UILabel!
    @IBOutlet weak var btnCancel: UIButton!
    
    @IBOutlet var lblsDigit: [UILabel]!

    var digits: [Int] = []

    var onCancel: (() -> Void)?
    var onCode: ((String) -> Void)?

    var commontitle = ""

    enum TwoFAAction {
        case cancel
        case code(digits: String)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        setStyle()
        setContent()
        view.alpha = 0.0
        fill()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func setContent() {
        lblTitle.text = commontitle
        lblAttempts.text = "Attempts remaining: 3".localized
        lblAttempts.isHidden = true
        btnCancel.setTitle("id_cancel".localized, for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        cardView.borderWidth = 1.0
        cardView.borderColor = .white.withAlphaComponent(0.05)
        lblTitle.setStyle(.txtBigger)
        lblAttempts.setStyle(.txt)
        btnCancel.setStyle(.inline)
        placeholders.forEach {
            $0.cornerRadius = $0.frame.width / 2
        }
    }

    func fill() {
        placeholders.forEach{ $0.isHidden = false}
        for n in 0...5 {
            if let d = digits[safe: n] {
                lblsDigit[n].text = "\(d)"
                placeholders[n].isHidden = true
            } else {
                lblsDigit[n].text = ""
            }
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func dismiss(_ action: TwoFAAction) {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: {
                switch action {
                case .cancel:
                    self.onCancel?()
                case .code(let digits):
                    self.onCode?(digits)
                }
            })
        })
    }

    @IBAction func btnCancel(_ sender: Any) {
        dismiss(.cancel)
    }

    @IBAction func btnDigit(_ sender: UIButton) {
        if digits.count < 6 {
            digits.append(sender.tag)
            fill()
        }
        if digits.count == 6 {
            dismiss(.code(digits: (digits.map(String.init)).joined()))
        }
    }
    
    @IBAction func btnPaste(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            if txt.isCode6Digits() {
                digits = []
                for c in txt {
                    if let n = Int("\(c)") {
                        digits.append(n)
                    }
                }
                fill()
                
                DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.3) {
                    if self.digits.count == 6 {
                        self.dismiss(.code(digits: (self.digits.map(String.init)).joined()))
                    }
                }
            }
        }
    }

    @IBAction func btnCursorBack(_ sender: Any) {
        guard digits.count > 0 else { return }
        digits.removeLast()
        fill()
    }
}
