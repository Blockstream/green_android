import UIKit

class TransactionAmountCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblRecipient: UILabel!

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    func configure(transaction: Transaction, network: String?) {

        let isIncoming = transaction.type == "incoming"
        let isOutgoing = transaction.type == "outgoing"
        let color: UIColor = isOutgoing ? UIColor.white : UIColor.customMatrixGreen()

        lblTitle.text = NSLocalizedString("id_recipient", comment: "")
        if isIncoming {
            lblTitle.text = NSLocalizedString("id_received", comment: "")
            lblRecipient.isHidden = true
        }
        lblRecipient.text = transaction.address()

        if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
            let (amount, denom) = balance.get(tag: btc)
            lblAmount.text = String(format: "%@%@", isOutgoing ? "-" : "+", amount ?? "")

            lblAsset.text = "\(denom)"
            if let fiat = balance.fiat {
                lblFiat.text = "≈ \(fiat) \(balance.fiatCurrency)"
            }
            lblAmount.textColor = color
            lblFiat.textColor = color

//            if isBtc || tag == getGdkNetwork("liquid").policyAsset {
            let (fiat, fiatCurrency) = balance.get(tag: "fiat")
            lblFiat.text = "≈ \(fiat ?? "N.A.") \(fiatCurrency)"
//            }
        }
        if network == "mainnet" {
            icon.image = UIImage(named: "ntw_btc")
        } else if network == "testnet" {
            icon.image = UIImage(named: "ntw_testnet")
        } else {
            icon.image = Registry.shared.image(for: transaction.defaultAsset)
        }
    }
}
