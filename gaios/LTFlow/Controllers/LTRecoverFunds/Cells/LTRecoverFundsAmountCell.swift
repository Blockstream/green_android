import UIKit

class LTRecoverFundsAmountCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var amountTextField: UITextField!
    @IBOutlet weak var denominationLabel: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(amount: UInt64) {
        amountTextField.text = "\(amount)"
        denominationLabel.text = "sats"
    }
}
