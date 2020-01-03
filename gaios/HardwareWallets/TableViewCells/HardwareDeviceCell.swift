import UIKit

class HardwareDeviceCell: UITableViewCell {

    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var connectionStatusLabel: UILabel!

    override func prepareForReuse() {
        nameLabel.text = ""
        connectionStatusLabel.text = ""
    }
}
