import UIKit

class TransactionStatusCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblDate: UILabel!
    @IBOutlet weak var lblStatus: UILabel!
    @IBOutlet weak var iconCheck: UIImageView!
    @IBOutlet weak var lblStep: UILabel!
    @IBOutlet weak var arc: UIView!
    @IBOutlet weak var spvVerifyIcon: UIImageView!

    enum TransactionStatus {
        case confirmed
        case confirming
        case unconfirmed
    }

    let loadingIndicator: ProgressView = {
        let progress = ProgressView(colors: [UIColor.customMatrixGreen()], lineWidth: 2)
        progress.translatesAutoresizingMaskIntoConstraints = false
        return progress
    }()

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.layer.borderWidth = 1.0
        bg.layer.borderColor = UIColor.gray.cgColor
        bg.layer.cornerRadius = 5.0
    }

    override func prepareForReuse() {
//       stop()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(transaction: Transaction, isLiquid: Bool, blockHeight: UInt32) {
        lblDate.text = transaction.date(dateStyle: .long, timeStyle: .short)

        var step: Int = 0
        var steps: Int = 0
        steps = isLiquid ? 2 : 6

        var status: TransactionStatus = .unconfirmed
        if transaction.blockHeight == 0 {
            lblStatus.text = NSLocalizedString("id_unconfirmed", comment: "")
            lblStatus.textColor = UIColor.errorRed()
        } else if isLiquid && blockHeight < transaction.blockHeight + 1 {
            step = Int(blockHeight) - Int(transaction.blockHeight) + 1
            status = .confirming
            lblStatus.textColor = UIColor.customTitaniumLight()
            lblStatus.text = NSLocalizedString("id_pending_confirmation", comment: "")
        } else if !isLiquid && blockHeight < transaction.blockHeight + 5 {
            if blockHeight >= transaction.blockHeight {
                status = .confirming
                step = Int(blockHeight) - Int(transaction.blockHeight) + 1
                lblStatus.textColor = UIColor.customTitaniumLight()
                lblStatus.text = NSLocalizedString("id_pending_confirmation", comment: "")
            }
        } else {
            status = .confirmed
            lblStatus.textColor = UIColor.customMatrixGreen()
            lblStatus.text = NSLocalizedString("id_completed", comment: "")
        }

        // status widget
        lblStep.isHidden = true
        iconCheck.isHidden = true
        if !shouldShowSpvStatus(tx: transaction) {
            lblStep.isHidden = status == .confirmed
            arc.subviews.forEach { $0.removeFromSuperview() }
            iconCheck.isHidden = !(status == .confirmed)

            arc.subviews.forEach { $0.removeFromSuperview() }
            if status != .confirmed {
                lblStep.text = "\(step)/\(steps)"
                arc.addSubview(ArcView(frame: arc.frame, step: Int(step), steps: Int(steps)))
            } else {
                arc.addSubview(ArcView(frame: arc.frame, step: Int(steps), steps: Int(steps)))
            }
        } else {
            setSpvVerifyIcon(tx: transaction)
            setLblStatus(transaction: transaction)
            if transaction.spvVerified == "verified" {
                stop()
                arc.subviews.forEach { $0.removeFromSuperview() }
                arc.addSubview(ArcView(frame: arc.frame, step: Int(steps), steps: Int(steps)))
                iconCheck.isHidden = false
            } else {
                if !loadingIndicator.isAnimating {
                    start()
                }
            }
        }
    }

    func shouldShowSpvStatus(tx: Transaction) -> Bool {
        if tx.spvVerified == "disabled" || tx.spvVerified == nil {
            return false
        }
        if tx.blockHeight == 0 {
            return false
        }
        if let account = subaccount(tx: tx), let notificationManager = account.session?.notificationManager,
           Int(notificationManager.blockHeight) - Int(tx.blockHeight) + 1 < 6 {
            return false
        }
        return true
    }

    func subaccount(tx: Transaction) -> WalletItem? {
        return WalletManager.current?.subaccounts.filter { $0.hashValue == tx.subaccount }.first
    }

    func setSpvVerifyIcon(tx: Transaction) {
        switch tx.spvVerified {
        case "disabled", "verified", nil:
            spvVerifyIcon.isHidden = true
        case "in_progress":
            spvVerifyIcon.isHidden = false
            spvVerifyIcon.image = UIImage(named: "ic_spv_progress")
            spvVerifyIcon.tintColor = .white
        case "not_verified":
            spvVerifyIcon.isHidden = false
            spvVerifyIcon.image = UIImage(named: "ic_spv_warning")
            spvVerifyIcon.tintColor = .red
        default:
            spvVerifyIcon.isHidden = false
            spvVerifyIcon.image = UIImage(named: "ic_spv_warning")
            spvVerifyIcon.tintColor = .yellow
        }
    }

    func setLblStatus(transaction: Transaction) {
        lblStatus.isHidden = ["disabled", nil].contains(transaction.spvVerified)
        switch transaction.spvVerified {
        case "verified":
            lblStatus.textColor = UIColor.customMatrixGreen()
            lblStatus.text = NSLocalizedString("id_verified", comment: "")
        case "not_verified":
            lblStatus.textColor = .red
            lblStatus.text = NSLocalizedString("id_invalid_merkle_proof", comment: "")
        case "not_longest":
            lblStatus.textColor = .yellow
            lblStatus.text = NSLocalizedString("id_not_on_longest_chain", comment: "")
        default:
            lblStatus.textColor = UIColor.customTitaniumLight()
            lblStatus.text = NSLocalizedString("id_verifying_transactions", comment: "")
        }
    }

    func start() {
        arc.addSubview(loadingIndicator)

        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor
                .constraint(equalTo: arc.centerXAnchor),
            loadingIndicator.centerYAnchor
                .constraint(equalTo: arc.centerYAnchor),
            loadingIndicator.widthAnchor
                .constraint(equalToConstant: arc.frame.width),
            loadingIndicator.heightAnchor
                .constraint(equalTo: arc.widthAnchor)
        ])

        loadingIndicator.isAnimating = true
    }

    func stop() {
        loadingIndicator.isAnimating = false
    }
}
