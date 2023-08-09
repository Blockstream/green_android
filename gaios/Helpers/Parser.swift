import Foundation
import BreezSDK

import gdk

struct CreateTransaction {
    var addressee: Addressee?
    var feeRate: UInt64?
    var sendAll: Bool?
    var subaccount: WalletItem?
    var error: String?
    var privateKey: String?
    var previousTransaction: [String: Any]?
    var assetId: String? {
        get { addressee?.assetId }
        set { addressee?.assetId = newValue }
    }
    var address: String? {
        get { addressee?.address }
        set { if let value = newValue  { addressee?.address = value } }
    }
    var satoshi: Int64? {
        get { if let satoshi = addressee?.satoshi { return abs(satoshi) }; return nil }
        set { addressee?.satoshi = newValue }
    }
    var tx: Transaction {
        var tx = Transaction([:], subaccount: subaccount?.hashValue)
        if var addressee = addressee {
            if let satoshi = addressee.satoshi {
                addressee.satoshi = abs(satoshi)
            }
            if sendAll ?? false {
                addressee.isGreedy = sendAll
            }
            tx.addressees = [addressee]
        }
        if let privateKey = privateKey {
            tx.privateKey = privateKey
        }
        if let error = error {
            tx.error = error
        }
        if let previousTransaction = previousTransaction {
            tx.previousTransaction = previousTransaction
        }
        return tx
    }
}

class Parser {
    let selectedAccount: WalletItem
    let input: String
    var discoverable: Bool
    var wm: WalletManager { WalletManager.current! }
    var lightningType: InputType?
    var createTx: CreateTransaction?
    var lightningSession: LightningSessionManager? { wm.lightningSession }
    var lightningSubaccount: WalletItem? { wm.lightningSubaccount }
    var error: String?
    
    private static var IgnorableErrors = [
        "id_invalid_amount",
        "id_no_amount_specified",
        "id_insufficient_funds",
        "id_invalid_asset_id" ]
    
    init(selectedAccount: WalletItem, input: String, discoverable: Bool) {
        self.selectedAccount = selectedAccount
        self.input = input
        self.discoverable = discoverable
    }
    
    private func parseLightning() async throws {
        if discoverable || selectedAccount.gdkNetwork.lightning {
            if let session = lightningSession,
               let type = session.parseLightningInputType(input) {
                let result = session.parseLightningTxInput(input, inputType: type)
                if result.isValid {
                    createTx = CreateTransaction(addressee: result.addressees.first)
                } else {
                    createTx = CreateTransaction(error: result.errors.first)
                }
                lightningType = type
                error = result.errors.first
            }
        }
    }
    
    private func parseGdk() async throws {
        if selectedAccount.gdkNetwork.lightning { return }
        guard let session = selectedAccount.session else { return }
        let res = try await session.parseTxInput(self.input, satoshi: nil, assetId: nil)
        if res.isValid {
            createTx = CreateTransaction(addressee: res.addressees.first)
        } else if let error = res.errors.first {
            if Parser.IgnorableErrors.contains(error) {
                let addressee = Addressee.from(address: self.input, satoshi: nil, assetId: nil)
                self.createTx = CreateTransaction(addressee: addressee)
            } else {
                self.createTx = CreateTransaction(error: error)
            }
            self.error = error
        } else {
            self.createTx = CreateTransaction(error: "id_invalid_address")
            self.error = "id_invalid_address"
        }
    }

    func parse() async throws {
        try await parseLightning()
        if lightningType == nil {
            try await self.parseGdk()
        }
    }
    
    var txType: TxType {
        switch lightningType {
        case .bolt11(_): return .bolt11
        case .lnUrlPay(_): return .lnurl
        default: return .transaction
        }
    }
}
