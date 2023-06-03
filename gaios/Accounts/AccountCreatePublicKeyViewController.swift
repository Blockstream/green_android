import UIKit
import gdk
import greenaddress

class AccountCreatePublicKeyViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var lblKeyHint: UILabel!
    @IBOutlet weak var containerKey: UIView!
    @IBOutlet weak var textViewKey: UITextView!
    @IBOutlet weak var lblErrorKey: UILabel!
    @IBOutlet weak var btnCancel: UIButton!
    @IBOutlet weak var btnPaste: UIButton!
    @IBOutlet weak var btnQr: UIButton!

    @IBOutlet weak var btnNext: UIButton!
    weak var delegate: AccountCreateRecoveryKeyDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        textViewKey.delegate = self

        AnalyticsManager.shared.recordView(.addAccountPublicKey, sgmt: AnalyticsManager.shared.sessSgmt(AccountsRepository.shared.current))
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_enter_your_xpub", comment: "")
        lblHint.text = NSLocalizedString("id_use_an_xpub_for_which_you_own", comment: "")
        lblKeyHint.text = NSLocalizedString("id_xpub", comment: "")
        lblErrorKey.text = NSLocalizedString("id_invalid_xpub", comment: "")
    }

    func setStyle() {
        textViewKey.textContainer.heightTracksTextView = true
        textViewKey.isScrollEnabled = false
        containerKey.cornerRadius = 6.0
        containerKey.borderWidth = 1.0
        containerKey.borderColor = UIColor.customTextFieldBg()
        containerKey.borderWidth = 1.0
        containerKey.borderColor = UIColor.customTextFieldBg()
        lblErrorKey.isHidden = true
        btnNext.setStyle(.primaryDisabled)
    }

    func isValid() -> Bool {
        do {
            _ = try Wally.bip32KeyFromBase58(textViewKey.text)
            return true
        } catch {
            return false
        }
    }

    func refresh() {
        lblErrorKey.isHidden = false
        btnNext.setStyle(.primaryDisabled)
        containerKey.borderColor = UIColor.errorRed()
        if isValid() {
            btnNext.setStyle(.primary)
            lblErrorKey.isHidden = true
            containerKey.borderColor = UIColor.customTextFieldBg()
        }
        btnCancel.isHidden = !(textViewKey.text.count > 0)
        btnPaste.isHidden = (textViewKey.text.count > 0)
    }

    @objc func triggerTextChange() {
        refresh()
    }

    func next() {
        navigationController?.popViewController(animated: true)
        delegate?.didPublicKey(textViewKey.text ?? "")
    }

    @IBAction func btnCancel(_ sender: Any) {
        textViewKey.text = ""
        triggerTextChange()
    }

    @IBAction func btnPaste(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            textViewKey.text = txt
        }
        triggerTextChange()
    }

    @IBAction func btnQr(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogScanViewController") as? DialogScanViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        next()
    }
}

extension AccountCreatePublicKeyViewController: DialogScanViewControllerDelegate {
    func didScan(value: String, index: Int?) {
        textViewKey.text = value
        triggerTextChange()
    }
    func didStop() {
        //
    }
}

extension AccountCreatePublicKeyViewController: UITextViewDelegate {
    func textViewDidChange(_ textView: UITextView) {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.triggerTextChange), object: nil)
        perform(#selector(self.triggerTextChange), with: nil, afterDelay: 0.5)
    }

    func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        if text == "\n" {
            textView.resignFirstResponder()
            return false
        }
        return true
    }
}
