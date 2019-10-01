import UIKit

class NetworkSelectionSettingsView: UIView {
    @IBOutlet weak var proxySettings: UIView!
    @IBOutlet weak var proxyLabel: UILabel!
    @IBOutlet weak var proxySwitch: UISwitch!
    @IBOutlet weak var torSwitch: UISwitch!
    @IBOutlet weak var torLabel: UILabel!
    @IBOutlet weak var socks5Hostname: UITextField!
    @IBOutlet weak var socks5Port: UITextField!
    @IBOutlet weak var proxySettingsLabel: UILabel!
    @IBOutlet weak var saveButton: UIButton!

    override func layoutSubviews() {
        super.layoutSubviews()
    }
}
