import Foundation
import UIKit
import lightning
import gdk

class LTInboundCellModel {

    var amount: UInt64
    var denom: String

    var title: String {
        "Your receive capacity is \(amount) \(denom) at the moment."
    }

    init(subaccount: WalletItem) {
        self.amount = subaccount.lightningSession?.nodeState?.inboundLiquiditySatoshi ?? 0
        self.denom = "sats"
    }
}
