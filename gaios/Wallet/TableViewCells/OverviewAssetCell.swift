import UIKit

class OverviewAssetCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblDenom: UILabel!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblAmount2: UILabel!
    @IBOutlet weak var icon: UIImageView!

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
        lblDenom.text = ""
        lblAsset.text = ""
        lblAmount.text = ""
        lblAmount2.text = ""
        icon.image = nil
    }

    func configure(tag: String, info: AssetInfo?, icon: UIImage?, satoshi: Int64, isLiquid: Bool = false) {
        prepareForReuse()
        if let balance = Balance.fromSatoshi(satoshi, asset: info) {
            let (amount, denom) = balance.toValue()
            lblAmount.text = amount
            lblDenom.text = denom
            lblAmount2.text = ""
            if tag == AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? "" {
                let (fiat, fiatCurrency) = balance.toFiat()
                lblAmount2.text = "≈ \(fiat) \(fiatCurrency)"
            }
        }
        selectionStyle = .none
        lblAsset.text = info?.name ?? tag
        self.icon.image = icon
    }
}
