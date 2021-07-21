import UIKit

class WalletDrawerCell: UITableViewCell {

    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var selectedView: UIView!
    @IBOutlet weak var iconSecurityType: UIImageView!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    func configure(_ item: Account, _ isSelected: Bool = false) {
        self.lblTitle.text = item.name
        self.icon.image = item.icon
        self.selectedView.isHidden = !isSelected
        self.selectedView.borderWidth = 1.0
        self.selectedView.borderColor = UIColor.customMatrixGreen().withAlphaComponent(0.6)
        self.selectedView.layer.cornerRadius = 4.0

        self.iconSecurityType.image = UIImage(named: "ic_keys_invert")!

        if item.isSingleSig ?? false {
            self.iconSecurityType.image = UIImage(named: "ic_key")!
        }

        if item.isWatchonly {
            self.iconSecurityType.image = UIImage(named: "ic_eye")!
        }
    }

}
