import UIKit

class FeeSummaryCell: UITableViewCell {

    @IBOutlet weak var lblFeeTitle: UILabel!
    @IBOutlet weak var lblFeeAmount: UILabel!
    @IBOutlet weak var lblFeeFiat: UILabel!
    @IBOutlet weak var lblFeeInfo: UILabel!

    private var btc: String {
        return AccountDao.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

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

    func configure(_ tx: Transaction) {
        if let balance = Balance.fromSatoshi(tx.fee) {
            let (amount, denom) = balance.toDenom()
            lblFeeAmount.text = "\(amount) \(denom)"
            let (fiat, fiatCurrency) = balance.toFiat()
            lblFeeFiat.text = "≈ \(fiat) \(fiatCurrency)"
            lblFeeInfo.text = "\(String(format: "( %.2f satoshi / vbyte )", Double(tx.feeRate) / 1000))"
        }
    }
}
