import UIKit

class RecipientCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!

    @IBOutlet weak var removeRecipientView: UIView!
    @IBOutlet weak var lblRecipientNum: UILabel!

    @IBOutlet weak var lblAddressHint: UILabel!
    @IBOutlet weak var addressContainer: UIView!
    @IBOutlet weak var addressTextView: UITextView!
    @IBOutlet weak var btnCancelAddress: UIButton!
    @IBOutlet weak var btnPasteAddress: UIButton!

    @IBOutlet weak var lblAssetHint: UILabel!
    @IBOutlet weak var iconAsset: UIImageView!
    @IBOutlet weak var lblAssetName: UILabel!
    @IBOutlet weak var disclosureArrow: UIImageView!

    @IBOutlet weak var amountContainer: UIView!
    @IBOutlet weak var amountTextField: UITextField!
    @IBOutlet weak var lblAmountHint: UILabel!
    @IBOutlet weak var lblCurrency: UILabel!
    @IBOutlet weak var btnCancelAmount: UIButton!
    @IBOutlet weak var btnPasteAmount: UIButton!

    @IBOutlet weak var lblAvailableFunds: UILabel!
    @IBOutlet weak var btnSendAll: UIButton!

    var removeRecipient: VoidToVoid?
    var needRefresh: VoidToVoid?
    var recipient: Recipient?
    var chooseAsset: VoidToVoid?
    var qrScan: VoidToVoid?
    var wallet: WalletItem?

    var isFiat: Bool {
        return recipient?.isFiat ?? false
    }

    var isLiquid: Bool {
        get {
            return AccountsManager.shared.current?.gdkNetwork?.liquid ?? false
        }
    }

    var network: String? {
        get {
            return AccountsManager.shared.current?.network
        }
    }

    private var asset: AssetInfo? {
        if let assetId = recipient?.assetId {
            return Registry.shared.infos[assetId] ?? AssetInfo(assetId: assetId, name: assetId, precision: 0, ticker: "")
        }
        return nil
    }

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
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

    // swiftlint:disable function_parameter_count
    func configure(recipient: Recipient,
                   index: Int,
                   isMultiple: Bool,
                   removeRecipient: VoidToVoid?,
                   needRefresh: VoidToVoid?,
                   chooseAsset: VoidToVoid?,
                   qrScan: VoidToVoid?,
                   walletItem: WalletItem?
    ) {
        lblRecipientNum.text = "#\(index + 1)"
        removeRecipientView.isHidden = !isMultiple
        self.recipient = recipient
        self.removeRecipient = removeRecipient
        self.addressTextView.text = recipient.address
        self.addressTextView.delegate = self
        self.amountTextField.text = recipient.amount
        self.needRefresh = needRefresh
        self.chooseAsset = chooseAsset
        self.qrScan = qrScan
        self.wallet = walletItem

        iconAsset.image = UIImage(named: "default_asset_icon")!
        lblAssetName.text = "Asset"
        if network == "mainnet" {
            iconAsset.image = UIImage(named: "ntw_btc")
            lblAssetName.text = "Bitcoin"
        } else if network == "testnet" {
            iconAsset.image = UIImage(named: "ntw_testnet")
            lblAssetName.text = "Testnet Bitcoin"
        } else {
            iconAsset.image = Registry.shared.image(for: asset?.assetId)
            lblAssetName.text = asset?.name ?? "Asset"
        }

        onChange()
    }

    func setStyle() {
        bg.cornerRadius = 8.0
        addressTextView.textContainer.heightTracksTextView = true
        addressTextView.isScrollEnabled = false
        addressContainer.cornerRadius = 6.0
        btnSendAll.setStyle(.outlinedGray)
        amountContainer.cornerRadius = 6.0
        disclosureArrow.image = UIImage(named: "arrow_right")!.maskWithColor(color: UIColor.customTitaniumLight())
    }

    func setContent() {
        lblAddressHint.text = "Recipient Address"
        lblAssetHint.text = "Asset"
        lblAvailableFunds.text = ""
        btnSendAll.setTitle("Send All funds", for: .normal)
        lblAmountHint.text = "Amount"
        lblCurrency.text = ""
        lblRecipientNum.text = "#"
    }

    func onChange() {
        recipient?.address = addressTextView.text
        recipient?.amount = amountTextField.text
        btnCancelAddress.isHidden = !(addressTextView.text.count > 0)
        btnPasteAddress.isHidden = (addressTextView.text.count > 0)
        btnCancelAmount.isHidden = !(amountTextField.text?.count ?? 0 > 0)
        btnPasteAmount.isHidden = (amountTextField.text?.count ?? 0 > 0)
        lblCurrency.text = getCurrency()
        lblAvailableFunds.text = getBalance()
        needRefresh?()
    }

    func getBalance() -> String {
        guard let assetId = recipient?.assetId else {
            return ""
        }
        let satoshi = wallet!.satoshi?[assetId] ?? 0
        let details = btc != assetId ? ["satoshi": satoshi, "asset_info": asset!.encode()!] : ["satoshi": satoshi]
        if let balance = Balance.convert(details: details) {
            let (amount, denom) = balance.get(tag: isFiat ? "fiat" : assetId)
            return "\(amount ?? "N.A.") \(denom)"
        }
        return ""
    }

    func getCurrency() -> String {
        let isMainnet = AccountsManager.shared.current?.gdkNetwork?.mainnet ?? true
        let settings = SessionManager.shared.settings!
        let currency = isMainnet ? settings.getCurrency() : "FIAT"
        let title = isFiat ? currency : settings.denomination.string
        return title
    }

    func getSatoshi() -> UInt64? {
        guard let assetId = recipient?.assetId else {
            return nil
        }
        var amountText = amountTextField.text ?? ""
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else { return nil }
        let isBtc = assetId == btc
        let denominationBtc = SessionManager.shared.settings!.denomination.rawValue
        let key = isFiat ? "fiat" : (isBtc ? denominationBtc : assetId)
        let details: [String: Any]
        if isBtc {
            details = [key: amountText]
        } else {
            details = [key: amountText, "asset_info": asset!.encode()!]
        }
        return Balance.convert(details: details)?.satoshi
    }

    func convertAmount() {
        // remember to avoid asset!

//        if content.sendAllFundsButton.isSelected {
//            content.amountTextField.text = NSLocalizedString("id_all", comment: "")
//            return
//        }
        guard let assetId = recipient?.assetId else {
            return
        }
        let satoshi = getSatoshi() ?? 0
        recipient?.isFiat = !isFiat
        let details = btc != assetId ? ["satoshi": satoshi, "asset_info": asset!.encode()!] : ["satoshi": satoshi]
        let (amount, _) = satoshi == 0 ? ("", "") : Balance.convert(details: details)?.get(tag: isFiat ? "fiat" : assetId) ?? ("", "")
        amountTextField.text = amount
    }

    @IBAction func recipientRemove(_ sender: Any) {
        removeRecipient?()
    }

    @IBAction func btnCancelAddress(_ sender: Any) {
        addressTextView.text = ""
        onChange()
    }

    @IBAction func btnPasteAddress(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            addressTextView.text = txt
        }
        onChange()
    }

    @IBAction func btnQr(_ sender: Any) {
        qrScan?()
    }

    @IBAction func btnChooseAsset(_ sender: Any) {
        if isLiquid {
            chooseAsset?()
        }
    }

    @IBAction func btnSendAll(_ sender: Any) {
        print("btnSendAll")
    }

    @IBAction func btnCancelAmount(_ sender: Any) {
        amountTextField.text = ""
        onChange()
    }

    @IBAction func btnPasteAmount(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            amountTextField.text = txt
        }
        onChange()
    }

    @IBAction func btnConvert(_ sender: Any) {
        convertAmount()
        onChange()
    }

    @IBAction func amountDidChange(_ sender: Any) {
        onChange()
    }
}

extension RecipientCell: UITextViewDelegate {
    func textViewDidChange(_ textView: UITextView) {
        onChange()
    }
}
