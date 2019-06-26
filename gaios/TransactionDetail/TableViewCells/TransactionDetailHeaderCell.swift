import UIKit

class TransactionDetailHeaderCell: UITableViewCell {
    @IBOutlet weak var dateTimeLabel: UILabel!

    func configure(with dateString: String) {
        dateTimeLabel.text = dateString
    }
}
