import UIKit

class TransactionAmountCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var bg: UIView!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    func configure() {
        lblTitle.text = "Received"
        lblAsset.text = "BTC"
        lblAmount.text = "0.000001"
        lblFiat.text = "0000 FIAT"
    }
}
