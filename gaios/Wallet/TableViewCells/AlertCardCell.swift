import UIKit

enum Card2faType {
    case reset(title: String, hint: String, lblMore: String)
    case dispute(title: String, hint: String, lblMore: String)

    static func buildResetCard() -> Card2faType {
        return .reset(title: "2FA Reset in Progress", hint: "Your wallet is locked for a Two-factor Authentication reset. The reset will be completed in %d days", lblMore: "Learn More")
    }

    static func buildDisputeCard() -> Card2faType {
        return .dispute(title: "2FA Reset in Progress", hint: "WARNING: Wallet locked by Two-Factor dispute. Contact support for more information.", lblMore: "")
    }
}

class AlertCardCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblMore: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(_ type: Card2faType) {
        self.backgroundColor = UIColor.customTitaniumDark()
        bg.layer.cornerRadius = 6.0

        switch type {
        case .reset(let title, let hint, let more):
            lblTitle.text = title
            lblHint.text = hint
            lblMore.text = more
        case .dispute(let title, let hint, let more):
            lblTitle.text = title
            lblHint.text = hint
            lblMore.text = more
        }

    }
}
