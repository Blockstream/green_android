import UIKit
import CoreBluetooth
import RxBluetoothKit

class HWWCell: UITableViewCell {

    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var connectionStatusLabel: UILabel!

    func configure(_ name: String, _ status: String) {
        nameLabel.text = name
        connectionStatusLabel.text = status
    }
}
