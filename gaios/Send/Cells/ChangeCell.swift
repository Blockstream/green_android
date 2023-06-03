import UIKit
import gdk

class ChangeCell: UITableViewCell {

    @IBOutlet weak var lblChangeTitle: UILabel!
    @IBOutlet weak var lblChangeHint: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func prepareForReuse() {
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(_ transaction: Transaction) {
        lblChangeTitle.text = NSLocalizedString("id_change", comment: "")
        lblChangeHint.text = "-"
        if let outputs = transaction.transactionOutputs, !outputs.isEmpty {
            var changeAddress = [String]()
            outputs.forEach { output in
                if output.isChange, let address = output.address {
                    changeAddress.append(address)
                }
            }
            lblChangeHint.text = changeAddress.map { "\($0)"}.joined(separator: "\n")
        }
    }
}
