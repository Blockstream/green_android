import UIKit

enum Card2faType {
    case reset(Int)
    case dispute
    case reactivate
}

class AlertCardCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnMore: UIButton!
    @IBOutlet weak var btnReactivate: UIButton!

    var onReactivate:(() -> Void)?
    var onMore:(() -> Void)?

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(_ type: Card2faType, onReactivate:(() -> Void)?, onMore:(() -> Void)?) {
        self.backgroundColor = UIColor.customTitaniumDark()
        bg.layer.cornerRadius = 6.0
        self.onReactivate = onReactivate
        self.onMore = onMore

        switch type {
        case .reset(let resetDaysRemaining):
            lblTitle.text = NSLocalizedString("id_2fa_reset_in_progress", comment: "")
            lblHint.text = String(format: NSLocalizedString("id_your_wallet_is_locked_for_a", comment: ""), resetDaysRemaining)
            btnMore.setTitle(NSLocalizedString("id_learn_more", comment: ""), for: .normal)
            btnReactivate.isHidden = true
        case .dispute:
            lblTitle.text = NSLocalizedString("id_2fa_reset_in_progress", comment: "")
            lblHint.text = NSLocalizedString("id_warning_wallet_locked_by", comment: "")
            btnMore.setTitle(NSLocalizedString("id_learn_more", comment: ""), for: .normal)
            btnReactivate.isHidden = true
        case .reactivate:
            lblTitle.text = "2FA Expired"
            lblHint.text = "2FA protection on some of your funds has expired"
            btnMore.setTitle(NSLocalizedString("id_learn_more", comment: ""), for: .normal)
            btnReactivate.setTitle("Reactivate 2FA", for: .normal)
        }

    }

    @IBAction func btnMore(_ sender: Any) {
        onMore?()
    }

    @IBAction func btnReactivate(_ sender: Any) {
        onReactivate?()
    }
}
