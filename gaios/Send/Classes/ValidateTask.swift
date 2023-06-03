import Foundation
import PromiseKit
import gdk

class ValidateTask {
    var tx: Transaction?
    private var cancelme = false
    private var task: DispatchWorkItem?

    init(tx: Transaction) {
        task = DispatchWorkItem {
            var tx = tx
            if let subaccount = tx.subaccountItem,
               let session = subaccount.session {
                if tx.isSweep && tx.addressees.isEmpty {
                    let address = try? session.getReceiveAddress(subaccount: subaccount.pointer).wait()
                    tx.addressees = [Addressee.from(address: address?.address ?? "", satoshi: nil, assetId: nil)]
                } else if !subaccount.gdkNetwork.lightning && tx.utxos == nil {
                    let unspent = try? session.getUnspentOutputs(subaccount: subaccount.pointer, numConfs: 0).wait()
                    tx.utxos = unspent ?? [:]
                }
                if let created = try? session.createTransaction(tx: tx).wait() {
                    tx = created
                    tx.subaccount = subaccount.hashValue
                }
            }
            self.tx = tx
        }
    }

    func execute() -> Promise<Transaction?> {
        let bgq = DispatchQueue.global(qos: .userInteractive)
        return Promise<Transaction?> { seal in
            self.task!.notify(queue: bgq) {
                guard !self.cancelme else { return seal.reject(PMKError.cancelled) }
                seal.fulfill(self.tx)
            }
            bgq.async(execute: self.task!)
        }
    }

    func cancel() {
        cancelme = true
        task?.cancel()
    }
}
