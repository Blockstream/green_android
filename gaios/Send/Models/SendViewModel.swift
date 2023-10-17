import Foundation
import BreezSDK
import UIKit
import gdk
import greenaddress

class SendViewModel {
    
    var wm: WalletManager? { WalletManager.current }
    var recipientCellModels = [RecipientCellModel]()
    var transaction: Transaction?
    var inputType: TxType
    var remoteAlert: RemoteAlert?
    var validateTask: Task<Transaction?, Error>?
    var inputDenomination: gdk.DenominationType

    var account: WalletItem {
        didSet {
            transaction = nil
            fee = nil
            feeRate = nil
            inputError = nil
        }
    }
    var assetId: String?
    var input: String? = nil
    var inputError: String? = nil
    var sendAll: Bool = false
    var satoshi: Int64? = nil
    var fee: UInt64? = nil
    var feeRate: UInt64? = nil
    var isFiat: Bool = false
    var editableAddress: Bool = true
    var editableAmount: Bool = false
    var addressInputType: AnalyticsManager.AddressInputType?
    
    var transactionPriority: TransactionPriority = .Medium {
        didSet {
            if transactionPriority != .Custom {
                fee = nil
                feeRate = feeEstimates[transactionPriority.rawValue]
            }
        }
    }
    
    var amount: String? {
        set {
            if let newValue = newValue {
                if isFiat {
                    satoshi = Balance.fromFiat(newValue)?.satoshi
                } else {
                    satoshi = Balance.from(newValue, assetId: assetId ?? feeAsset, denomination: inputDenomination)?.satoshi
                }
            } else {
                satoshi = nil
            }
        }
        get {
            if let satoshi = satoshi {
                if isFiat {
                    return Balance.fromSatoshi(satoshi, assetId: assetId ?? feeAsset)?.toFiat().0
                } else {
                    return Balance.fromSatoshi(satoshi, assetId: assetId ?? feeAsset)?.toValue(inputDenomination).0
                }
            } else {
                return nil
            }
        }
    }
    var defaultMinFee: UInt64 { session.gdkNetwork.liquid ? 100 : 1000 }
    var feeLiquidEstimates = [UInt64](repeating: 100, count: 25)
    var feeBtcEstimates = [UInt64](repeating: 1000, count: 25)
    var feeEstimates: [UInt64] {
        session.gdkNetwork.liquid ? feeLiquidEstimates : feeBtcEstimates
    }
    
    func loadFees() async {
        let session = self.wm?.activeSessions.values.filter { !$0.gdkNetwork.liquid && !$0.gdkNetwork.lightning }.first
        if let fees = try? await session?.getFeeEstimates() {
            self.feeBtcEstimates = fees
        }
    }
    
    private var session: SessionManager { account.session! }
    private var isLiquid: Bool { session.gdkNetwork.liquid }
    private var isBtc: Bool { !session.gdkNetwork.liquid }
    private var isLightning: Bool { !session.gdkNetwork.lightning }
    private var btc: String { session.gdkNetwork.getFeeAsset() }
    private var feeAsset: String { session.gdkNetwork.getFeeAsset() }
    
    init(account: WalletItem, inputType: TxType, transaction: Transaction?, input: String?, addressInputType: AnalyticsManager.AddressInputType?) {
        self.account = account
        self.transaction = transaction
        self.assetId = account.gdkNetwork.getFeeAsset()
        self.inputType = inputType
        self.transactionPriority = inputType == .bumpFee ? .Custom : .Medium
        self.addressInputType = addressInputType
        let settings =  WalletManager.current?.prominentSession?.settings
        let denom = settings?.denomination ?? .BTC
        self.inputDenomination = denom
        self.remoteAlert = RemoteAlertManager.shared.alerts(screen: .send, networks: wm?.activeNetworks ?? []).first
        self.input = input
        if inputType == .bumpFee {
            feeRate = transaction?.feeRate
            fee = transaction?.fee
        }
    }
    
