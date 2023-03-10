import Foundation
import PromiseKit

enum InputType {
    case transaction
    case sweep
    case bumpFee
}

class ValidateTask {
    var tx: Transaction?
    private var cancelme = false
    private var task: DispatchWorkItem?

    init(details: [String: Any], inputType: InputType, session: SessionManager, account: WalletItem) {
        task = DispatchWorkItem {
            var details = details
            let subaccount = details["subaccount"] as? UInt32
            if inputType == .transaction && details["utxos"] == nil {
                let unspent = try? session.getUnspentOutputs(subaccount: subaccount ?? 0, numConfs: 0).wait()
                details["utxos"] = unspent ?? [:]
            } else if inputType == .sweep && details["addressees"] == nil {
                let address = try? account.session?.getReceiveAddress(subaccount: account.pointer).wait()
                let addressee: [String: Any] = ["address": address ?? ""]
                details["addressees"] = [addressee]
            }
            let inputTx = Transaction(details, subaccount: account.hashValue)
            var tx = try? session.createTransaction(tx: inputTx).wait()
            tx?.subaccount = account.hashValue
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
