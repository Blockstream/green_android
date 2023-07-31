import Foundation
import UIKit

class AddressAuthCellModel {

    var address: String
    var tx: Int
    var canSign: Bool

    init(address: String, tx: Int, canSign: Bool) {
        self.address = address
        self.tx = tx
        self.canSign = canSign
    }
}
