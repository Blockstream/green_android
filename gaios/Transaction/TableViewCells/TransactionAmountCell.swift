import UIKit

class TransactionAmountCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblRecipient: UILabel!
    @IBOutlet weak var copyRecipientIcon: UIImageView!
    @IBOutlet weak var copyAmountIcon: UIImageView!

    var copyAmount: ((String) -> Void)?
    var copyRecipient: ((String) -> Void)?

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
        lblAmount.text = ""
        lblAsset.text = ""
        self.icon.image = UIImage()
        lblFiat.isHidden = true
        lblRecipient.isHidden = true
    }

    func configure(tx: Transaction, network: String?, index: Int, copyAmount: ((String) -> Void)?, copyRecipient: ((String) -> Void)?) {

        self.copyAmount = copyAmount
        self.copyRecipient = copyRecipient

        copyRecipientIcon.image = copyRecipientIcon.image?.maskWithColor(color: .white)
        let color: UIColor = tx.type == .outgoing ? UIColor.white : UIColor.customMatrixGreen()
        copyAmountIcon.image = copyAmountIcon.image?.maskWithColor(color: color)
        lblTitle.text = NSLocalizedString("id_recipient", comment: "")
        if tx.type == .incoming {
            lblTitle.text = NSLocalizedString("id_received", comment: "")
        }
        lblRecipient.text = tx.address()
        lblRecipient.isHidden = tx.type == .incoming
        lblAmount.textColor = color
        lblFiat.textColor = color
        copyRecipientIcon.isHidden = (tx.address() ?? "").isEmpty

        if network == "mainnet" {
            icon.image = UIImage(named: "ntw_btc")
        } else if network == "testnet" {
            icon.image = UIImage(named: "ntw_testnet")
        } else {
            icon.image = SessionsManager.current?.registry?.image(for: tx.defaultAsset)
        }

        let amounts = Transaction.sort(tx.amounts)
        var amount = amounts[index]
        // OUT transactions in BTC/L-BTC have fee included
        if tx.type == .outgoing && amount.key == tx.defaultAsset {
            amount.value -= tx.fee
        }

        if tx.defaultAsset == btc {
            if let balance = Balance.convert(details: ["satoshi": amount.value]) {
                let (amount, denom) = balance.get(tag: btc)
                lblAmount.text = String(format: "%@", amount ?? "")
                lblAsset.text = "\(denom)"
                if let fiat = balance.fiat {
                    lblFiat.text = "≈ \(fiat) \(balance.fiatCurrency)"
                }
                let (fiat, fiatCurrency) = balance.get(tag: "fiat")
                lblFiat.text = "≈ \(fiat ?? "N.A.") \(fiatCurrency)"
                lblFiat.isHidden = false
            }
        } else {
            let info = SessionsManager.current?.registry?.infos[amount.key]
            let icon = SessionsManager.current?.registry?.image(for: amount.key)
            let tag = amount.key
            let asset = info ?? AssetInfo(assetId: tag, name: tag, precision: 0, ticker: "")
            let details = ["satoshi": amount.value, "asset_info": asset.encode()!] as [String: Any]
            if let balance = Balance.convert(details: details) {
                let (amount, denom) = balance.get(tag: tag)
                lblAmount.text = String(format: "%@", amount ?? "")
                lblAsset.text = denom
                self.icon.image = icon
                lblFiat.isHidden = true
                lblRecipient.isHidden = true
                copyRecipientIcon.isHidden = true
            }
        }
    }

    @IBAction func copyRecipientBtn(_ sender: Any) {
        copyRecipient?(lblRecipient.text ?? "")
    }

    @IBAction func copyAmountBtn(_ sender: Any) {
        copyAmount?(lblAmount.text ?? "")
    }
}
