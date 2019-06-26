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
        var status: TransactionStatus = .unconfirmed
        if transaction.blockHeight == 0 {
            statusLabel.textColor = UIColor.errorRed()
            statusLabel.text = NSLocalizedString("id_unconfirmed", comment: "")
        } else if isLiquid && getGAService().getBlockheight() - transaction.blockHeight < 1 {
            status = .confirming
            statusLabel.textColor = UIColor.customTitaniumLight()
            statusLabel.text = NSLocalizedString("id_12_confirmations", comment: "")
        } else if !isLiquid && getGAService().getBlockheight() - transaction.blockHeight < 5 {
            status = .confirming
            statusLabel.textColor = UIColor.customTitaniumLight()
            let blocks = getGAService().getBlockheight() - transaction.blockHeight + 1
            statusLabel.text = String(format: NSLocalizedString("id_d6_confirmations", comment: ""), blocks)
        } else {
            status = .confirmed
            statusLabel.textColor = UIColor.customMatrixGreen()
            statusLabel.text = NSLocalizedString("id_completed", comment: "")
        }

        let showBumpFee = !isLiquid && transaction.canRBF && !getGAService().isWatchOnly && !getGAService().getTwoFactorReset()!.isResetActive
        statusImageView.isHidden = !(status == .confirmed)
        increaseFeeStackView.isHidden = !showBumpFee
    }
}
