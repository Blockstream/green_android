import UIKit

protocol FeeEditCellDelegate: AnyObject {
    func setCustomFee()
    func updatePriority(_ priority: TransactionPriority)
}

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
    @IBOutlet weak var lblInvalidFee: UILabel!

    var fee: UInt64?
    var feeRate: UInt64?
    var txError: String?
    var transactionPriority: TransactionPriority?
    weak var delegate: FeeEditCellDelegate?

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
        lblFeeTitle.text = NSLocalizedString("id_fee", comment: "")
        lblTimeTitle.text = "Confirmation Time"
        lblTimeHint.text = ""
        lblTipCustom.text = NSLocalizedString("id_custom", comment: "")
        lblTipLow.text = NSLocalizedString("id_low", comment: "")
        lblTipMedium.text = NSLocalizedString("id_medium", comment: "")
        lblTipHigh.text = NSLocalizedString("id_high", comment: "")
        lblInvalidFee.text = NSLocalizedString("id_invalid_replacement_fee_rate", comment: "")
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

    func configure(fee: UInt64?,
                   feeRate: UInt64?,
                   txError: String?,
                   transactionPriority: TransactionPriority
    ) {
        self.fee = fee
        self.feeRate = feeRate
        self.txError = txError
        lblFeeValue.isHidden = true
        lblFeeRate.isHidden = true
        lblFeeFiat.isHidden = true
        lblInvalidFee.isHidden = true

        lblTimeHint.text = transactionPriority == .Custom ? NSLocalizedString("id_custom", comment: "") : "~ \(transactionPriority.time)"
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

        if ((txError ?? "").isEmpty || txError == "id_invalid_replacement_fee_rate"), let fee = fee, let feeRate = feeRate {
            if let balance = Balance.convert(details: ["satoshi": fee]) {
                let (amount, denom) = balance.get(tag: btc)
                lblFeeValue.text = "\(amount ?? "") \(denom)"
                let (fiat, fiatCurrency) = balance.get(tag: "fiat")
                lblFeeFiat.text = "≈ \(fiat ?? "N.A.") \(fiatCurrency)"
                lblFeeRate.text = "\(String(format: "( %.2f satoshi / vbyte )", Double(feeRate) / 1000))"
                lblFeeValue.isHidden = false
                lblFeeRate.isHidden = false
                lblFeeFiat.isHidden = false
            }
        }
        lblInvalidFee.isHidden = !(txError == "id_invalid_replacement_fee_rate")
    }

    func setPriority(_ switchIndex: Int) {
        let tp = switchIndexToFee(switchIndex)
        lblTimeHint.text = tp == .Custom ? NSLocalizedString("id_custom", comment: "") : "~ \(tp.time)"
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
        delegate?.setCustomFee()
    }

    @IBAction func feeSlider(_ sender: UISlider) {
        let step = Float(1)
        let roundedValue = round(sender.value / step) * step
        sender.value = roundedValue
        delegate?.updatePriority(switchIndexToFee(Int(sender.value)))
    }
}
