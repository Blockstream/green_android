import UIKit

class TransactionStatusCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblDate: UILabel!
    @IBOutlet weak var lblStatus: UILabel!
    @IBOutlet weak var iconCheck: UIImageView!
    @IBOutlet weak var lblStep: UILabel!
    @IBOutlet weak var arc: UIView!

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.layer.borderWidth = 1.0
        bg.layer.borderColor = UIColor.gray.cgColor
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(transaction: Transaction, isLiquid: Bool) {
        lblDate.text = transaction.date(dateStyle: .long, timeStyle: .short)

        var step: UInt32 = 0
        var steps: UInt32 = 0
        steps = isLiquid ? 2 : 6

        let blockHeight = SessionManager.shared.notificationManager.blockHeight
        var status: TransactionStatus = .unconfirmed
        if transaction.blockHeight == 0 {
            lblStatus.text = NSLocalizedString("id_unconfirmed", comment: "")
            lblStatus.textColor = UIColor.errorRed()
        } else if isLiquid && blockHeight < transaction.blockHeight + 1 {
            step = (blockHeight - transaction.blockHeight) + 1
            status = .confirming
            lblStatus.textColor = UIColor.customTitaniumLight()
            lblStatus.text = NSLocalizedString("id_pending_confirmation", comment: "")
            // NSLocalizedString("id_12_confirmations", comment: "")
        } else if !isLiquid && blockHeight < transaction.blockHeight + 5 {
            if blockHeight >= transaction.blockHeight {
                status = .confirming
                step = (blockHeight - transaction.blockHeight) + 1
                lblStatus.textColor = UIColor.customTitaniumLight()
                lblStatus.text = NSLocalizedString("id_pending_confirmation", comment: "")
                //String(format: NSLocalizedString("id_d6_confirmations", comment: ""), confirmCount)
            }
        } else {
            status = .confirmed
            lblStatus.textColor = UIColor.customMatrixGreen()
            lblStatus.text = NSLocalizedString("id_completed", comment: "")
        }

        lblStep.isHidden = status == .confirmed
        arc.subviews.forEach { $0.removeFromSuperview() }
        iconCheck.isHidden = !(status == .confirmed)
        if status != .confirmed {
            lblStep.text = "\(step)/\(steps)"
            arc.addSubview(ArcView(frame: arc.frame, num: Int(step)))
        } else {
            arc.addSubview(ArcView(frame: arc.frame, num: Int(steps)))
        }

//        // SPV verify
//        spvVerifyLabel.isHidden = ["disabled", "unconfirmed", nil].contains(transaction.spvVerified)
//        switch transaction.spvVerified {
//        case "verified":
//            spvVerifyLabel.textColor = .green
//            spvVerifyLabel.text = NSLocalizedString("id_verified", comment: "")
//        case "not_verified":
//            spvVerifyLabel.textColor = .red
//            spvVerifyLabel.text = NSLocalizedString("id_invalid_merkle_proof", comment: "")
//        case "not_longest":
//            spvVerifyLabel.textColor = .yellow
//            spvVerifyLabel.text = NSLocalizedString("id_not_on_longest_chain", comment: "")
//        default:
//            spvVerifyLabel.textColor = .white
//            spvVerifyLabel.text = NSLocalizedString("id_verifying_transactions", comment: "")
//        }
    }
}
