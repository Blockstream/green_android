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

    var setCustomFee: VoidToVoid?
    var updatePriority: ((TransactionPriority) -> Void)?

    private var defaultFee: TransactionPriority = {
        guard let settings = SessionManager.shared.settings else { return .High }
        if let pref = TransactionPriority.getPreference() {
            settings.transactionPriority = pref
        }
        return settings.transactionPriority
    }()

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
        lblFeeValue.isHidden = true
        lblFeeRate.isHidden = true
        lblFeeFiat.isHidden = true
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
    }

    func configure(setCustomFee: VoidToVoid?, updatePriority: ((TransactionPriority) -> Void)?) {
        self.setCustomFee = setCustomFee
        self.updatePriority = updatePriority

        setPriority(feeToSwitchIndex(defaultFee))
        feeSlider.value = Float(feeToSwitchIndex(defaultFee))
        updatePriority?(defaultFee)
    }

    func setPriority(_ switchIndex: Int) {
        let tp = switchIndexToFee(switchIndex)
        lblTimeHint.text = tp == .Custom ? "Custom" : "~ \(tp.time)"
        updatePriority?(tp)
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

    func onChabge() {

    }

    @IBAction func btnCustomFee(_ sender: Any) {
        setCustomFee?()
    }

    @IBAction func feeSlider(_ sender: UISlider) {
        let step = Float(1)
        let roundedValue = round(sender.value / step) * step
        sender.value = roundedValue
        setPriority(Int(sender.value))
    }
}
