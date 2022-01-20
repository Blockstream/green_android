import UIKit

class FeeEditCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblFeeTitle: UILabel!
    @IBOutlet weak var lblFeeValue: UILabel!
    @IBOutlet weak var lblFeeRate: UILabel!
    @IBOutlet weak var lblFeeFiat: UILabel!
    @IBOutlet weak var btnCustomFee: UIButton!
    @IBOutlet weak var lblTimeTitle: UILabel!
    @IBOutlet weak var lblTimeHint: UILabel!
    @IBOutlet weak var feeSlider: UISlider!

    @IBOutlet weak var icon1: UIImageView!
    @IBOutlet weak var icon2: UIImageView!
    @IBOutlet weak var icon3: UIImageView!
    @IBOutlet weak var icon4: UIImageView!

    @IBOutlet weak var lblTipCustom: UILabel!
    @IBOutlet weak var lblTipLow: UILabel!
    @IBOutlet weak var lblTipMedium: UILabel!
    @IBOutlet weak var lblTipHigh: UILabel!

    var transaction: Transaction?
    var setCustomFee: VoidToVoid?
    var updatePriority: ((TransactionPriority) -> Void)?
    var transactionPriority: TransactionPriority?

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 8.0
        let icons = [icon1, icon2, icon3, icon4]
        icons.forEach { icon in
            if let icon = icon {
                icon.image = icon.image?.maskWithColor(color: UIColor.customMatrixGreen())
            }
        }
        lblFeeTitle.text = "Fee"
        lblTimeTitle.text = "Confirmation Time"
        lblTimeHint.text = ""
        lblTipCustom.text = NSLocalizedString("id_custom", comment: "")
        lblTipLow.text = NSLocalizedString("id_low", comment: "")
        lblTipMedium.text = NSLocalizedString("id_medium", comment: "")
        lblTipHigh.text = NSLocalizedString("id_high", comment: "")
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
        lblTipLow.textColor = .white
        lblTipMedium.textColor = .white
        lblTipHigh.textColor = .white
        lblTipCustom.textColor = .white
    }

    func configure(transaction: Transaction?,
                   setCustomFee: VoidToVoid?,
                   updatePriority: ((TransactionPriority) -> Void)?,
                   transactionPriority: TransactionPriority
    ) {
        self.setCustomFee = setCustomFee
        self.updatePriority = updatePriority
        self.transaction = transaction
        lblFeeValue.isHidden = true
        lblFeeRate.isHidden = true
        lblFeeFiat.isHidden = true

        lblTimeHint.text = transactionPriority == .Custom ? "Custom" : "~ \(transactionPriority.time)"
        feeSlider.value = Float(feeToSwitchIndex(transactionPriority))

        switch transactionPriority {
        case .Low:
            lblTipLow.textColor = UIColor.customMatrixGreen()
        case .Medium:
            lblTipMedium.textColor = UIColor.customMatrixGreen()
        case .High:
            lblTipHigh.textColor = UIColor.customMatrixGreen()
        case .Custom:
            lblTipCustom.textColor = UIColor.customMatrixGreen()
        }

        if let tx = transaction, tx.error.isEmpty {
            if let balance = Balance.convert(details: ["satoshi": tx.fee]) {
                let (amount, denom) = balance.get(tag: btc)
                lblFeeValue.text = "\(amount ?? "") \(denom)"
                let (fiat, fiatCurrency) = balance.get(tag: "fiat")
                lblFeeFiat.text = "≈ \(fiat ?? "N.A.") \(fiatCurrency)"
                lblFeeRate.text = "\(String(format: "( %.2f satoshi / vbyte )", Double(tx.feeRate) / 1000))"
                lblFeeValue.isHidden = false
                lblFeeRate.isHidden = false
                lblFeeFiat.isHidden = false
            }
        }

    }

    func setPriority(_ switchIndex: Int) {
        let tp = switchIndexToFee(switchIndex)
        lblTimeHint.text = tp == .Custom ? "Custom" : "~ \(tp.time)"
    }

    func feeToSwitchIndex(_ fee: TransactionPriority) -> Int {
        switch fee {
        case .High:
            return 3
        case .Medium:
            return 2
        case .Low:
            return 1
        case .Custom:
            return 0
        }
    }

    func switchIndexToFee(_ switchIndex: Int) -> TransactionPriority {
        // [3, 12, 24, 0]
        switch switchIndex {
        case 3:
            return TransactionPriority.High
        case 2:
            return TransactionPriority.Medium
        case 1:
            return TransactionPriority.Low
        default:
            return TransactionPriority.Custom
        }
    }

    func onChange() {

    }

    @IBAction func btnCustomFee(_ sender: Any) {
        setCustomFee?()
    }

    @IBAction func feeSlider(_ sender: UISlider) {
        let step = Float(1)
        let roundedValue = round(sender.value / step) * step
        sender.value = roundedValue
        updatePriority?(switchIndexToFee(Int(sender.value)))
    }
}
