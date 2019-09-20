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
    var multipleAssets: Bool!
    var isIncoming: Bool!
    var isRedeposit: Bool!

    override func layoutSubviews() {
        super.layoutSubviews()
    }

    override func prepareForReuse() {
        status.text = ""
        address.text = ""
        amount.text = ""
        date.text = ""
        bumpFee.image = nil
        imageDirection.image = nil
    }

    func setup(with transaction: Transaction) {
        let assetTag = transaction.defaultAsset
        bumpFee.isHidden = !transaction.canRBF || isLiquid
        multipleAssets = transaction.amounts.count > 1
        isIncoming = transaction.type == "incoming"
        isRedeposit = transaction.type == "redeposit"
        if isRedeposit, let balance = Balance.convert(details: ["satoshi": transaction.fee]) {
            let (fee, denom) = balance.get(tag: "btc")
            amount.text = "-\(fee) \(denom)"
        } else if multipleAssets && isIncoming {
            amount.text = NSLocalizedString("id_multiple_assets", comment: "")
        } else {
            amount.text = transaction.amount()
        }
        selectionStyle = .none
        date.text = transaction.date(dateStyle: .medium, timeStyle: .none)

        let isAsset = !(assetTag == "btc")
        if !transaction.memo.isEmpty {
            address.text = transaction.memo
        } else if isAsset && transaction.assets[assetTag]?.entity?.domain != nil {
            address.text = multipleAssets && isIncoming ?
                NSLocalizedString("id_multiple_assets", comment: "") :
                transaction.assets[assetTag]?.entity?.domain ?? ""
        } else if isRedeposit {
            address.text = String(format: "%@ %@", NSLocalizedString("id_redeposited", comment: ""),
                                  isAsset ? NSLocalizedString("id_asset", comment: "") : "")
        } else if isIncoming {
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
        if isIncoming {
            amount.textColor = isLiquid ? UIColor.blueLight() : UIColor.customMatrixGreen()
            imageDirection.image = UIImage(named: isLiquid ? "tx_received" : "tx_received_mainnet")
        } else {
            amount.textColor = UIColor.white
            imageDirection.image = UIImage(named: isRedeposit ? "tx_received" : "tx_send")
        }
    }
}
