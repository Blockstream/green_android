import Foundation
import UIKit
import gdk

class LTRecoverFundsViewModel {
    var wallet: WalletItem?
    var address: String?
    var amount: UInt64?
    init(wallet: WalletItem? = nil, address: String? = nil, amount: UInt64? = nil) {
        self.wallet = wallet
        self.address = address
        self.amount = amount
    }
}

class LTRecoverFundsViewController: UIViewController {
    var viewModel: LTRecoverFundsViewModel!
}
