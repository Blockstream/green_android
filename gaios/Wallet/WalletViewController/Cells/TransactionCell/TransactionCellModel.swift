import Foundation
import UIKit

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
    var amounts = [(key: String, value: Int64)]()

    private let wm = WalletManager.current

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
            self.status = isPending() ? "Redepositing" : "Redeposited"
            icon = UIImage(named: "ic_tx_received")!
        case .incoming:
            self.status = isPending() ? "id_receiving".localized : "id_received".localized
            icon = UIImage(named: "ic_tx_received")!
        case .outgoing:
            self.status = isPending() ? "is_sending".localized : "id_sent".localized
            icon = UIImage(named: "ic_tx_sent")!
        case .mixed:
            self.status = isPending() ? "Swapping" : "Swap"
        }
    }

    func isPending() -> Bool {
        if tx.blockHeight == 0 {
            return true
        } else if tx.isLiquid && self.blockHeight < tx.blockHeight + 1 {
            return true
        } else if !tx.isLiquid && self.blockHeight < tx.blockHeight + 5 {
            return true
        } else {
            return false
        }
    }

    func statusUI() -> PendingStateUI {
        if tx.blockHeight == 0 {
            return PendingStateUI(style: .unconfirmed,
                                  label: "id_unconfirmed".localized,
                                  progress: nil)
        } else if tx.isLiquid && self.blockHeight < tx.blockHeight + 1 {
            return PendingStateUI(style: .pending,
                                  label: "id_12_confirmations".localized,
                                  progress: 0.5)
        } else if !tx.isLiquid && self.blockHeight < tx.blockHeight + 5 {
            guard blockHeight >= tx.blockHeight else {
                return PendingStateUI(style: .simple,
                                      label: "",
                                      progress: nil)
            }
            let confirmCount = (blockHeight - tx.blockHeight) + 1
            return PendingStateUI(style: .pending,
                                  label: String(format: "id_d6_confirmations".localized, confirmCount),
                                  progress: Double(confirmCount) / 6.0)
        } else {
            return PendingStateUI(style: .simple,
                                  label: date,
                                  progress: nil)
        }
    }

    func amounts(_ tx: Transaction, _ subaccount: WalletItem) -> [(key: String, value: Int64)] {
        var amounts = [(key: String, value: Int64)]()
        let feeAsset = subaccount.gdkNetwork.getFeeAsset()
        if tx.type == .redeposit {
            amounts = [(key: feeAsset, value: -1 * Int64(tx.fee))]
        } else {
            amounts = tx.assetamounts
            // remove L-BTC asset only if fee on outgoing transactions
            if tx.type == .some(.outgoing) || tx.type == .some(.mixed) {
                amounts = amounts.filter({ !($0.key == feeAsset && abs($0.value) == Int64(tx.fee)) })
            }
        }
        return amounts
    }

//    func checkBlockHeight(transaction: Transaction, blockHeight: UInt32) {
//        if transaction.blockHeight == 0 {
//            setStatus(.unconfirmed, label: NSLocalizedString("id_unconfirmed", comment: ""))
//        } else if transaction.isLiquid && blockHeight < transaction.blockHeight + 1 {
//            setStatus(.holding, label: NSLocalizedString("id_12_confirmations", comment: ""))
//        } else if !transaction.isLiquid && blockHeight < transaction.blockHeight + 5 {
//            guard blockHeight >= transaction.blockHeight else {
//                setStatus(.confirmed, label: "")
//                return
//            }
//            let confirmCount = (blockHeight - transaction.blockHeight) + 1
//            setStatus(.holding, label: String(format: NSLocalizedString("id_d6_confirmations", comment: ""), confirmCount))
//        } else {
//            setStatus(.confirmed, label: "")
//        }
//    }

//    private func setStatus(_ status: TransactionStatus, label: String) {
//        lblDate.isHidden = true
//        statusBadge.isHidden = false
//        lblStatus.textColor = .white
//        lblStatus.text = label
//
//        switch status {
//        case .unconfirmed:
//            statusBadge.backgroundColor = UIColor.warningYellow()
//        case .holding:
//            statusBadge.backgroundColor = .gray
//        case .confirmed:
//            statusBadge.isHidden = true
//            lblDate.isHidden = false
//        }
//    }
}
