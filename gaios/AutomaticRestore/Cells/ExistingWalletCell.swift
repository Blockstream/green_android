import UIKit

class ExistingWalletCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblSecurity: UILabel!
    @IBOutlet weak var lblStatus: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.layer.cornerRadius = 6.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
        icon.image = UIImage()
        lblSecurity.text = ""
        lblStatus.text = ""
    }

    func configure(_ wallet: ExistingWallet) {
        lblSecurity.text = wallet.isSingleSig ? NSLocalizedString("id_singlesig", comment: "") : NSLocalizedString("id_multisig_shield", comment: "")
        lblStatus.text = wallet.isFound ? "Wallet found" : "Wallet not found"
        if wallet.isJustRestored {
            lblStatus.text = NSLocalizedString("id_wallet_already_restored", comment: "")
        }
        self.icon.image = wallet.isSingleSig ? UIImage(named: "ic_key")! : UIImage(named: "ic_keys_invert")!
        bg.alpha = wallet.isFound || !wallet.isJustRestored ? 1.0 : 0.5
    }
}
