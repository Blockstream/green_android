import UIKit

class WalletListCell: UITableViewCell {

    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var iconSecurityType: UIImageView!
    @IBOutlet weak var circleImageView: UIImageView!
    @IBOutlet weak var iconPassphrase: UIImageView!
    @IBOutlet weak var iconHW: UIImageView!
    @IBOutlet weak var lblHint: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    override func prepareForReuse() {
        icon.image = UIImage()
        lblTitle.text = ""
        lblHint.text = ""
        iconSecurityType.image = UIImage()
        iconPassphrase.image = UIImage()
        iconHW.image = UIImage()
    }

    func configure(item: Account, isSelected: Bool = false) {
        self.lblTitle.text = item.name
        self.icon.image = item.icon
        self.circleImageView.isHidden = !isSelected

        self.iconSecurityType.image = UIImage(named: "ic_keys_invert")!

        if item.isSingleSig ?? false {
            self.iconSecurityType.image = UIImage(named: "ic_key")!
        }

        if item.isWatchonly {
            self.iconSecurityType.image = UIImage(named: "ic_eye")!
        }

        lblHint.isHidden = !(item.isEphemeral || item.isHW)

        if let ephemeralId = item.ephemeralId {
            lblHint.text = "BIP39 #\( ephemeralId )"
            iconPassphrase.image = UIImage(named: "ic_passphrase")!
        }
        if item.isHW {
            if let ntw = AvailableNetworks(rawValue: item.network) {
                lblTitle.text = (item.isSingleSig ?? false ? "SingleSig" : "Multisig") + " " + ntw.name()
            }
            lblHint.text = item.name
            if item.isJade {
                iconHW.image = UIImage(named: "ic_hww_jade")!
            }
            if item.isLedger {
                iconHW.image = UIImage(named: "ic_hww_ledger")!
            }
        }
        iconPassphrase.isHidden = !item.isEphemeral
        iconHW.isHidden = !item.isHW
    }
}
