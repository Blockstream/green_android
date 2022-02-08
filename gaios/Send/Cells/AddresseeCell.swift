import UIKit

class AddresseeCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!

    @IBOutlet weak var lblRecipientTitle: UILabel!
    @IBOutlet weak var lblRecipientAddress: UILabel!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblDenomination: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblFiat: UILabel!

    var isFiat = false

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    var network: String? {
        get {
            return AccountsManager.shared.current?.network
        }
    }

    override func awakeFromNib() {
        super.awakeFromNib()
        setStyle()
        setContent()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
    }

    func configure(transaction: Transaction, index: Int) {
        let addressee = transaction.addressees[index]
        lblRecipientTitle.text = NSLocalizedString("id_recipient", comment: "")
        lblRecipientAddress.text = addressee.address

        let asset = transaction.defaultAsset
        if asset == "btc" || asset == getGdkNetwork("liquid").policyAsset {
            if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
                let (value, denom) = balance.get(tag: btc)
                lblAmount.text = value ?? ""
                lblDenomination.text = "\(denom)"
                let (fiat, fiatCurrency) = balance.get(tag: "fiat")
                lblFiat.text = "≈ \(fiat ?? "N.A.") \(fiatCurrency)"
            }
        } else {
            let info = Registry.shared.infos[asset] ?? AssetInfo(assetId: asset, name: "", precision: 0, ticker: "")
            let details = ["satoshi": transaction.amounts[asset]!, "asset_info": info.encode()!] as [String: Any]
            if let balance = Balance.convert(details: details) {
                let (amount, ticker) = balance.get(tag: transaction.defaultAsset)
                lblAmount.text = amount ?? "N.A."
                lblDenomination.text = "\(ticker)"
                lblFiat.isHidden = true
            }
        }
        icon.image = UIImage(named: "default_asset_icon")!
        if network == "mainnet" {
            icon.image = UIImage(named: "ntw_btc")
        } else if network == "testnet" {
            icon.image = UIImage(named: "ntw_testnet")
        } else {
            icon.image = Registry.shared.image(for: asset)
        }
    }

    func setStyle() {
        bg.cornerRadius = 8.0
    }

    func setContent() {
    }
}
