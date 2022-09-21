import Foundation
import UIKit

class ACTransactionCellModel {
    var tx: Transaction
    var blockHeight: UInt32

    init(tx: Transaction, blockHeight: UInt32) {
        self.tx = tx
        self.blockHeight = blockHeight
    }
}
