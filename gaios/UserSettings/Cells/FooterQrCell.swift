import UIKit

class FooterQrCell: UICollectionReusableView {

    @IBOutlet weak var passphraseView: UIView!
    @IBOutlet weak var lblPassphraseTitle: UILabel!
    @IBOutlet weak var lblPassphraseValue: UILabel!
    @IBOutlet weak var btnLearn: UIButton!
    @IBOutlet weak var lblQrInfo: UILabel!

    @IBOutlet weak var actionBtn: UIButton!
    var mnemonic: String?
    @IBOutlet weak var qrImg: UIImageView!

    func configure(mnemonic: String?) {
        self.mnemonic = mnemonic

        qrImg.image = QRImageGenerator.imageForTextWhite(text: mnemonic ?? "", frame: qrImg.frame)

        let isEphemeral = AccountsManager.shared.current?.isEphemeral ?? false
        passphraseView.isHidden = !isEphemeral
        lblQrInfo.isHidden = !isEphemeral
        if isEphemeral {
            lblQrInfo.text = "The QR code does not include the BIP39 Passphrase"
            lblPassphraseTitle.text = "BIP39 Passphrase"
            lblPassphraseValue.text = "TO DO"
            btnLearn.setTitle("Learn More", for: .normal)
            btnLearn.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
        }
    }

    @IBAction func actionBtn(_ sender: Any) {
        actionBtn.isHidden = true
    }

    @IBAction func btnLearnMore(_ sender: Any) {
        UIApplication.shared.open(ExternalUrls.passphraseReadMore, options: [:], completionHandler: nil)
    }
}
