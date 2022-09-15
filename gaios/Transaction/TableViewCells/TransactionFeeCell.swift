import UIKit

class TransactionFeeCell: UITableViewCell {

    @IBOutlet weak var lblFee: UILabel!
    @IBOutlet weak var lblValue: UILabel!
    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var feeBtnView: UIView!
    @IBOutlet weak var btnFee: UIButton!
    @IBOutlet weak var copyIcon: UIImageView!

    var feeAction: VoidToVoid?
    var copyFee: ((String) -> Void)?
    var amount: String?

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

    func configure(transaction: Transaction, isLiquid: Bool, feeAction: VoidToVoid?, copyFee: ((String) -> Void)?) {

        self.copyFee = copyFee
        let color: UIColor = .white
        copyIcon.image = copyIcon.image?.maskWithColor(color: color)
        lblFee.text = NSLocalizedString("id_fee", comment: "")

        btnFee.setTitle(NSLocalizedString("id_increase_fee", comment: "") + " »", for: .normal)
        btnFee.setStyle(.primary)
        self.feeAction = feeAction

        if let balance = Balance.fromSatoshi(transaction.fee) {
            let (amount, denom) = balance.toValue()
            let (fiat, fiatCurrency) = balance.toFiat()
            lblValue.text = "\(amount) \(denom)"
            self.amount = amount
            lblFiat.text = "≈ \(fiat) \(fiatCurrency)"
            lblHint.text = "\(String(format: "( %.2f satoshi / vbyte )", Double(transaction.feeRate) / 1000))"
        }
        let isWatchonly = AccountsManager.shared.current?.isWatchonly ?? false
        let showBumpFee = !isLiquid && transaction.canRBF && !isWatchonly && !(WalletManager.current?.currentSession?.isResetActive ?? false)
        feeBtnView.isHidden = !showBumpFee
    }

    @IBAction func btnFee(_ sender: Any) {
        feeAction?()
    }

    @IBAction func copyFeeBtn(_ sender: Any) {
        copyFee?(amount ?? "")
    }

}
