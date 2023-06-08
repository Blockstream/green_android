import UIKit
import gdk

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
    @IBOutlet weak var recipientView: UIView!

    var copyAmount: ((String) -> Void)?
    var copyRecipient: ((String) -> Void)?

    private var btc: String {
        return WalletManager.current?.account.gdkNetwork.getFeeAsset() ?? ""
    }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.layer.cornerRadius = 5.0

        lblTitle.setStyle(.sectionTitle)
        lblRecipient.font = UIFont.systemFont(ofSize: 16.0, weight: .semibold)
        lblAmount.font = UIFont.systemFont(ofSize: 16.0, weight: .semibold)
        lblAsset.font = UIFont.systemFont(ofSize: 16.0, weight: .semibold)
        lblFiat.font = UIFont.systemFont(ofSize: 12.0, weight: .regular)
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

    // swiftlint:disable function_parameter_count
    func configure(tx: Transaction, isLightning: Bool, id: String, value: Int64, hideBalance: Bool,
                   copyAmount: ((String) -> Void)?, copyRecipient: ((String) -> Void)?) {

        self.copyAmount = copyAmount
        self.copyRecipient = copyRecipient

        copyRecipientIcon.image = copyRecipientIcon.image?.maskWithColor(color: .white)
        let color: UIColor = value > 0 ? .customMatrixGreen() : .white
        copyAmountIcon.image = copyAmountIcon.image?.maskWithColor(color: color)
        lblTitle.text = NSLocalizedString(value > 0 ? "id_received_on" : "id_sent_to", comment: "")
        let address = address(tx)
        lblRecipient.text = address
        if address == nil { lblTitle.text = NSLocalizedString(value > 0 ? "id_received" : "id_sent", comment: "")}
        lblRecipient.isHidden = tx.isLiquid
        lblFiat.textColor = color
        copyRecipientIcon.isHidden = tx.isLiquid
        recipientView.isHidden = tx.isLiquid || isLightning
        lblAmount.textColor = color
        lblFiat.isHidden = id != tx.feeAsset
        lblAsset.textColor = color

        if tx.subaccountItem?.gdkNetwork.lightning ?? false {
            icon.image = UIImage(named: "ic_lightning_btc")
        } else {
            let registry = WalletManager.current?.registry
            Task { icon.image = await registry?.image(for: id) }
        }

        if let balance = Balance.fromSatoshi(value, assetId: id) {
            let (amount, denom) = balance.toValue()
            lblAmount.text = amount
            lblAsset.text = denom
            let (fiat, curr) = balance.toFiat()
            lblFiat.text = "≈ \(fiat) \(curr)"
            if hideBalance {
                lblAmount.attributedText = Common.obfuscate(color: color, size: 14, length: 5)
                lblFiat.attributedText =  Common.obfuscate(color: color, size: 10, length: 5)
            }
        }
    }

    func address(_ tx: Transaction) -> String? {
        if tx.isLiquid {
            return nil
        }
        switch tx.type {
        case .outgoing:
            let output = tx.outputs?.filter { $0["is_relevant"] as? Bool == false}.first
            return output?["address"] as? String
        case .incoming:
            let output = tx.outputs?.filter { $0["is_relevant"] as? Bool == true}.first
            return output?["address"] as? String
        default:
            return nil
        }
    }

    @IBAction func copyRecipientBtn(_ sender: Any) {
        copyRecipient?(lblRecipient.text ?? "")
    }

    @IBAction func copyAmountBtn(_ sender: Any) {
        copyAmount?(lblAmount.text ?? "")
    }
}
