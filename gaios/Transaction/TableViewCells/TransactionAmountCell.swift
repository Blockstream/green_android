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

    func configure(transaction: Transaction, network: String?, index: Int, copyAmount: ((String) -> Void)?, copyRecipient: ((String) -> Void)?) {

        self.copyAmount = copyAmount
        self.copyRecipient = copyRecipient

        copyRecipientIcon.image = copyRecipientIcon.image?.maskWithColor(color: .white)
        let isIncoming = transaction.type == "incoming"
        let isOutgoing = transaction.type == "outgoing"
        let color: UIColor = isOutgoing ? UIColor.white : UIColor.customMatrixGreen()
        copyAmountIcon.image = copyAmountIcon.image?.maskWithColor(color: color)
        lblTitle.text = NSLocalizedString("id_recipient", comment: "")
        if isIncoming {
            lblTitle.text = NSLocalizedString("id_received", comment: "")
            // lblRecipient.isHidden = true
        }
        lblRecipient.text = transaction.address()
        lblRecipient.isHidden = false
        lblAmount.textColor = color
        lblFiat.textColor = color
        copyRecipientIcon.isHidden = (transaction.address() ?? "").isEmpty

        if network == "mainnet" {
            icon.image = UIImage(named: "ntw_btc")
        } else if network == "testnet" {
            icon.image = UIImage(named: "ntw_testnet")
        } else {
            icon.image = Registry.shared.image(for: transaction.defaultAsset)
        }

        if transaction.defaultAsset == btc {
            if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
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
            let amounts = Transaction.sort(transaction.amounts)
            if let amount = isIncoming ? amounts[index] : amounts.filter({ $0.key == transaction.defaultAsset}).first {
                let info = Registry.shared.infos[amount.key]
                let icon = Registry.shared.image(for: amount.key)
                let tag = amount.key
                let asset = info ?? AssetInfo(assetId: tag, name: tag, precision: 0, ticker: "")
                let satoshi = transaction.amounts[amount.key] ?? 0
                let details = ["satoshi": satoshi, "asset_info": asset.encode()!] as [String: Any]

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
    }

    @IBAction func copyRecipientBtn(_ sender: Any) {
        copyRecipient?(lblRecipient.text ?? "")
    }

    @IBAction func copyAmountBtn(_ sender: Any) {
        copyAmount?(lblAmount.text ?? "")
    }
}
