import UIKit
import gdk

class FeeSummaryCell: UITableViewCell {

    @IBOutlet weak var lblFeeTitle: UILabel!
    @IBOutlet weak var lblFeeAmount: UILabel!
    @IBOutlet weak var lblFeeFiat: UILabel!
    @IBOutlet weak var lblFeeInfo: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()

        lblFeeTitle.text = NSLocalizedString("id_fee", comment: "")
    }

    override func prepareForReuse() {
        lblFeeAmount.text = ""
        lblFeeFiat.text = ""
        lblFeeInfo.text = ""
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(_ tx: Transaction, inputDenomination: DenominationType) {
        if let balance = Balance.fromSatoshi(tx.fee, assetId: tx.subaccountItem?.gdkNetwork.getFeeAsset() ?? "btc") {
            let (amount, denom) = balance.toDenom(inputDenomination)
            lblFeeAmount.text = "\(amount) \(denom)"
            let (fiat, fiatCurrency) = balance.toFiat()
            lblFeeFiat.text = "≈ \(fiat) \(fiatCurrency)"
            lblFeeInfo.text = "\(String(format: "( %.2f satoshi / vbyte )", Double(tx.feeRate) / 1000))"
        }
    }
}
