import Foundation
import UIKit

class ACTransactionCellModel {
    var tx: Transaction
    var blockHeight: UInt32
    var subaccountName: String
    var value: String?
    var status: String?
    var date: String
    var multipleAssets: Bool = false

    init(tx: Transaction, blockHeight: UInt32) {
        self.tx = tx
        self.blockHeight = blockHeight
        self.multipleAssets = tx.amounts.count > 1
        self.date = tx.date(dateStyle: .medium, timeStyle: .none)

        let subaccounts = WalletManager.current?.subaccounts
        self.subaccountName = subaccounts?.filter { $0.hashValue == tx.subaccount }.first?.localizedName() ?? ""

        let assetId = tx.defaultAsset
        let satoshi = tx.amounts[assetId]
        let asset = WalletManager.current?.registry.info(for: assetId)
        let pending = OVTransactionCellModel.isPending(tx: tx, blockHeight: blockHeight)

        switch tx.type {
        case .redeposit:
            // For redeposits we show fees paid in btc
            if let balance = Balance.fromSatoshi(tx.fee)?.toDenom() {
                self.value = "\(balance.0) \(balance.1)"
            }
            self.status = pending ? "Redepositing" : "Redeposited"
        case .incoming:
            if multipleAssets {
                self.value = NSLocalizedString("id_multiple_assets", comment: "")
            }
            if let balance = Balance.fromSatoshi(satoshi ?? 0, asset: asset)?.toValue() {
                self.value = "\(balance.0) \(balance.1)"
            }
            self.status = pending ? "Receiving" : "Received"
        case .outgoing:
            if let balance = Balance.fromSatoshi(satoshi ?? 0, asset: asset)?.toValue() {
                self.value = "\(balance.0) \(balance.1)"
            }
            self.status = pending ? "Sending" : "Sent"
        case .mixed:
            if let balance = Balance.fromSatoshi(satoshi ?? 0, asset: asset)?.toValue() {
                self.value = "\(balance.0) \(balance.1)"
            }
            self.status = pending ? "Swaping" : "Swap"
        }
    }

    static func isPending(tx: Transaction, blockHeight: UInt32) -> Bool {
        if tx.blockHeight == 0 {
            return true
        } else if tx.isLiquid && blockHeight < tx.blockHeight + 1 {
            return true
        } else if !tx.isLiquid && blockHeight < tx.blockHeight + 5 {
            return true
        } else {
            return false
        }
    }
}
