import UIKit

class AddresseeCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!

    @IBOutlet weak var lblRecipientTitle: UILabel!
    @IBOutlet weak var lblRecipientAddress: UILabel!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblDenomination: UILabel!
    @IBOutlet weak var lblAmount: UILabel!

    var isFiat = false

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    var network: String? {
        get {
            return AccountsManager.shared.current?.network
        }
    }

    override func awakeFromNib() {
        super.awakeFromNib()
        setStyle()
        setContent()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
    }

    func configure(addressee: Addressee?) {
        lblRecipientTitle.text = "Recipient"
        lblRecipientAddress.text = addressee?.address

        let isLiquid = AccountsManager.shared.current?.gdkNetwork?.liquid ?? false
        if isLiquid {
            let tag = addressee?.assetId ?? "btc"
            // let info = Registry.shared.infos[tag]
            var icon = Registry.shared.image(for: tag)
            if network == "mainnet" {
                icon = UIImage(named: "ntw_btc")
            } else if network == "testnet" {
                icon = UIImage(named: "ntw_testnet")
            }
            self.icon.image = icon
//            content.assetTableCell?.configure(tag: tag, info: info, icon: icon, satoshi: addressee.satoshi, negative: false, isTransaction: false, sendAll: transaction.sendAll)
        }

        if let balance = Balance.convert(details: ["satoshi": addressee?.satoshi ?? 0]) {
            let (amount, denom) = balance.get(tag: isFiat ? "fiat" : btc)
//            if transaction.sendAll {
//                content.amountText.text! = NSLocalizedString("id_all", comment: "")
//            } else {
                lblAmount.text = amount ?? "N.A."
                lblDenomination.text = denom
//            }
        }

//        if let balance = Balance.convert(details: ["satoshi": transaction.fee]) {
//            let (amount, denom) = balance.get(tag: isFiat ? "fiat" : btc)
//            content.assetsFeeLabel.text = "\(amount ?? "N.A.") \(denom)"
//            content.feeLabel.text = "\(amount ?? "N.A.") \(denom)"
//        }

        // Show change address only for hardware wallet transaction
//        let isHW = AccountsManager.shared.current?.isHW ?? false
//        content.changeAddressView.isHidden = !isHW
//        if let outputs = transaction.transactionOutputs, !outputs.isEmpty, isHW {
//            var changeAddress = [String]()
//            outputs.forEach { output in
//                let isChange = output["is_change"] as? Bool ?? false
//                let isFee = output["is_fee"] as? Bool ?? false
//                if isChange && !isFee, let address = output["address"] as? String {
//                    changeAddress.append(address)
//                }
//            }
//            content.changeAddressValue.text = changeAddress.map { "- \($0)"}.joined(separator: "\n")
//        }
    }

    func setStyle() {
        bg.cornerRadius = 8.0
    }

    func setContent() {
    }
}
