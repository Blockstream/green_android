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

    func configure(mnemonic: String?, bip39Passphrase: String?) {
        self.mnemonic = mnemonic

        qrImg.image = QRImageGenerator.imageForTextWhite(text: mnemonic ?? "", frame: qrImg.frame)

        let isEphemeral = AccountDao.shared.current?.isEphemeral ?? false
        passphraseView.isHidden = !isEphemeral
        lblQrInfo.isHidden = !isEphemeral
        if isEphemeral {
            lblQrInfo.text = NSLocalizedString("id_the_qr_code_does_not_include", comment: "")
            lblPassphraseTitle.text = NSLocalizedString("id_bip39_passphrase", comment: "")
            lblPassphraseValue.text = bip39Passphrase ?? ""
            btnLearn.setTitle(NSLocalizedString("id_learn_more", comment: ""), for: .normal)
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
