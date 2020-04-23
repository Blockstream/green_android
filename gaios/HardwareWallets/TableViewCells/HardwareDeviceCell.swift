import UIKit
import CoreBluetooth
import RxBluetoothKit

protocol MenuActionsDelegate: AnyObject {
    func ota(_ peripheral: Peripheral)
}

class HardwareDeviceCell: UITableViewCell {

    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var connectionStatusLabel: UILabel!
    var peripheral: Peripheral?
    weak var delegate: MenuActionsDelegate?

    override func prepareForReuse() {
        nameLabel.text = ""
        connectionStatusLabel.text = ""
    }

    @objc public func otaItemTapped (_ sender: UIMenuController) {
        print("ota")
        if let p = peripheral {
            delegate?.ota(p)
        }
    }

     override func canPerformAction(_ action: Selector, withSender sender: Any?) -> Bool {
        return action == #selector(otaItemTapped)
    }
}
