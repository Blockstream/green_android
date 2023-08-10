import Foundation
import UIKit
import lightning
import gdk

class LTInboundCellModel {

    var amount: UInt64
    var denom: String

    var title: String {
        "Your receive capacity is \(amount) \(denom) at the moment."
    } // id_your_current_receive_capacity

    init(amount: UInt64, denom: String = "sats") {
        self.amount = amount
        self.denom = denom
    }
}
