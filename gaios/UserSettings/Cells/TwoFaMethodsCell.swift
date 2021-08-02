import UIKit

class TwoFaMethodsCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var actionSwitch: UISwitch!

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

    func configure(_ item: FactorItem, onActionSwitch: (() -> Void)? = nil) {
        self.lblTitle.text = item.name
        self.actionSwitch.isOn = item.enabled
        self.actionSwitch.isUserInteractionEnabled = false
        self.onActionSwitch = onActionSwitch
    }

    @IBAction func actionSwitchChanged(_ sender: Any) {
        onActionSwitch?()
    }
}
