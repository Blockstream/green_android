import UIKit

class TransactionFeeCell: UITableViewCell {

    @IBOutlet weak var lblFee: UILabel!
    @IBOutlet weak var lblValue: UILabel!
    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var feeBtnView: UIView!
    @IBOutlet weak var btnFee: UIButton!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    func configure() {
        lblFee.text = "Fee"
        lblValue.text = "0.000001 BTC"
        lblFiat.text = "0000 FIAT"
        lblHint.text = "satoshi/vbyte"
        btnFee.setTitle("Increase fee »", for: .normal)
        btnFee.setStyle(.primary)
    }
}
