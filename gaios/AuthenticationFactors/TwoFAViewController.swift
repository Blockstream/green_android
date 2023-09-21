import Foundation
import UIKit

class TwoFAViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!

    @IBOutlet var placeholders: [UIView]!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var btnHelp: UIButton!
    @IBOutlet weak var btnCancel: UIButton!
    
    @IBOutlet weak var cardInfo: UIView!
    @IBOutlet weak var lblInfoTitle: UILabel!
    @IBOutlet weak var lblInfoHint: UILabel!
    @IBOutlet weak var btnInfoRetry: UIButton!
    @IBOutlet weak var btnInfoSupport: UIButton!
    
    @IBOutlet var lblsDigit: [UILabel]!
    
    var digits: [Int] = []

    var onCancel: (() -> Void)?
    var onCode: ((String) -> Void)?

    var commontitle = ""
    var attemptsRemaining = 0

    var orderedPlaceHolders: [UIView] {
        return placeholders.sorted { $0.tag < $1.tag }
    }

    var orderedLblsDigit: [UILabel] {
        return lblsDigit.sorted { $0.tag < $1.tag }
    }

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
        cardInfo.isHidden = true
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        UIView.animate(withDuration: 0.3) {
            self.view.alpha = 1.0
        }
    }

    func setContent() {
        lblTitle.text = commontitle
        btnHelp.setTitle("id_help".localized, for: .normal)
        btnCancel.setTitle("id_cancel".localized, for: .normal)
        lblInfoTitle.text = "Are you not receiving your 2FA code?".localized
        lblInfoHint.text = "Try again, using another 2FA method.".localized
        btnInfoRetry.setTitle("Try Again".localized, for: .normal)
        btnInfoSupport.setTitle("Contact Support".localized, for: .normal)
    }

    func setStyle() {
        [cardInfo, cardView].forEach{
            $0.layer.cornerRadius = 10
            $0.borderWidth = 1.0
            $0.borderColor = .white.withAlphaComponent(0.05)
        }
        lblTitle.setStyle(.txtBigger)
        btnHelp.setStyle(.outlinedWhite)
        btnCancel.setStyle(.inline)
        orderedPlaceHolders.forEach {
            $0.cornerRadius = $0.frame.width / 2
        }
        lblInfoTitle.setStyle(.txtBigger)
        lblInfoHint.setStyle(.txtCard)
        btnInfoRetry.setStyle(.primary)
        btnInfoSupport.setStyle(.outlinedWhite)
    }

    func fill() {
        orderedPlaceHolders.forEach{ $0.isHidden = false}
        for n in 0...5 {
            if let d = digits[safe: n] {
                orderedLblsDigit[n].text = "\(d)"
                orderedPlaceHolders[n].isHidden = true
            } else {
                orderedLblsDigit[n].text = ""
            }
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func refresh() {
        cardInfo.isHidden.toggle()
        cardView.isHidden.toggle()
        btnCancel.isHidden.toggle()
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

    @IBAction func btnHelp(_ sender: Any) {
        refresh()
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

    @IBAction func btnInfoRetry(_ sender: Any) {
        dismiss(.cancel)
    }
    
    @IBAction func btnInfoSupport(_ sender: Any) {
        let request = DialogErrorRequest(
            account: AccountsRepository.shared.current,
            networkType: .bitcoinMS,
            error: "",
            screenName: "2FA")
        showOpenSupportUrl(request)
        dismiss(.cancel)
    }
}
