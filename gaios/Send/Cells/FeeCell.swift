import UIKit

class FeeCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblFeeTitle: UILabel!
    @IBOutlet weak var btnCustomFee: UIButton!
    @IBOutlet weak var lblTimeTitle: UILabel!
    @IBOutlet weak var lblTimeHint: UILabel!
    @IBOutlet weak var feeSlider: UISlider!

    @IBOutlet weak var icon1: UIImageView!
    @IBOutlet weak var icon2: UIImageView!
    @IBOutlet weak var icon3: UIImageView!
    @IBOutlet weak var icon4: UIImageView!

    var setCustomFee: VoidToVoid?

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
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
    }

    func configure(setCustomFee: VoidToVoid?) {
        self.setCustomFee = setCustomFee
        setPriority(2)
    }

    func setPriority(_ index: Int) {
        let tp = getPriority(index)
        lblTimeHint.text = tp == .Custom ? "" : "~ \(tp.time)"
    }

    func getPriority(_ index: Int) -> TransactionPriority {
        // [3, 12, 24, 0]
        switch index {
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
