import UIKit

class FooterQrCell: UICollectionReusableView {

    @IBOutlet weak var actionBtn: UIButton!
    var mnemonic: String?
    @IBOutlet weak var qrImg: UIImageView!

    func configure(mnemonic: String?) {
        self.mnemonic = mnemonic

        qrImg.image = QRImageGenerator.imageForTextWhite(text: mnemonic ?? "", frame: qrImg.frame)
    }

    @IBAction func actionBtn(_ sender: Any) {
        actionBtn.isHidden = true
    }
}