    func validateTransaction() async throws -> Task<Transaction?, Error>? {
        let tx = Transaction(self.transaction?.details ?? [:], subaccount: account.hashValue)
        validateTask?.cancel()
        validateTask = Task {
            let tx = try? await validate(tx: tx)
            if let tx = tx {
                self.transaction = tx
                reload()
            }
            return tx
        }
        return validateTask
    }

    func wait() async throws {
        var attempts = 0
        func attempt() async throws {
            if attempts == 10 {
                throw GaError.TimeoutError()
            }
            attempts += 1
            if let session = account.session {
                if !(session.logged && !session.paused) {
                    try await Task.sleep(nanoseconds:  500_000_000)
                    try await attempt()
                }
            }
        }
        return try await attempt()
    }

    func validate(tx: Transaction) async throws -> Transaction? {
        var tx = tx
        if Task.isCancelled { return nil }
        if let subaccount = tx.subaccountItem,
           let session = subaccount.session {
            switch inputType {
            case .transaction:
                let asset = assetId == "btc" ? nil : assetId
                if let input = input {
                    tx.addressees = [Addressee.from(address: input, satoshi: satoshi ?? 0, assetId: asset, isGreedy: sendAll)]
                }
                tx.feeRate = feeRate ?? feeEstimates[transactionPriority.rawValue]
            case .sweep:
                tx.sessionSubaccount = account.pointer
                tx.privateKey = input
                tx.feeRate = feeRate ?? feeEstimates[transactionPriority.rawValue]
            case .bumpFee:
                tx.feeRate = feeRate ?? feeEstimates[transactionPriority.rawValue]
            case .bolt11:
                break
            case .lnurl:
                if var addressee = tx.addressees.first {
                    addressee.satoshi = satoshi ?? 0
                    tx.addressees = [addressee]
                }
            }
            if Task.isCancelled { return nil }
            if tx.isSweep {
                if tx.addressees.isEmpty {
                    var address = try await session.getReceiveAddress(subaccount: subaccount.pointer)
                    address.isGreedy = true
                    address.satoshi = 0
                    var addressee = address.toDict()
                    var btc = tx.subaccountItem?.gdkNetwork.getFeeAsset()
                    addressee?["id_asset"] = btc
                    tx.details["addressees"] = [addressee]
                }
                if tx.utxos?.isEmpty ?? true {
                    let unspent = try? await session.getUnspentOutputsForPrivateKey(UnspentOutputsForPrivateKeyParams(privateKey: tx.privateKey ?? "", password: nil))
                    tx.utxos = unspent ?? [:]
                }
            } else if !subaccount.gdkNetwork.lightning && tx.utxos == nil {
                let unspent = try? await session.getUnspentOutputs(subaccount: subaccount.pointer, numConfs: 0)
                tx.utxos = unspent ?? [:]
            }
            if Task.isCancelled { return nil }
            if let created = try? await session.createTransaction(tx: tx) {
                tx = created
                tx.subaccount = subaccount.hashValue
            }
        }
        if Task.isCancelled { return nil }
        return tx
    }
    
    var amountError: String? {
        return ["id_invalid_amount", "id_no_amount_specified", "id_insufficient_funds", "id_invalid_payment_request_assetid", "id_invalid_asset_id"].contains(inputError) ? inputError : nil
    }
    
    var addressError: String? {
        return ["id_invalid_address", "id_invalid_private_key"].contains(inputError) ? inputError : nil
    }
    
    var inlineErrors = [
        "id_invalid_address",
        "id_invalid_private_key",
        "id_invalid_amount",
        "id_no_amount_specified",
        "id_insufficient_funds",
        "id_invalid_payment_request_assetid",
        "id_invalid_asset_id"
    ]
    
