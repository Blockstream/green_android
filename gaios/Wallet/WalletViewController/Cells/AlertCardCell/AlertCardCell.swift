import UIKit

enum AlertCardType {
    case reset(Int)
    case dispute
    case reactivate
    case systemMessage(SystemMessage)
    case fiatMissing
    case testnetNoValue
    case ephemeralWallet
    case remoteAlert(RemoteAlert)
}

class AlertCardCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnRight: UIButton!
    @IBOutlet weak var btnLeft: UIButton!
    @IBOutlet weak var btnsContainer: UIView!
    @IBOutlet weak var iconWarn: UIImageView!
    @IBOutlet weak var btnDismiss: UIButton!

    var onLeft:(() -> Void)?
    var onRight:(() -> Void)?
    var onDismiss:(() -> Void)?

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
        btnsContainer.isHidden = false
        btnRight.isHidden = false
        btnLeft.isHidden = false
    }

    func configure(_ model: AlertCardCellModel,
                   onLeft:(() -> Void)?,
                   onRight:(() -> Void)?,
                   onDismiss:(() -> Void)?
    ) {
        self.backgroundColor = UIColor.customTitaniumDark()
        bg.layer.cornerRadius = 6.0

        self.onLeft = onLeft
        self.onRight = onRight
        self.onDismiss = onDismiss

        btnsContainer.isHidden = false
        iconWarn.isHidden = true
        btnDismiss.isHidden = true

        switch model.type {
        case .reset(let resetDaysRemaining):
            lblTitle.text = NSLocalizedString("id_2fa_reset_in_progress", comment: "")
            lblHint.text = String(format: NSLocalizedString("id_your_wallet_is_locked_for_a", comment: ""), resetDaysRemaining)
            btnRight.setTitle(NSLocalizedString("id_learn_more", comment: ""), for: .normal)
            btnLeft.isHidden = true
        case .dispute:
            lblTitle.text = NSLocalizedString("id_2fa_dispute_in_progress", comment: "")
            lblHint.text = NSLocalizedString("id_warning_wallet_locked_by", comment: "")
            btnRight.setTitle(NSLocalizedString("id_learn_more", comment: ""), for: .normal)
            btnLeft.isHidden = true
        case .reactivate:
            lblTitle.text = NSLocalizedString("id_2fa_expired", comment: "")
            lblHint.text = "2FA protection on some of your funds has expired"
            btnRight.setTitle(NSLocalizedString("id_learn_more", comment: ""), for: .normal)
            btnLeft.setTitle("Reactivate 2FA", for: .normal)
        case .systemMessage(var system):
            lblTitle.text = NSLocalizedString("id_system_message", comment: "")
            lblHint.text = system.text.count > 200 ? system.text.prefix(200) + " ..." : system.text
            btnRight.setTitle(NSLocalizedString("id_accept", comment: ""), for: .normal)
            btnLeft.isHidden = true
        case .fiatMissing:
            lblTitle.text = NSLocalizedString("id_warning", comment: "")
            lblHint.text = NSLocalizedString("id_your_favourite_exchange_rate_is", comment: "")
            btnRight.isHidden = true
            btnLeft.isHidden = true
            btnsContainer.isHidden = true
        case .testnetNoValue:
            lblTitle.text = NSLocalizedString("id_warning", comment: "")
            lblHint.text = NSLocalizedString("id_this_wallet_operates_on_a_test", comment: "")
            btnRight.isHidden = true
            btnLeft.isHidden = true
            btnsContainer.isHidden = true
        case .ephemeralWallet:
            lblTitle.text = NSLocalizedString("id_passphrase_protected", comment: "")
            lblHint.text = NSLocalizedString("id_this_wallet_is_based_on_your", comment: "")
            btnRight.isHidden = true
            btnLeft.isHidden = true
            btnsContainer.isHidden = true
        case .remoteAlert(let remoteAlert):
            lblTitle.text = remoteAlert.title
            lblHint.text = remoteAlert.message
            lblTitle.isHidden = remoteAlert.title?.isEmpty ?? true
            lblHint.isHidden = remoteAlert.message?.isEmpty ?? true
            btnRight.setTitle(NSLocalizedString("id_learn_more", comment: ""), for: .normal)
            btnRight.isHidden = onRight == nil
            btnLeft.isHidden = true
            btnsContainer.isHidden = onRight == nil
            if remoteAlert.isWarning ?? false {
                iconWarn.isHidden = false
            }
            if remoteAlert.dismissable ?? false {
                btnDismiss.isHidden = false
            }
        }
    }

    @IBAction func btnRight(_ sender: Any) {
        onRight?()
    }

    @IBAction func btnLeft(_ sender: Any) {
        onLeft?()
    }

    @IBAction func onDismiss(_ sender: Any) {
        onDismiss?()
    }
}
