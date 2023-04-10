import Foundation
import UIKit
import gdk

class TwoFactorSettingsCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var actionSwitch: UISwitch!
    @IBOutlet weak var maskedData: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        lblTitle.textColor = .white
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    var viewModel: TwoFactorSettingsCellModel? {
        didSet {
            self.lblTitle.text = viewModel?.item.name
            self.actionSwitch.isOn = viewModel?.item.enabled ?? false
            self.actionSwitch.isUserInteractionEnabled = false
            if let data = viewModel?.item.maskedData {
                self.maskedData.text = data
            } else {
                self.maskedData.text = ""
            }
        }
    }

    @IBAction func actionSwitchChanged(_ sender: Any) {
        viewModel?.onActionSwitch?()
    }
}
