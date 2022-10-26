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

    init(details: [String: Any], inputType: InputType) {
        guard let session = WalletManager.current?.currentSession else { return }
        task = DispatchWorkItem {
            var details = details
            if inputType == .transaction && details["utxos"] == nil {
                let subaccount = details["subaccount"] as? UInt32
                let unspent = try? session.getUnspentOutputs(subaccount: subaccount ?? 0, numConfs: 0).wait()
                details["utxos"] = unspent ?? [:]
            }
            self.tx = try? session.createTransaction(tx: Transaction(details)).wait()
        }
    }

    func execute() -> Promise<Transaction?> {
        let bgq = DispatchQueue.global(qos: .background)
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
