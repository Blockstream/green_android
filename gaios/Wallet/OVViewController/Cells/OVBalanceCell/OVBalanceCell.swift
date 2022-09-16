import UIKit

class OVBalanceCell: UITableViewCell {

    @IBOutlet weak var lblBalanceTitle: UILabel!
    @IBOutlet weak var lblBalanceValue: UILabel!
    @IBOutlet weak var lblAssetsNum: UILabel!
    @IBOutlet weak var lblAssetsOther: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(lblBalanceTitle: String,
                   lblBalanceValue: String,
                   lblAssetsNum: String,
                   lblAssetsOther: String
    ) {
        self.lblBalanceTitle.text = lblBalanceTitle
        self.lblBalanceValue.text = lblBalanceValue
        self.lblAssetsNum.text = lblAssetsNum
        self.lblAssetsOther.text = lblAssetsOther
    }
}
