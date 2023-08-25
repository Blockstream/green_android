import Foundation
import UIKit

import gdk

class DialogSignViewModel {

    var wallet: WalletItem
    var address: String
    var isHW: Bool { WalletManager.current?.account.isHW ?? false }

    init(wallet: WalletItem, address: String) {
        self.wallet = wallet
        self.address = address
    }

    func sign(message: String) async throws -> String? {
        let params = SignMessageParams(address: address, message: message)
        let res = try await wallet.session?.signMessage(params)
        return res?.signature
    }
}
