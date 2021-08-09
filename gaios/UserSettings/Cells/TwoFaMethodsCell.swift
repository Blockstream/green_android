import UIKit

class TwoFaMethodsCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var actionSwitch: UISwitch!
    @IBOutlet weak var maskedData: UILabel!

    var onActionSwitch: (() -> Void)?

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        lblTitle.textColor = .white
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    func configure(_ item: TwoFactorItem, onActionSwitch: (() -> Void)? = nil) {
        self.lblTitle.text = item.name
        self.actionSwitch.isOn = item.enabled
        self.actionSwitch.isUserInteractionEnabled = false
        self.onActionSwitch = onActionSwitch
        if let data = item.maskedData {
            self.maskedData.text = data
        } else {
            self.maskedData.text = ""
        }
    }

    @IBAction func actionSwitchChanged(_ sender: Any) {
        onActionSwitch?()
    }
}
