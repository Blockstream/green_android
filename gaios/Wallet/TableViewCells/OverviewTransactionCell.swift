import Foundation
import UIKit

class OverviewTransactionCell: UITableViewCell {

    @IBOutlet weak var statusBadge: UIView!
    @IBOutlet weak var lblStatus: UILabel!
    @IBOutlet weak var lblNote: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblDate: UILabel!
    @IBOutlet weak var lblDenom: UILabel!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var spvVerifyIcon: UIImageView!

    var isLiquid: Bool {
        let account = AccountsManager.shared.current
        return account?.gdkNetwork?.liquid ?? false
    }
    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

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
        lblAmount.text = ""
        lblDate.text = ""
        lblDenom.text = ""
        icon.image = UIImage()
        spvVerifyIcon.image = UIImage()
    }

    func setup(transaction: Transaction, network: String?) {
        prepareForReuse()
        self.backgroundColor = UIColor.customTitaniumDark()
        statusBadge.layer.cornerRadius = 3.0
        let assetTag = transaction.defaultAsset
        let multipleAssets = transaction.amounts.count > 1
        let isRedeposit = transaction.type == .redeposit
        let isIncoming = transaction.type == .incoming
        if isRedeposit, let balance = Balance.convert(details: ["satoshi": transaction.fee]) {
            // For redeposits we show fees paid in btc
            let (fee, denom) = balance.get(tag: btc)
            lblAmount.text = "-\(fee ?? "")"
            lblDenom.text = "\(denom)"
        } else if multipleAssets && isIncoming {
            lblAmount.text = NSLocalizedString("id_multiple_assets", comment: "")
        } else if transaction.defaultAsset == btc {
            if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
                let (value, denom) = balance.get(tag: btc)
                lblAmount.text = String(format: "%@%@", transaction.type == .outgoing || transaction.type == .redeposit ? "-" : "+", value ?? "")
                lblDenom.text = "\(denom)"
            }
        } else {
            let asset = transaction.defaultAsset
            let info = SessionsManager.current?.registry?.infos[asset] ?? AssetInfo(assetId: asset, name: "", precision: 0, ticker: "")
            let details = ["satoshi": transaction.amounts[asset]!, "asset_info": info.encode()!] as [String: Any]
            if let balance = Balance.convert(details: details) {
                let (value, ticker) = balance.get(tag: transaction.defaultAsset)
                lblAmount.text = String(format: "%@%@", transaction.type == .outgoing || transaction.type == .redeposit ? "-" : "+", value ?? "")
                lblDenom.text = "\(ticker)"
            }
        }
        selectionStyle = .none
        lblDate.text = transaction.date(dateStyle: .medium, timeStyle: .none)

        let isAsset = !(assetTag == "btc")
        if !transaction.memo.isEmpty {
            lblNote.text = transaction.memo
        } else if isAsset && SessionsManager.current?.registry?.infos[assetTag]?.entity?.domain != nil {
            lblNote.text = multipleAssets && isIncoming ?
                NSLocalizedString("id_multiple_assets", comment: "") :
            SessionsManager.current?.registry?.infos[assetTag]?.entity?.domain ?? ""
        } else if isRedeposit {
            lblNote.text = String(format: "%@ %@", NSLocalizedString("id_redeposited", comment: ""),
                                  isAsset ? NSLocalizedString("id_asset", comment: "") : "")
        } else if isIncoming {
            lblNote.text = String(format: "%@ %@", NSLocalizedString("id_received", comment: ""),
                                  isAsset ? NSLocalizedString("id_asset", comment: "") : "")
        } else {
            lblNote.text = String(format: "%@ %@", NSLocalizedString("id_sent", comment: ""), isLiquid && isAsset ? NSLocalizedString("id_asset", comment: "") : "")
        }

        setIcon(transaction: transaction, network: network)
        setSpvVerifyIcon(tx: transaction)
    }

    func setIcon(transaction: Transaction, network: String?) {
        if network == "mainnet" {
            icon.image = UIImage(named: "ntw_btc")
        } else if network == "testnet" {
            icon.image = UIImage(named: "ntw_testnet")
        } else {
            icon.image = SessionsManager.current?.registry?.image(for: transaction.defaultAsset)
        }
    }

    func setSpvVerifyIcon(tx: Transaction) {
        switch tx.spvVerified {
        case "disabled", "verified", nil:
            spvVerifyIcon.isHidden = true
        case "in_progress":
            spvVerifyIcon.isHidden = false
            spvVerifyIcon.image = UIImage(named: "ic_spv_progress")
            spvVerifyIcon.tintColor = .white
        case "not_verified":
            spvVerifyIcon.isHidden = false
            spvVerifyIcon.image = UIImage(named: "ic_spv_warning")
            spvVerifyIcon.tintColor = .red
        default:
            spvVerifyIcon.isHidden = false
            spvVerifyIcon.image = UIImage(named: "ic_spv_warning")
            spvVerifyIcon.tintColor = .yellow
        }
    }

    func checkBlockHeight(transaction: Transaction, blockHeight: UInt32) {

        if transaction.blockHeight == 0 {
            setStatus(.unconfirmed, label: NSLocalizedString("id_unconfirmed", comment: ""))
        } else if isLiquid && blockHeight < transaction.blockHeight + 1 {
            setStatus(.holding, label: NSLocalizedString("id_12_confirmations", comment: ""))
        } else if !isLiquid && blockHeight < transaction.blockHeight + 5 {
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

    func checkTransactionType(transaction: Transaction) {
        if transaction.type == .incoming {
            lblAmount.textColor = isLiquid ? UIColor.blueLight() : UIColor.customMatrixGreen()
        } else {
            lblAmount.textColor = UIColor.white
        }
    }
}