    func reload() {
        guard let tx = self.transaction else { return }
        inputError = tx.error
        sendAll = tx.addressees.first?.isGreedy ?? false
        fee = tx.fee == 0 ? nil : tx.fee
        feeRate = tx.feeRate == 0 ? nil : tx.feeRate
        if let addressee = tx.addressees.first {
            if input == nil {
                input = addressee.address
            }
            if let addrAssetId = addressee.assetId {
                assetId = addrAssetId
            }
            if let addrSatoshi = addressee.satoshi, satoshi == nil, addrSatoshi != 0 {
                satoshi = abs(addrSatoshi)
            }
        }
        if sendAll {
            let assetId = assetId ?? feeAsset
            let value = tx.amounts.filter({$0.key == assetId}).first?.value ?? 0
            satoshi = abs(value)
        }
        editableAddress = true
        editableAmount = true
        switch inputType {
        case .transaction, .lnurl:
            editableAmount = tx.addressees.count > 0 && !sendAll
        case .sweep, .bumpFee, .bolt11:
            editableAddress = false
            editableAmount = false
        }
    }
    
    var alertCellModel: AlertCardCellModel? {
        if let remoteAlert = remoteAlert {
            return AlertCardCellModel(type: .remoteAlert(remoteAlert))
        }
        return nil
    }
    
    var feeCellModel: FeeEditCellModel {
        return FeeEditCellModel(fee: fee,
                                feeRate: feeRate,
                                txError: nil,
                                feeEstimates: feeEstimates,
                                inputDenomination: inputDenomination,
                                transactionPriority: transactionPriority)
    }
    
    var accountAssetCellModel: ReceiveAssetCellModel {
        return ReceiveAssetCellModel(assetId: assetId ?? AssetInfo.btcId, account: account)
    }

    var amountCellModel: AmountEditCellModel {
        let balance = account.satoshi?[assetId ?? feeAsset]
        return AmountEditCellModel(text: amount, error: amountError, balance: balance, assetId: assetId ?? AssetInfo.btcId, editable: editableAmount, sendAll: sendAll, isFiat: isFiat, isLightning: account.type.lightning, inputDenomination: inputDenomination)
    }

    var addressEditCellModel: AddressEditCellModel {
        return AddressEditCellModel(text: input, error: addressError, editable: editableAddress)
    }

    func validateInput() async throws {
        guard let input = input, !input.isEmpty else {
            editableAmount = false
            return
        }
        if inputType == .bumpFee {
            return // nothing to do
        } else if inputType == .sweep {
            transaction?.privateKey = input
            return
        }
        let parser = Parser(input: input)
        do {
            try await parser.runSingleAccount(account: account)
            transaction = parser.createTx?.tx
            if let sat = parser.createTx?.satoshi {
                satoshi = sat
            }
            switch parser.lightningType {
            case .bolt11(_): inputType = .bolt11
            case .lnUrlPay(_): inputType = .lnurl
            default: inputType = .transaction
            }
            inputError = parser.createTx?.error
            reload()
        } catch {
            switch error {
            case ParserError.InvalidInput(let txt),
                ParserError.InvalidNetwork(let txt),
                ParserError.InvalidTransaction(let txt):
                inputError = txt
                transaction = CreateTransaction(error: txt).tx
            default:
                transaction = CreateTransaction(error: error.localizedDescription).tx
            }
            reload()
        }
    }

    func dialogInputDenominationViewModel(inputDenomination: DenominationType?) -> DialogInputDenominationViewModel? {
        let settings = wm?.prominentSession?.settings
        let list: [DenominationType] = [ .BTC, .MilliBTC, .MicroBTC, .Bits, .Sats]
        var selected = settings?.denomination ?? .BTC
        if let inputDenomination = inputDenomination { selected = inputDenomination }
        let network: NetworkSecurityCase = session.gdkNetwork.mainnet ? .bitcoinSS : .testnetSS
        return DialogInputDenominationViewModel(denomination: selected,
                                           denominations: list,
                                           network: network,
                                            isFiat: isFiat)
    }

    func getBalance() -> Balance? {
        if let satoshi = satoshi {
            return Balance.fromSatoshi(satoshi, assetId: assetId ?? feeAsset)
        }
        return nil
    }
}
