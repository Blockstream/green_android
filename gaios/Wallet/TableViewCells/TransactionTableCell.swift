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
        var account = AccountsManager.shared.current
        return account?.gdkNetwork?.liquid ?? false
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
        self.backgroundColor = UIColor.customTitaniumDark()
        let assetTag = transaction.defaultAsset
        bumpFee.isHidden = !transaction.canRBF || isLiquid
        multipleAssets = transaction.amounts.count > 1
        isIncoming = transaction.type == "incoming"
        isRedeposit = transaction.type == "redeposit"
        if isRedeposit, let balance = Balance.convert(details: ["satoshi": transaction.fee]) {
            let (fee, denom) = balance.get(tag: "btc")
            amount.text = "-\(fee ?? "") \(denom)"
        } else if multipleAssets && isIncoming {
            amount.text = NSLocalizedString("id_multiple_assets", comment: "")
        } else if "btc" == transaction.defaultAsset {
            if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
                let (value, denom) = balance.get(tag: "btc")
                amount.text = String(format: "%@%@ %@", transaction.type == "outgoing" || transaction.type == "redeposit" ? "-" : "", value ?? "", denom)
            }
        } else {
            let asset = transaction.defaultAsset
            let info = Registry.shared.infos[asset] ?? AssetInfo(assetId: asset, name: "", precision: 0, ticker: "")
            let details = ["satoshi": transaction.amounts[asset]!, "asset_info": info.encode()!] as [String: Any]
            if let balance = Balance.convert(details: details) {
                let (value, ticker) = balance.get(tag: transaction.defaultAsset)
                amount.text = String(format: "%@%@ %@", transaction.type == "outgoing" || transaction.type == "redeposit" ? "-" : "", value ?? "", ticker)
            }
        }
        selectionStyle = .none
        date.text = transaction.date(dateStyle: .medium, timeStyle: .none)

        let isAsset = !(assetTag == "btc")
        if !transaction.memo.isEmpty {
            address.text = transaction.memo
        } else if isAsset && Registry.shared.infos[assetTag]?.entity?.domain != nil {
            address.text = multipleAssets && isIncoming ?
                NSLocalizedString("id_multiple_assets", comment: "") :
                Registry.shared.infos[assetTag]?.entity?.domain ?? ""
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
            guard blockHeight >= transaction.blockHeight else {
                status.text = ""
                return
            }
            let confirmCount = (blockHeight - transaction.blockHeight) + 1
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
