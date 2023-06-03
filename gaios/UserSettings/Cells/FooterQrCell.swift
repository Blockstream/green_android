import UIKit

class FooterQrCell: UICollectionReusableView {

    @IBOutlet weak var passphraseView: UIView!
    @IBOutlet weak var lblPassphraseTitle: UILabel!
    @IBOutlet weak var lblPassphraseValue: UILabel!
    @IBOutlet weak var btnLearn: UIButton!
    @IBOutlet weak var lblQrInfo: UILabel!
    @IBOutlet weak var bip85View: UIView!
    @IBOutlet weak var infoPanel: UIView!
    @IBOutlet weak var lblBip85: UILabel!

    @IBOutlet weak var actionBtn: UIButton!
    var mnemonic: String?
    @IBOutlet weak var qrImg: UIImageView!

    func configure(mnemonic: String?, bip39Passphrase: String?) {
        self.mnemonic = mnemonic

        qrImg.image = QRImageGenerator.imageForTextWhite(text: mnemonic ?? "", frame: qrImg.frame)
        qrImg.isHidden = true

        let isEphemeral = bip39Passphrase != nil
        passphraseView.isHidden = !isEphemeral
        lblQrInfo.isHidden = !isEphemeral
        if isEphemeral {
            lblQrInfo.text = NSLocalizedString("id_the_qr_code_does_not_include", comment: "")
            lblPassphraseTitle.text = NSLocalizedString("id_bip39_passphrase", comment: "")
            lblPassphraseValue.text = bip39Passphrase ?? ""
            btnLearn.setTitle(NSLocalizedString("id_learn_more", comment: ""), for: .normal)
            btnLearn.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
        }
        bip85View.isHidden = true
    }

    func configureBip85(mnemonic: String?) {
        self.mnemonic = mnemonic

        qrImg.image = QRImageGenerator.imageForTextWhite(text: mnemonic ?? "", frame: qrImg.frame)
        qrImg.isHidden = true
        passphraseView.isHidden = true
        lblQrInfo.isHidden = true
        bip85View.isHidden = false
        infoPanel.cornerRadius = 5.0
        infoPanel.backgroundColor = UIColor.gGreenFluo().withAlphaComponent(0.2)
        lblBip85.text = "This is your BIP85 derived recovery phrase only for your Lightning account.\n\nWARNING: You can't fully restore your wallet with that."
    }

    @IBAction func actionBtn(_ sender: Any) {
        qrImg.isHidden = false
        actionBtn.isHidden = true
    }

    @IBAction func btnLearnMore(_ sender: Any) {
        UIApplication.shared.open(ExternalUrls.passphraseReadMore, options: [:], completionHandler: nil)
    }
}
