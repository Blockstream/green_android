import UIKit

class TransactionFeeCell: UITableViewCell {

    @IBOutlet weak var lblFee: UILabel!
    @IBOutlet weak var lblValue: UILabel!
    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var feeBtnView: UIView!
    @IBOutlet weak var btnFee: UIButton!
    @IBOutlet weak var copyIcon: UIImageView!
    @IBOutlet weak var bg: UIView!

    var feeAction: VoidToVoid?
    var copyFee: ((String) -> Void)?
    var amount: String?

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.layer.cornerRadius = 5.0
        lblFee.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        lblFee.textColor = .white.withAlphaComponent(0.4)
        lblValue.font = UIFont.systemFont(ofSize: 14.0, weight: .semibold)
        lblFiat.font = UIFont.systemFont(ofSize: 12.0, weight: .semibold)
        lblFiat.textColor = .white.withAlphaComponent(0.4)
        lblHint.font = UIFont.systemFont(ofSize: 12.0, weight: .semibold)
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

    func configure(transaction: Transaction,
                   isLiquid: Bool,
                   hideBalance: Bool,
                   feeAction: VoidToVoid?, copyFee: ((String) -> Void)?) {

        self.copyFee = copyFee
        let color: UIColor = .white
        copyIcon.image = copyIcon.image?.maskWithColor(color: color)
        lblFee.text = NSLocalizedString("id_network_fee", comment: "")

        btnFee.setTitle(NSLocalizedString("id_increase_fee", comment: "") + " »", for: .normal)
        btnFee.setStyle(.primary)
        self.feeAction = feeAction

        if let balance = Balance.fromSatoshi(transaction.fee, assetId: transaction.subaccountItem?.gdkNetwork.getFeeAsset() ?? "btc") {
            let (amount, denom) = balance.toValue()
            let (fiat, fiatCurrency) = balance.toFiat()
            lblValue.text = "\(amount) \(denom)"
            self.amount = amount
            lblFiat.text = "≈ \(fiat) \(fiatCurrency)"
            lblHint.text = "\(String(format: "( %.2f satoshi / vbyte )", Double(transaction.feeRate) / 1000))"
            if hideBalance {
                lblValue.attributedText = Common.obfuscate(color: color, size: 10, length: 5)
                lblFiat.attributedText = Common.obfuscate(color: color, size: 10, length: 5)
            }
        }
        let isWatchonly = WalletManager.current?.account.isWatchonly ?? false
        let showBumpFee = !isLiquid && transaction.canRBF && !isWatchonly && !(subaccount(from: transaction)?.session?.isResetActive ?? false)
        feeBtnView.isHidden = !showBumpFee
    }

    func subaccount(from tx: Transaction) -> WalletItem? {
        return WalletManager.current?.subaccounts.filter { $0.hashValue == tx.subaccount }.first
    }

    @IBAction func btnFee(_ sender: Any) {
        feeAction?()
    }

    @IBAction func copyFeeBtn(_ sender: Any) {
        copyFee?(amount ?? "")
    }

}
