import UIKit

enum TransactionStatus {
    case confirmed
    case confirming
    case unconfirmed
}

class TransactionStatusTableCell: UITableViewCell {

    @IBOutlet weak var statusLabel: UILabel!
    @IBOutlet weak var statusImageView: UIImageView!
    @IBOutlet weak var increaseFeeButton: UIButton!
    @IBOutlet weak var increaseFeeStackView: UIStackView!

    func configure(for transaction: Transaction, isLiquid: Bool) {
        let blockHeight = SessionManager.shared.blockHeight
        var status: TransactionStatus = .unconfirmed
        if transaction.blockHeight == 0 {
            statusLabel.text = NSLocalizedString("id_unconfirmed", comment: "")
            statusLabel.textColor = UIColor.errorRed()
        } else if isLiquid && blockHeight < transaction.blockHeight + 1 {
            status = .confirming
            statusLabel.textColor = UIColor.customTitaniumLight()
            statusLabel.text = NSLocalizedString("id_12_confirmations", comment: "")
        } else if !isLiquid && blockHeight < transaction.blockHeight + 5 {
            if blockHeight >= transaction.blockHeight {
                status = .confirming
                let confirmCount = (blockHeight - transaction.blockHeight) + 1
                statusLabel.textColor = UIColor.customTitaniumLight()
                statusLabel.text = String(format: NSLocalizedString("id_d6_confirmations", comment: ""), confirmCount)
            }
        } else {
            status = .confirmed
            statusLabel.textColor = UIColor.customMatrixGreen()
            statusLabel.text = NSLocalizedString("id_completed", comment: "")
        }
        let isWatchonly = AccountsManager.shared.current?.isWatchonly ?? false
        let showBumpFee = !isLiquid && transaction.canRBF && !isWatchonly && !(SessionManager.shared.twoFactorReset?.isResetActive ?? false)
        statusImageView.isHidden = !(status == .confirmed)
        increaseFeeStackView.isHidden = !showBumpFee
    }
}
