import UIKit
import gdk

protocol FeeEditCellDelegate: AnyObject {
    func setCustomFee()
    func updatePriority(_ priority: TransactionPriority)
}

struct FeeEditCellModel {
    var fee: UInt64?
    var feeRate: UInt64?
    var txError: String?
    var customFee: UInt64 = 1000
    var feeEstimates: [UInt64?]
    var inputDenomination: DenominationType
    var transactionPriority: TransactionPriority?
    
    func selectedFee() -> Int {
        switch transactionPriority {
        case .High:
            return 0
        case .Medium:
            return 1
        case .Low:
            return 2
        case .Custom:
            return 3
        case .none:
            return 0
        }
    }
}

class FeeEditCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
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

    private weak var delegate: FeeEditCellDelegate?

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        let icons = [icon1, icon2, icon3, icon4]
        icons.forEach { icon in
            if let icon = icon {
                icon.image = icon.image?.maskWithColor(color: UIColor.customMatrixGreen())
            }
        }
        lblTimeTitle.text = NSLocalizedString("id_confirmation_time", comment: "")
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

    func configure(tx: Transaction?, cellModel: FeeEditCellModel, delegate: FeeEditCellDelegate) {
        lblFeeValue.isHidden = true
        lblFeeRate.isHidden = true
        lblFeeFiat.isHidden = true
        lblInvalidFee.isHidden = true
        self.delegate = delegate

        let estimateConfirmTime = cellModel.transactionPriority?.time(isLiquid: WalletManager.current?.account.gdkNetwork.liquid ?? false)
        lblTimeHint.text = cellModel.transactionPriority == .Custom ? "id_custom".localized : "~ \(estimateConfirmTime ?? "")"
        feeSlider.value = Float(feeToSwitchIndex(cellModel.transactionPriority ?? .Low))

        switch cellModel.transactionPriority {
        case .Low:
            lblTipLow.textColor = UIColor.customMatrixGreen()
        case .Medium:
            lblTipMedium.textColor = UIColor.customMatrixGreen()
        case .High:
            lblTipHigh.textColor = UIColor.customMatrixGreen()
        case .Custom:
            lblTipCustom.textColor = UIColor.customMatrixGreen()
        case .none:
            break
        }

        let btc = tx?.subaccountItem?.gdkNetwork.getFeeAsset() ?? "btc"
        if ((cellModel.txError ?? "").isEmpty || cellModel.txError == "id_invalid_replacement_fee_rate"), let fee = cellModel.fee, let feeRate = cellModel.feeRate {
            if let balance = Balance.fromSatoshi(fee, assetId: btc) {
                let (amount, denom) = balance.toDenom(cellModel.inputDenomination)
                lblFeeValue.text = "\(amount) \(denom)"
                let (fiat, fiatCurrency) = balance.toFiat()
                lblFeeFiat.text = "≈ \(fiat) \(fiatCurrency)"
                lblFeeRate.text = "\(String(format: "( %.2f satoshi / vbyte )", Double(feeRate) / 1000))"
                lblFeeValue.isHidden = false
                lblFeeRate.isHidden = false
                lblFeeFiat.isHidden = false
            }
        }
        lblInvalidFee.isHidden = !(cellModel.txError == "id_invalid_replacement_fee_rate")
        btnCustomFee.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.setCutomFeeBtn
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
