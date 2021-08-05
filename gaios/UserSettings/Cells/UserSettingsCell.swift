import UIKit

class UserSettingsCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var actionSwitch: UISwitch!
    @IBOutlet weak var disclosure: UIImageView!
    var onActionSwitch: (() -> Void)?

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        lblTitle.textColor = .white
        lblHint.textColor = UIColor.customGrayLight()
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    func configure(_ item: UserSettingsItem, onActionSwitch: (() -> Void)? = nil) {
        self.lblTitle.text = item.title
        self.lblHint.text = item.subtitle
        self.onActionSwitch = onActionSwitch

        switch item.type {
        case .Logout:
            lblHint.textColor = UIColor.errorRed()
            disclosure.isHidden = true
        case .LoginWithBiometrics:
            lblHint.textColor = UIColor.customGrayLight()
            disclosure.isHidden = true
        case .TermsOfUse, .PrivacyPolicy:
            lblHint.textColor = UIColor.customGrayLight()
            disclosure.isHidden = false
            disclosure.image = UIImage(named: "link")?.maskWithColor(color: .white)
        default:
            lblHint.textColor = UIColor.customGrayLight()
            disclosure.isHidden = false
            disclosure.image = UIImage(named: "rightArrow")?.maskWithColor(color: .white)
        }
    }

    @IBAction func actionSwitchChanged(_ sender: UISwitch) {
        onActionSwitch?()
    }
}
