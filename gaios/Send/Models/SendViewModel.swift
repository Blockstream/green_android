import Foundation
import PromiseKit

class SendViewModel {

    var wm: WalletManager? { WalletManager.current }
    var recipientCellModels = [RecipientCellModel]()
    var account: WalletItem
    var transaction: Transaction?
    var transactionPriority: TransactionPriority = .High
    var inputType: InputType
    var customFee: UInt64 = 1000

    private var session: SessionManager { account.session! }
    private var isLiquid: Bool { session.gdkNetwork.liquid }
    private var isBtc: Bool { !session.gdkNetwork.liquid }
    private var btc: String { session.gdkNetwork.getFeeAsset() }
    private var validateTask: ValidateTask?

    init(account: WalletItem, inputType: InputType, transaction: Transaction?) {
        self.account = account
        self.inputType = inputType
        self.transaction = transaction
        self.transactionPriority = inputType == .bumpFee ? .High : defaultTransactionPriority()
    }

    var recipient: RecipientCellModel? {
        recipientCellModels.first
    }

    var isSendAll: Bool {
        recipient?.isSendAll ?? false
    }

    func addressee() -> [String: Any] {
        // handling only 1 recipient for the moment
        var addressee: [String: Any] = [:]
        addressee["address"] = recipient?.address ?? ""
        addressee["satoshi"] = recipient?.satoshi() ?? 0
        if let assetId = recipient?.assetId, assetId != AssetInfo.btcId {
            addressee["asset_id"] = recipient?.assetId
        }
        return addressee
    }

    func privateKey() -> String {
        // handling only 1 recipient for the moment
        return recipient?.address ?? ""
    }

    func createTx() -> Promise<Transaction?> {
        var details: [String: Any] = [:]
        if let tx = transaction { details = tx.details }
        transaction = nil
        var estimates = feeEstimates()
        estimates[3] = customFee
        let feeEstimate = estimates[selectedFee()] ?? customFee
        let feeRate = feeEstimate
        switch inputType {
        case .transaction:
            if recipient?.isBipAddress() ?? false { details = [:] }
            details["addressees"] = [addressee()]
            details["fee_rate"] = feeRate
            details["subaccount"] = account.pointer
            details["send_all"] = isSendAll
        case .sweep:
            details["private_key"] = privateKey()
            details["fee_rate"] = feeRate
            details["subaccount"] = account.pointer
        case .bumpFee:
            details["fee_rate"] = feeRate
        }
        validateTask?.cancel()
        validateTask = ValidateTask(details: details, inputType: inputType, session: session)
        return Promise.value(validateTask)
            .compactMap { $0 }
            .then { $0.execute() }
            .get { self.updateRecipientFromTx(tx: $0) }
    }

    func loadRecipient() {
        var recipient = RecipientCellModel(account: account, inputType: inputType)
        if inputType == .bumpFee {
            let addressee = transaction?.addressees.first
            recipient.address = addressee?.address
            if let satoshi = addressee?.satoshi {
                let (amount, _) = satoshi == 0 ? ("", "") : Balance.fromSatoshi(satoshi)?.toDenom() ?? ("", "")
                recipient.amount = amount
            }
            recipient.txError = transaction?.error ?? ""
        }
        recipient.assetId = account.gdkNetwork.liquid ? nil : AssetInfo.btcId
        recipientCellModels.append(recipient)
    }

    func feeEstimates() -> [UInt64?] {
        var feeEstimates = [UInt64?](repeating: 0, count: 4)
        guard let estimates = session.getFeeEstimates() else {
            // We use the default minimum fee rates when estimates are not available
            let defaultMinFee = session.gdkNetwork.liquid ? 100 : 1000
            return [UInt64(defaultMinFee), UInt64(defaultMinFee), UInt64(defaultMinFee), UInt64(defaultMinFee)]
        }
        for (index, value) in [3, 12, 24, 0].enumerated() {
            feeEstimates[index] = estimates[value]
        }
        feeEstimates[3] = nil
        return feeEstimates
    }

    func defaultTransactionPriority() -> TransactionPriority {
        guard let settings = WalletManager.current?.prominentSession?.settings else {
            return .High
        }
        if let pref = TransactionPriority.getPreference() {
            settings.transactionPriority = pref
        }
        return settings.transactionPriority
    }

    func selectedFee() -> Int {
        switch transactionPriority {
        case .High:
            return 0
        case .Medium:
            return 1
        case .Low:
            return 2
        case .Custom:
            return 3
        }
    }

    var inlineErrors = ["id_invalid_address",
                      "id_invalid_private_key",
                      "id_invalid_amount",
                      "id_insufficient_funds",
                      "id_invalid_payment_request_assetid",
                      "id_invalid_asset_id"
    ]

    func isBipAddress() -> Bool {
        return recipient?.isBipAddress() ?? false
    }

    func updateRecipientFromTx(tx: Transaction?) {
        transaction = tx
        let addreessee = tx?.addressees.first
        let asset = isLiquid ? addreessee?.assetId ?? "" : "btc"
        if recipientCellModels.first != nil {
            recipientCellModels[0].txError = tx?.error ?? ""
            //recipientCellModels[0].assetId = asset
            //recipientCellModels[0].amount = nil
            recipientCellModels[0].isSendAll = tx?.sendAll ?? isSendAll
            if tx?.sendAll ?? false {
                var value = addreessee?.satoshi ?? 0
                if !session.gdkNetwork.electrum {
                    value = tx?.amounts.filter({$0.key == asset}).first?.value ?? 0
                }
                let assetInfo = WalletManager.current?.registry.info(for: asset)
                if let balance = Balance.fromSatoshi(value, asset: assetInfo) {
                    let (amount, _) = value == 0 ? ("", "") : balance.toValue()
                    recipientCellModels[0].amount = amount
                }
            }
        }
    }

    func updateRecipient(assetId: String?) {
        if recipientCellModels.first != nil {
            recipientCellModels[0].assetId = assetId
            recipientCellModels[0].amount = nil
            recipientCellModels[0].txError = ""
        }
    }
}
