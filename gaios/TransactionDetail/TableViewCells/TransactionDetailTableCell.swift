import UIKit

class TransactionDetailTableCell: UITableViewCell {

    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var detailLabel: CopyableLabel!
    @IBOutlet weak var titleLabelHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var notesImageView: UIImageView!

    private var btc: String {
        return getGdkNetwork(getNetwork()).getFeeAsset()
    }

    override func prepareForReuse() {
        titleLabel.text = ""
        detailLabel.text = ""
    }

    func configure(for transaction: Transaction, cellType: TransactionDetailCellType, walletName: String = "") {
        notesImageView.isHidden = true
        switch cellType {
        case .fee:
            titleLabel.text = NSLocalizedString("id_fee", comment: "")
            if let balance = Balance.convert(details: ["satoshi": transaction.fee]) {
                let (amount, denom) = balance.get(tag: btc)
                detailLabel.text = "\(amount ?? "") \(denom) \(String(format: "( %.2f sat / vbyte )", Double(transaction.feeRate) / 1000))"
            }
        case .amount:
            titleLabel.text = NSLocalizedString("id_amount", comment: "")
            if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
                let (amount, denom) = balance.get(tag: btc)
                detailLabel.text = String(format: "%@%@ %@", transaction.type == "outgoing" || transaction.type == "redeposit" ? "-" : "", amount ?? "", denom)
            }
        case .notes:
            notesImageView.isHidden = false
            titleLabel.text = NSLocalizedString("id_my_notes", comment: "").localizedCapitalized
            let memo = transaction.memo
            if memo.isEmpty {
                detailLabel.text = NSLocalizedString("id_add_a_note_only_you_can_see_it", comment: "")
                detailLabel.textColor = UIColor.customTitaniumMedium()
            } else {
                detailLabel.text = memo
                detailLabel.textColor = UIColor.white
            }
        case .txident:
            titleLabel.text = NSLocalizedString("id_transaction_id", comment: "")
            detailLabel.text = transaction.hash
            detailLabel.numberOfLines = 4
            detailLabel.lineBreakMode = .byCharWrapping
        case .recipient:
            titleLabel.text = NSLocalizedString("id_recipient", comment: "")
            detailLabel.text = transaction.address()
        case .wallet:
            titleLabel.text = NSLocalizedString("id_received_on", comment: "")
            detailLabel.text = walletName
        default:
            break
        }
    }
}
