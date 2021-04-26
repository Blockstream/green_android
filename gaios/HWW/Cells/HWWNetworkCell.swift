import UIKit
import CoreBluetooth
import RxBluetoothKit

class HWWNetworkCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var name: UILabel!

    func configure(_ type: AvailableNetworks) {
        icon.image = type.icon()
        name.text = type.name()
        bg.cornerRadius = 4.0
    }
}
