import UIKit

enum Card2faType {
    case reset
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
        case .reset:
            lblTitle.text = "2FA Reset in Progress"
            lblHint.text = "Your wallet is locked for a Two-factor Authentication reset. The reset will be completed in %d days"
            btnMore.setTitle("Learn More", for: .normal)
            btnReactivate.isHidden = true
        case .dispute:
            lblTitle.text = "2FA Reset in Progress"
            lblHint.text = "WARNING: Wallet locked by Two-Factor dispute. Contact support for more information."
            btnMore.setTitle("Learn More", for: .normal)
            btnReactivate.isHidden = true
        case .reactivate:
            lblTitle.text = "2FA Expired"
            lblHint.text = "2FA protection on some of your funds has expired"
            btnMore.setTitle("Learn More", for: .normal)
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
