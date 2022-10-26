import Foundation
import UIKit

class OverviewTransactionCell: UITableViewCell {

    @IBOutlet weak var statusBadge: UIView!
    @IBOutlet weak var lblStatus: UILabel!
    @IBOutlet weak var lblNote: UILabel!
    @IBOutlet weak var lblDate: UILabel!
    @IBOutlet weak var amountsStackView: UIStackView!

    override func layoutSubviews() {
        super.layoutSubviews()
    }

    enum TransactionStatus {
        case unconfirmed
        case holding
        case confirmed
    }

    override func prepareForReuse() {
        lblStatus.text = ""
        lblNote.text = ""
        lblDate.text = ""
        amountsStackView.subviews.forEach({ $0.removeFromSuperview() })
    }

    func setup(transaction: Transaction, network: String?) {
        prepareForReuse()
        backgroundColor = UIColor.customTitaniumDark()
        statusBadge.layer.cornerRadius = 3.0
        selectionStyle = .none
        lblDate.text = transaction.date(dateStyle: .medium, timeStyle: .none)

        if !transaction.memo.isEmpty {
            lblNote.text = transaction.memo
        }

        var amounts = [(key: String, value: Int64)]()
        let feeAsset = WalletManager.current?.currentSession?.gdkNetwork.getFeeAsset()
         if transaction.type == .redeposit,
           let feeAsset = feeAsset {
            amounts = [(key: feeAsset, value: -1 * Int64(transaction.fee))]
        } else {
            amounts = Transaction.sort(transaction.amounts)
            // remove L-BTC asset only if fee on outgoing transactions
            if transaction.type == .some(.outgoing) || transaction.type == .some(.mixed) {
                amounts = amounts.filter({ !($0.key == feeAsset && abs($0.value) == Int64(transaction.fee)) })
            }
        }
        amounts.forEach { addAssetAmountView(tx: transaction, satoshi: $0.value, assetId: $0.key) }
    }

    func addAssetAmountView(tx: Transaction, satoshi: Int64, assetId: String) {
        if let amountView = Bundle.main.loadNibNamed("AssetAmountView", owner: self, options: nil)?.first as? AssetAmountView {
                amountView.setup(tx: tx,
                                 satoshi: satoshi,
                                 assetId: assetId)
            amountsStackView.addArrangedSubview(amountView)
        }
    }

    func checkBlockHeight(transaction: Transaction, blockHeight: UInt32) {
        if transaction.blockHeight == 0 {
            setStatus(.unconfirmed, label: NSLocalizedString("id_unconfirmed", comment: ""))
        } else if transaction.isLiquid && blockHeight < transaction.blockHeight + 1 {
            setStatus(.holding, label: NSLocalizedString("id_12_confirmations", comment: ""))
        } else if !transaction.isLiquid && blockHeight < transaction.blockHeight + 5 {
            guard blockHeight >= transaction.blockHeight else {
                setStatus(.confirmed, label: "")
                return
            }
            let confirmCount = (blockHeight - transaction.blockHeight) + 1
            setStatus(.holding, label: String(format: NSLocalizedString("id_d6_confirmations", comment: ""), confirmCount))
        } else {
            setStatus(.confirmed, label: "")
        }
    }

    private func setStatus(_ status: TransactionStatus, label: String) {
        lblDate.isHidden = true
        statusBadge.isHidden = false
        lblStatus.textColor = .white
        lblStatus.text = label

        switch status {
        case .unconfirmed:
            statusBadge.backgroundColor = UIColor.warningYellow()
        case .holding:
            statusBadge.backgroundColor = .gray
        case .confirmed:
            statusBadge.isHidden = true
            lblDate.isHidden = false
        }
    }
}
