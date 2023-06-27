import Foundation
import PromiseKit
import gdk

class SendConfirmViewModel {

    var wm: WalletManager? { WalletManager.current }
    var account: WalletItem
    var tx: Transaction
    var addresseeCellModels: [AddresseeCellModel]
    var session: SessionManager { account.session! }
    var remoteAlert: RemoteAlert?
    var lightningSession: LightningSessionManager? { WalletManager.current?.lightningSession }

    init(account: WalletItem, tx: Transaction) {
        self.account = account
        self.tx = tx
        self.addresseeCellModels = [AddresseeCellModel(tx: tx, index: 0)]
        self.remoteAlert = RemoteAlertManager.shared.alerts(screen: .sendConfirm, networks: wm?.activeNetworks ?? []).first
    }

    func send() -> Promise<Void> {
        let bgq = session.bgq
        return Guarantee()
            .compactMap(on: bgq) { self.tx.subaccountItem?.gdkNetwork.liquid ?? false }
            .then(on: bgq) { $0 ? self.session.blindTransaction(tx: self.tx) : Promise.value(self.tx) }
            .then(on: bgq) { self.session.signTransaction(tx: $0) }
            .then(on: bgq) { tx -> Promise<Void> in
                if self.tx.isSweep {
                    return self.session.broadcastTransaction(txHex: tx.transaction ?? "")
                } else {
                    return self.session.sendTransaction(tx: tx)
                }
            }
    }

    func nodeId() -> String? {
        return lightningSession?.nodeState?.id
    }

    func isLightning() -> Bool {
        return lightningSession != nil
    }
}
