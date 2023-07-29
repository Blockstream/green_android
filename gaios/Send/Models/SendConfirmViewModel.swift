import Foundation

import gdk

class SendConfirmViewModel {
    
    var wm: WalletManager? { WalletManager.current }
    var account: WalletItem
    var tx: Transaction
    var addresseeCellModels: [AddresseeCellModel]
    var session: SessionManager { account.session! }
    var remoteAlert: RemoteAlert?
    var isLightning: Bool { account.gdkNetwork.lightning }
    var isHW: Bool { wm?.account.isHW ?? false }
    var isLedger: Bool { wm?.account.isLedger ?? false }

    init(account: WalletItem, tx: Transaction) {
        self.account = account
        self.tx = tx
        self.addresseeCellModels = [AddresseeCellModel(tx: tx, index: 0)]
        self.remoteAlert = RemoteAlertManager.shared.alerts(screen: .sendConfirm, networks: wm?.activeNetworks ?? []).first
    }
    
    func send() async throws {
        if wm?.hwDevice != nil {
            let bleDevice = BleViewModel.shared
            if !bleDevice.isConnected() {
                try await bleDevice.connect()
                _ = try await bleDevice.authenticating()
            }
        }
        let liquid = tx.subaccountItem?.gdkNetwork.liquid
        if liquid ?? false {
            tx = try await session.blindTransaction(tx: tx)
        }
        try await tx = session.signTransaction(tx: tx)
        if tx.isSweep {
            return try await session.broadcastTransaction(txHex: tx.transaction ?? "")
        } else {
            return try await session.sendTransaction(tx: tx)
        }
    }

    func nodeId() -> String? {
        return WalletManager.current?.lightningSession?.nodeState?.id
    }
}
