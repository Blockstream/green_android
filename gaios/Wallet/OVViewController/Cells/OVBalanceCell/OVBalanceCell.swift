import UIKit

class OVBalanceCell: UITableViewCell {

    @IBOutlet weak var lblBalanceTitle: UILabel!
    @IBOutlet weak var lblBalanceValue: UILabel!
    @IBOutlet weak var lblAssetsNum: UILabel!
    @IBOutlet weak var lblAssetsOther: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblBalanceTitle.text = "Total Balance"
        lblAssetsOther.text = "other assets"
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    var viewModel: OVBalanceCellModel? {
        didSet {
            lblBalanceValue.text = viewModel?.value
            lblAssetsNum.text = "\(viewModel?.numAssets ?? 0)"
        }
    }
}
