import Foundation
import UIKit
import gdk

struct PendingStateUI {
    var style: MultiLabelStyle
    var label: String
    var progress: Double?
}

class TransactionCellModel {
    var tx: Transaction
    var blockHeight: UInt32
    var status: String?
    var date: String
    var icon = UIImage()
    var subaccount: WalletItem?
    var amounts = [String: Int64]()

    private let wm = WalletManager.current
    var assetAmountList: AssetAmountList {
        get async { await AssetAmountList(amounts) }
    }

    init(tx: Transaction, blockHeight: UInt32) {
        self.tx = tx
        self.blockHeight = blockHeight
        self.date = tx.date(dateStyle: .medium, timeStyle: .none)
        self.subaccount = wm?.subaccounts.filter { $0.hashValue == tx.subaccount }.first
        if let subaccount = self.subaccount {
            self.amounts = amounts(self.tx, subaccount)
        }
        switch tx.type {
        case .redeposit:
            // For redeposits we show fees paid in btc
            self.status = tx.isPending(block: blockHeight) ? "Redepositing" : "Redeposited"
            icon = UIImage(named: "ic_tx_received")!
        case .incoming:
            self.status = tx.isPending(block: blockHeight) ? "id_receiving".localized : "id_received".localized
            icon = UIImage(named: "ic_tx_received")!
        case .outgoing:
            self.status = tx.isPending(block: blockHeight) ? "Sending" : "id_sent".localized
            icon = UIImage(named: "ic_tx_sent")!
        case .mixed:
            self.status = tx.isPending(block: blockHeight) ? "Swapping" : "Swap"
            icon = UIImage(named: "ic_tx_swap")!
        }
    }

    func statusUI() -> PendingStateUI {
        if tx.isUnconfirmed(block: blockHeight) {
            return PendingStateUI(style: .unconfirmed,
                                  label: "id_unconfirmed".localized,
                                  progress: nil)
        } else if tx.isLightning {
            if tx.isPending(block: blockHeight) {
                return PendingStateUI(style: .unconfirmed,
                                      label: "id_unconfirmed".localized,
                                      progress: nil)
            }
        } else if tx.isLiquid {
            if tx.isPending(block: blockHeight) {
                return PendingStateUI(style: .pending,
                                      label: "id_12_confirmations".localized,
                                      progress: nil)
            }
        } else {
            guard blockHeight >= tx.blockHeight else {
                return PendingStateUI(style: .simple,
                                      label: "",
                                      progress: nil)
            }
            let confirmCount = tx.blockHeight == 0 ? 0 : (blockHeight - tx.blockHeight) + 1
            let progress = confirmCount >= 6 ? 1.0 : Double(confirmCount) / 6.0
            if progress < 1.0 {
                return PendingStateUI(style: .pending,
                                      label: String(format: "id_d6_confirmations".localized, confirmCount),
                                      progress: progress)
            }
        }
        return PendingStateUI(style: .simple,
                                     label: date,
                                     progress: nil)
    }

    func amounts(_ tx: Transaction, _ subaccount: WalletItem) -> [String: Int64] {
        let feeAsset = subaccount.gdkNetwork.getFeeAsset()
        if tx.type == .redeposit {
            return [feeAsset: -1 * Int64(tx.fee)]
        } else {
            // remove L-BTC asset only if fee on outgoing transactions
            if tx.type == .some(.outgoing) || tx.type == .some(.mixed) {
                return tx.amounts.filter({ !($0.key == feeAsset && abs($0.value) == Int64(tx.fee)) })
            }
        }
        return tx.amounts
    }
}
