import Foundation
import PromiseKit

class SendConfirmViewModel {

    var wm: WalletManager? { WalletManager.current }
    var account: WalletItem
    var tx: Transaction
    var addresseeCellModels: [AddresseeCellModel]
    var session: SessionManager { account.session! }

    init(account: WalletItem, tx: Transaction) {
        self.account = account
        self.tx = tx
        self.addresseeCellModels = [AddresseeCellModel(tx: tx, index: 0)]
    }

    func send() -> Promise<Void> {
        return Guarantee()
            .then {
                self.session.signTransaction(tx: self.tx)
            }.then { result -> Promise<Void> in
                if self.tx.isSweep {
                    let tx = result["transaction"] as? String
                    return self.session.broadcastTransaction(txHex: tx ?? "")
                } else {
                    let tx = Transaction(result, subaccount: self.account.hashValue)
                    return self.session.sendTransaction(tx: tx)
                }
            }
    }
}
