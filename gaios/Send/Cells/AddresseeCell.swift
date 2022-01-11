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

    func configure(transaction: Transaction, index: Int) {
        let addressee = transaction.addressees[index]
        let isSendAll = transaction.sendAll
        lblRecipientTitle.text = "Recipient"
        lblRecipientAddress.text = addressee.address

        let asset = transaction.defaultAsset
        let info = Registry.shared.infos[asset] ?? AssetInfo(assetId: asset, name: "", precision: 0, ticker: "")
        let details = ["satoshi": transaction.amounts[asset]!, "asset_info": info.encode()!] as [String: Any]
        if asset == "btc" {
            if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
                let (value, denom) = balance.get(tag: btc)
                lblAmount.text = value ?? ""
                lblDenomination.text = "\(denom)"
            }
        } else {
            if let balance = Balance.convert(details: details) {
                let (amount, ticker) = balance.get(tag: transaction.defaultAsset)
                lblAmount.text = amount ?? "N.A."
                lblDenomination.text = "\(ticker)"
            }
        }
        if isSendAll {
            lblAmount.text = NSLocalizedString("id_all", comment: "")
        }
        icon.image = UIImage(named: "default_asset_icon")!
        if network == "mainnet" {
            icon.image = UIImage(named: "ntw_btc")
        } else if network == "testnet" {
            icon.image = UIImage(named: "ntw_testnet")
        } else {
            icon.image = Registry.shared.image(for: asset)
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
