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
        guard let session = SessionsManager.current else { return }
        task = DispatchWorkItem {
            var details = details
            if inputType == .transaction && details["utxos"] == nil {
                let unspentCall = try? session.getUnspentOutputs(details: ["subaccount": details["subaccount"] ?? 0, "num_confs": 0])
                let unspentData = try? unspentCall?.resolve().wait()
                let unspentResult = unspentData?["result"] as? [String: Any]
                let unspent = unspentResult?["unspent_outputs"] as? [String: Any]
                details["utxos"] = unspent ?? [:]
            }

            let createCall = try? session.createTransaction(details: details)
            let createData = try? createCall?.resolve().wait()
            let createResult = createData?["result"] as? [String: Any]
            self.tx = Transaction(createResult ?? [:])
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
