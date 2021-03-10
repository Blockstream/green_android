import UIKit

class RecoveryPhraseViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var cardQR: UIView!
    @IBOutlet weak var lblQRTitle: UILabel!
    @IBOutlet weak var lblQRHint: UILabel!

    @IBOutlet weak var cardPhrase: UIView!
    @IBOutlet weak var lblPhraseTitle: UILabel!
    @IBOutlet weak var lblPhraseHint: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()
    }

    func setContent() {
        title = ""
        lblTitle.text = "Enter your Recovery Phrase"
        lblHint.text = ""
        lblQRTitle.text = "QR Code"
        lblQRHint.text = "As easy as snapping a photo with your phone. If you have an existing wallet, we suggest this one."
        lblPhraseTitle.text = "Recovery Phrase"
        lblPhraseHint.text = "Got it written down? Great. You can type it in here. Don’t lose it, it’ll stay the same."
    }

    func setStyle() {
        cardQR.layer.cornerRadius = 5.0
        cardPhrase.layer.cornerRadius = 5.0
    }

    func setActions() {
        let tapGesture1 = UITapGestureRecognizer(target: self, action: #selector(didPressCardQR))
        cardQR.addGestureRecognizer(tapGesture1)
        let tapGesture2 = UITapGestureRecognizer(target: self, action: #selector(didPressCardPhrase))
        cardPhrase.addGestureRecognizer(tapGesture2)
    }

    @objc func didPressCardQR() {
        next(.qr)
    }

    @objc func didPressCardPhrase() {
        next(.phrase)
    }

    func next(_ recoveryType: RecoveryType) {

        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "MnemonicViewController") as? MnemonicViewController {
            vc.isTemporary = false // tempRestore
            vc.recoveryType = recoveryType
            navigationController?.pushViewController(vc, animated: true)
        }
    }
}
