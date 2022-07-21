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
        icon.image = wallet.isSingleSig ? UIImage(named: "ic_key")! : UIImage(named: "ic_keys_invert")!
        bg.alpha = 0.5
        switch wallet.failure {
        case .some(.invalid):
            lblStatus.text = NSLocalizedString("id_invalid_recovery_phrase", comment: "")
        case .some(.notFound):
            lblStatus.text = NSLocalizedString("id_wallet_not_found", comment: "")
        case .some(.isJustRestored):
            lblStatus.text = NSLocalizedString("id_wallet_already_restored", comment: "")
        case .some(.disconnect):
            lblStatus.text = NSLocalizedString("id_connection_failed", comment: "")
        case .none:
            lblStatus.text = NSLocalizedString("id_wallet_found", comment: "")
            bg.alpha = 1.0
        }
    }
}
