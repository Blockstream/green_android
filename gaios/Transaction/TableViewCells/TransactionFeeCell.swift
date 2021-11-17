import UIKit

class TransactionFeeCell: UITableViewCell {

    @IBOutlet weak var lblFee: UILabel!
    @IBOutlet weak var lblValue: UILabel!
    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var feeBtnView: UIView!
    @IBOutlet weak var btnFee: UIButton!

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    override func prepareForReuse() {
        lblValue.text = ""
        lblFiat.text = ""
        lblHint.text = ""
    }

    func configure(transaction: Transaction, isLiquid: Bool) {
        lblFee.text = NSLocalizedString("id_fee", comment: "")

        btnFee.setTitle(NSLocalizedString("id_increase_fee", comment: "") + " »", for: .normal)
        btnFee.setStyle(.primary)

        if let balance = Balance.convert(details: ["satoshi": transaction.fee]) {
            let (amount, denom) = balance.get(tag: btc)
            lblValue.text = "\(amount ?? "") \(denom)"
            if let fiat = balance.fiat {
                lblFiat.text = "≈ \(fiat) \(balance.fiatCurrency)"
            }
            lblHint.text = "\(String(format: "( %.2f satoshi / vbyte )", Double(transaction.feeRate) / 1000))"
        }
        let isWatchonly = AccountsManager.shared.current?.isWatchonly ?? false
        let showBumpFee = !isLiquid && transaction.canRBF && !isWatchonly && !(SessionManager.shared.isResetActive ?? false)
        feeBtnView.isHidden = !showBumpFee
    }
}
