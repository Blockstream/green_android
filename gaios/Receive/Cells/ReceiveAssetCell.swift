import UIKit
import gdk

class ReceiveAssetCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!

    @IBOutlet weak var iconAsset: UIImageView!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblAccount1: UILabel!
    @IBOutlet weak var lblAccount2: UILabel!
    @IBOutlet weak var iconType: UIImageView!

    var onTap: (() -> Void)?

    static var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: ReceiveAssetCellModel, onTap: (() -> Void)?) {
        let name = model.assetName
        let ticker = model.ticker
        self.lblAsset.text = name ?? ticker
        self.iconAsset.image = model.icon
        self.lblAccount1.text = model.account.localizedName.uppercased()
        self.lblAccount2.text = model.account.type.longText.uppercased()
        self.iconType.image = networkImage(model.account.gdkNetwork)
        self.onTap = onTap
    }

    func networkImage(_ network: GdkNetwork) -> UIImage? {
        if network.lightning {
            return UIImage(named: "ic_lightning")
        } else if network.multisig {
            return UIImage(named: "ic_key_ms")
        } else {
            return UIImage(named: "ic_key_ss")
        }
    }

    @IBAction func didTap(_ sender: Any) {
        self.onTap?()
    }
}
