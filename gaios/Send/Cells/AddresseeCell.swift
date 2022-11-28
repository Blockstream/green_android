import UIKit

class AddresseeCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!

    @IBOutlet weak var lblRecipientTitle: UILabel!
    @IBOutlet weak var lblRecipientAddress: UILabel!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblDenomination: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblFiat: UILabel!

    var isFiat = false

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
        let account = subaccount(from: transaction)
        let addressee = transaction.addressees[index]
        lblRecipientTitle.text = NSLocalizedString("id_recipient", comment: "")
        lblRecipientAddress.text = addressee.address

        let addreessee = transaction.addressees.first
        var value = addreessee?.satoshi ?? 0
        let asset = (account?.gdkNetwork.liquid ?? false) ? addreessee?.assetId ?? "" : "btc"
        if !(AccountsManager.shared.current?.isSingleSig ?? false) && transaction.sendAll {
            value = transaction.amounts.filter({$0.key == asset}).first?.value ?? 0
        }
        let assetInfo = WalletManager.current?.registry.info(for: asset)
        let feeAsset = account?.gdkNetwork.getFeeAsset()
        if let balance = Balance.fromSatoshi(value, asset: assetInfo) {
            let (amount, ticker) = value == 0 ? ("", "") : balance.toValue()
            lblAmount.text = amount
            lblDenomination.text = ticker
            if asset == feeAsset {
                let (fiat, fiatCurrency) = balance.toFiat()
                lblFiat.text = "≈ \(fiat) \(fiatCurrency)"
            }
            lblFiat.isHidden = asset != feeAsset
        }
        icon.image = WalletManager.current?.registry.image(for: asset)
    }

    func subaccount(from tx: Transaction) -> WalletItem? {
        return WalletManager.current?.subaccounts.filter { $0.hashValue == tx.subaccount }.first
    }

    func setStyle() {
        bg.cornerRadius = 8.0
    }

    func setContent() {
    }
}
