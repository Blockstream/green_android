import Foundation
import UIKit

protocol TwoFAViewControllerDelegate: AnyObject {
    func onDone()
}

class TwoFAViewController: UIViewController {

    @IBOutlet weak var bgLayer: UIView!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet var boxes: [UIView]!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblAttempts: UILabel!
    @IBOutlet weak var btnCancel: UIButton!
    
    @IBOutlet var lblsDigit: [UILabel]!
    weak var delegate: TwoFAViewControllerDelegate?

    var digits: [Int] = []

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
        lblTitle.text = "Please provide your code".localized
        lblAttempts.text = "Attempts remaining: 3".localized
        btnCancel.setTitle("id_cancel".localized, for: .normal)
    }

    func setStyle() {
        cardView.layer.cornerRadius = 10
        boxes.forEach{
            $0.borderWidth = 1.0
            $0.borderColor = UIColor.gGreenMatrix()
            $0.cornerRadius = 3.0
        }
        lblTitle.setStyle(.txtBigger)
        lblAttempts.setStyle(.txt)
        btnCancel.setStyle(.inline)
    }

    func fill() {
        for n in 0...5 {
            if let d = digits[safe: n] {
                lblsDigit[n].text = "\(d)"
            } else {
                lblsDigit[n].text = ""
            }
        }
        print( digits )
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func dismiss() {
        UIView.animate(withDuration: 0.3, animations: {
            self.view.alpha = 0.0
        }, completion: { _ in
            self.dismiss(animated: false, completion: {
                self.delegate?.onDone()
            })
        })
    }

    @IBAction func btnCancel(_ sender: Any) {
        dismiss()
    }

    @IBAction func btnDigit(_ sender: UIButton) {
        if digits.count < 6 {
            digits.append(sender.tag)
            fill()
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
            }
        }
    }

    @IBAction func btnCursorBack(_ sender: Any) {
        guard digits.count > 0 else { return }
        digits.removeLast()
        fill()
    }
}
