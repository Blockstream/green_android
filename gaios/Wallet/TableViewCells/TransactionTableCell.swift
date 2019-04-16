import Foundation
import UIKit

class TransactionTableCell: UITableViewCell {

    @IBOutlet weak var status: UILabel!
    @IBOutlet weak var address: UILabel!
    @IBOutlet weak var amount: UILabel!
    @IBOutlet weak var date: UILabel!
    @IBOutlet weak var bumpFee: UIImageView!

    override func layoutSubviews() {
        super.layoutSubviews()
    }

    func setup(with transaction: Transaction) {
        bumpFee.isHidden = !transaction.canRBF
        amount.text = transaction.amount()
        selectionStyle = .none
        date.text = transaction.date()
        if !transaction.memo.isEmpty {
            address.text = transaction.memo
        } else if transaction.type == "redeposit" {
            address.text = NSLocalizedString("id_redeposited", comment: String())
        } else if transaction.type == "incoming" {
            address.text = NSLocalizedString("id_received", comment: String())
        } else {
            address.text = transaction.address()
        }
    }

    func checkBlockHeight(transaction: Transaction, blockHeight: UInt32) {
        if transaction.blockHeight == 0 {
            status.text = NSLocalizedString("id_unconfirmed", comment: "")
            status.textColor = UIColor.errorRed()
        } else if blockHeight < transaction.blockHeight + 5 {
            let confirmCount = blockHeight - transaction.blockHeight + 1
            status.textColor = UIColor.customTitaniumLight()
            status.text = String(format: NSLocalizedString("id_d6_confirmations", comment: ""), confirmCount)
        } else {
            status.text = NSLocalizedString("id_completed", comment: "")
            status.textColor = UIColor.customTitaniumLight()
        }
    }

    func checkTransactionType(transaction: Transaction) {
        if transaction.type == "incoming" {
            amount.textColor = UIColor.customMatrixGreen()
        } else {
            amount.textColor = UIColor.white
        }
    }
}
