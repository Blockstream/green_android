import UIKit

class BalanceCell: UITableViewCell {

    @IBOutlet weak var lblBalanceTitle: UILabel!
    @IBOutlet weak var lblBalanceValue: UILabel!
    @IBOutlet weak var lblBalanceFiat: UILabel!
    @IBOutlet weak var btnAssets: UIButton!

    private var isFiat = false
    private var model: BalanceCellModel?
    private var onAssets: (() -> Void)?

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblBalanceTitle.text = "Total Balance"
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: BalanceCellModel, onAssets: (() -> Void)?) {
        lblBalanceTitle.text = "Total Balance"
        lblBalanceValue.text = model.value
        lblBalanceFiat.text = model.valueFiat
        let uLineAttr = [NSAttributedString.Key.underlineStyle: NSUnderlineStyle.thick.rawValue]
        let str = NSAttributedString(string: "\(model.numAssets) assets in total", attributes: uLineAttr)
        btnAssets.setAttributedTitle(str, for: .normal)
        self.onAssets = onAssets
    }

    @IBAction func btnFiat(_ sender: Any) {
        if isFiat {
            if let value = model?.value {
                lblBalanceValue.text = value
                isFiat = !isFiat
            }
        } else {
            if let fiatValue = model?.fiatValue {
                lblBalanceValue.text = fiatValue
                isFiat = !isFiat
            }
        }
    }

    @IBAction func btnAssets(_ sender: Any) {
        onAssets?()
    }
}
