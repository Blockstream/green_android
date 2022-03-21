import UIKit
import CoreBluetooth
import RxBluetoothKit

class HWWNetworkSecurityCaseCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var icon1: UIImageView!
    @IBOutlet weak var icon2: UIImageView!
    @IBOutlet weak var name: UILabel!

    func configure(_ type: NetworkSecurityCase) {
        icon1.image = type.icons().0
        icon2.image = type.icons().1
        name.text = type.name()
        bg.cornerRadius = 4.0
    }
}
