import Foundation
import UIKit

class TransactionTableCell: UITableViewCell {

    @IBOutlet weak var status: UILabel!
    @IBOutlet weak var address: UILabel!
    @IBOutlet weak var amount: UILabel!
    @IBOutlet weak var date: UILabel!
    @IBOutlet weak var bumpFee: UIImageView!
    @IBOutlet weak var imageDirection: UIImageView!

    var isLiquid: Bool {
        return getGdkNetwork(getNetwork()).liquid
    }

    override func layoutSubviews() {
        super.layoutSubviews()
    }

    func setup(with transaction: Transaction) {
        let assetTag = transaction.defaultAsset
        bumpFee.isHidden = !transaction.canRBF || isLiquid
        if transaction.type == "redeposit", let balance = Balance.convert(details: ["satoshi": transaction.fee]) {
            let (fee, denom) = balance.get(tag: "btc")
            amount.text = "-\(fee) \(denom)"
        } else {
            amount.text = transaction.amount()
        }
        selectionStyle = .none
        date.text = transaction.date()

        let isAsset = !(assetTag == "btc")
        if !transaction.memo.isEmpty {
            address.text = transaction.memo
        } else if isAsset && transaction.assets[assetTag]?.entity?.domain != nil {
            address.text = transaction.assets[assetTag]?.entity?.domain ?? ""
        } else if transaction.type == "redeposit" {
            address.text = String(format: "%@ %@", NSLocalizedString("id_redeposited", comment: ""),
                                  isAsset ? NSLocalizedString("id_asset", comment: "") : "")
        } else if transaction.type == "incoming" {
            address.text = String(format: "%@ %@", NSLocalizedString("id_received", comment: ""),
                                  isAsset ? NSLocalizedString("id_asset", comment: "") : "")
        } else {
            address.text = String(format: "%@ %@",
                                  isLiquid ? NSLocalizedString("id_sent", comment: "") : transaction.address() ?? "",
                                  isLiquid && isAsset ? NSLocalizedString("id_asset", comment: "") : "")
        }
    }

    func checkBlockHeight(transaction: Transaction, blockHeight: UInt32) {
        if transaction.blockHeight == 0 {
            status.text = NSLocalizedString("id_unconfirmed", comment: "")
            status.textColor = UIColor.errorRed()
        } else if isLiquid && blockHeight < transaction.blockHeight + 1 {
            status.textColor = UIColor.customTitaniumLight()
            status.text = NSLocalizedString("id_12_confirmations", comment: "")
        } else if !isLiquid && blockHeight < transaction.blockHeight + 5 {
            let confirmCount = blockHeight - transaction.blockHeight + 1
            status.textColor = UIColor.customTitaniumLight()
            status.text = String(format: NSLocalizedString("id_d6_confirmations", comment: ""), confirmCount)
        } else {
            status.text = ""
        }
    }

    func checkTransactionType(transaction: Transaction) {
        if ["incoming", "redeposit"].contains(transaction.type) {
            amount.textColor = isLiquid ? UIColor.blueLight() : UIColor.customMatrixGreen()
            imageDirection.image = UIImage(named: isLiquid ? "tx_received" : "tx_received_mainnet")
        } else {
            amount.textColor = UIColor.white
            imageDirection.image = UIImage(named: "tx_send")
        }
    }
}
