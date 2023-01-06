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
        lblTitle.text = item.name
        lblHint.text = ""

        let img: UIImage? = {
            if item.isWatchonly {
                return UIImage(named: "ic_eye_flat")
            } else if item.gdkNetwork?.mainnet ?? true {
                return UIImage(named: "ic_wallet")
            } else {
                return UIImage(named: "ic_wallet_testnet")
            }
        }()
        self.icon.image = img!.maskWithColor(color: .white)
        self.circleImageView.isHidden = !isSelected

        self.iconSecurityType.image = UIImage() // UIImage(named: "ic_keys_invert")!

        lblHint.isHidden = !(item.isEphemeral || item.isHW)

        if let ephemeralId = item.ephemeralId {
            lblHint.text = "BIP39 #\( ephemeralId )"
            iconPassphrase.image = UIImage(named: "ic_passphrase")!
        }
        if item.isHW {
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
