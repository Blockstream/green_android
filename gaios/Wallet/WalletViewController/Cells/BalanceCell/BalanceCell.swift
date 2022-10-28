import UIKit

class BalanceCell: UITableViewCell {

    @IBOutlet weak var lblBalanceTitle: UILabel!
    @IBOutlet weak var lblBalanceValue: UILabel!
    @IBOutlet weak var lblAssetsNum: UILabel!
    @IBOutlet weak var lblAssetsOther: UILabel!

    var isFiat = false

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblBalanceTitle.text = "Total Balance"
        lblAssetsOther.text = "other assets"
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    var viewModel: BalanceCellModel? {
        didSet {
            lblBalanceValue.text = viewModel?.value
            lblAssetsNum.text = "\(viewModel?.numAssets ?? 0)"
        }
    }

    @IBAction func btnFiat(_ sender: Any) {
        if isFiat {
            if let value = viewModel?.value {
                lblBalanceValue.text = value
                isFiat = !isFiat
            }
        } else {
            if let fiatValue = viewModel?.fiatValue {
                lblBalanceValue.text = fiatValue
                isFiat = !isFiat
            }
        }
    }
}
