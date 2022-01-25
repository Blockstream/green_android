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
    @IBOutlet weak var lblAddressError: UILabel!

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
    @IBOutlet weak var btnConvert: UIButton!
    @IBOutlet weak var lblAmountError: UILabel!
    @IBOutlet weak var lblAmountExchange: UILabel!

    @IBOutlet weak var lblAvailableFunds: UILabel!
    @IBOutlet weak var btnSendAll: UIButton!

    var removeRecipient: VoidToVoid?
    var needRefresh: VoidToVoid?
    var recipient: Recipient?
    var chooseAsset: VoidToVoid?
    var qrScan: VoidToVoid?
    var wallet: WalletItem?
    var tapSendAll: VoidToVoid?
    var isSendAll: Bool = false
    var isSweep: Bool = false
    var isBumpFee: Bool = false
    var validateTransaction: VoidToVoid?

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
                   walletItem: WalletItem?,
                   tapSendAll: VoidToVoid?,
                   isSendAll: Bool,
                   isSweep: Bool,
                   isBumpFee: Bool,
                   validateTransaction: VoidToVoid?
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
        self.tapSendAll = tapSendAll
        self.isSendAll = isSendAll
        self.isSweep = isSweep
        self.isBumpFee = isBumpFee
        self.validateTransaction = validateTransaction
        lblAddressError.isHidden = true
        lblAmountError.isHidden = true
        lblAmountExchange.isHidden = true

        lblAddressHint.text = NSLocalizedString(isSweep ? "id_enter_a_private_key_to_sweep" : "id_enter_an_address", comment: "")
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
            let name = asset?.assetId == btc ? "Liquid Bitcoin" : asset?.name
            lblAssetName.text = name ?? "Asset"
        }
        onChange()
        amountTextField.addDoneButtonToKeyboard(myAction: #selector(self.amountTextField.resignFirstResponder))
    }

    func setStyle() {
        bg.cornerRadius = 8.0
        addressTextView.textContainer.heightTracksTextView = true
        addressTextView.isScrollEnabled = false
        addressContainer.cornerRadius = 6.0
        addressContainer.borderWidth = 1.0
        addressContainer.borderColor = UIColor.customTextFieldBg()
        amountContainer.borderWidth = 1.0
        amountContainer.borderColor = UIColor.customTextFieldBg()
        btnSendAll.setStyle(.outlinedGray)
        amountContainer.cornerRadius = 6.0
        disclosureArrow.image = UIImage(named: "arrow_right")!.maskWithColor(color: UIColor.customTitaniumLight())
    }

    func setContent() {
        lblAssetHint.text = "Asset"
        lblAvailableFunds.text = ""
        btnSendAll.setTitle("Send All funds", for: .normal)
        lblAmountHint.text = "Amount"
        lblCurrency.text = ""
        lblRecipientNum.text = "#"
    }

    func onChange() {
        recipient?.address = addressTextView.text
        btnCancelAddress.isHidden = !(addressTextView.text.count > 0)
        btnPasteAddress.isHidden = (addressTextView.text.count > 0)
        btnCancelAmount.isHidden = !(amountTextField.text?.count ?? 0 > 0)
        btnPasteAmount.isHidden = (amountTextField.text?.count ?? 0 > 0)
        lblCurrency.text = getDenomination()
        lblAvailableFunds.text = getBalance()
        btnSendAll.isHidden = recipient?.assetId == nil
        if isSendAll {
            btnSendAll.setStyle(.primary)
            recipient?.amount = nil
            amountTextField.text = ""
            amountTextField.alpha = 0.6
            btnConvert.alpha = 0.6
        } else {
            btnSendAll.setStyle(.outlinedGray)
            recipient?.amount = amountTextField.text
            amountTextField.alpha = 1.0
            btnConvert.alpha = 1.0
        }
        amountTextField.isUserInteractionEnabled = !isSendAll && recipient?.assetId != nil
        btnConvert.isUserInteractionEnabled = !isSendAll && recipient?.assetId != nil
        btnPasteAmount.isUserInteractionEnabled = !isSendAll && recipient?.assetId != nil
        btnCancelAmount.isUserInteractionEnabled = !isSendAll && recipient?.assetId != nil

        btnConvert.isHidden = !(recipient?.assetId == "btc" || recipient?.assetId == getGdkNetwork("liquid").policyAsset)

        if let address = addressTextView.text {
            if address.starts(with: "bitcoin:") || address.starts(with: "liquidnetwork:") || isSweep {
                lblAvailableFunds.isHidden = true
                btnSendAll.isHidden = true
                btnConvert.isHidden = true
                btnPasteAmount.isUserInteractionEnabled = false
                btnPasteAmount.alpha = 0.6
                btnCancelAmount.isUserInteractionEnabled = false
                btnCancelAmount.alpha = 0.6
                amountTextField.isUserInteractionEnabled = false
                amountTextField.alpha = 0.6
            }
        }
        if isBumpFee {
            isUserInteractionEnabled = false
            bg.alpha = 0.6
        }

        needRefresh?()
    }

    func onTransactionValidate(_ tx: Transaction?) {
        addressContainer.borderColor = UIColor.customTextFieldBg()
        amountContainer.borderColor = UIColor.customTextFieldBg()
        lblAddressError.isHidden = true
        lblAmountError.isHidden = true
        lblAmountExchange.isHidden = true

        if tx?.error == "id_invalid_address" && !addressTextView.text.isEmpty {
            addressContainer.borderColor = UIColor.errorRed()
            lblAddressError.isHidden = false
            lblAddressError.text = NSLocalizedString(tx?.error ?? "Error", comment: "")
        } else if (tx?.error == "id_invalid_amount" || tx?.error == "id_insufficient_funds") && !(amountTextField.text ?? "").isEmpty {
            amountContainer.borderColor = UIColor.errorRed()
            lblAmountError.isHidden = false
            lblAmountError.text = NSLocalizedString(tx?.error ?? "Error", comment: "")
        }
        let isBip21 = addressTextView.text.starts(with: "bitcoin:") || addressTextView.text.starts(with: "liquidnetwork:")
        if let transaction = tx, tx?.sendAll == true || isBip21 {
            let asset = transaction.defaultAsset
            let info = Registry.shared.infos[asset] ?? AssetInfo(assetId: asset, name: "", precision: 0, ticker: "")
            let details = ["satoshi": transaction.amounts[asset]!, "asset_info": info.encode()!] as [String: Any]
            if asset == "btc" {
                if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
                    let (value, _) = balance.get(tag: btc)
                    amountTextField.text = value ?? ""
                }
            } else {
                if let balance = Balance.convert(details: details) {
                    let (amount, _) = balance.get(tag: transaction.defaultAsset)
                    amountTextField.text = amount ?? ""
                }
            }
        }
        if let transaction = tx, transaction.error.isEmpty {
            let asset = transaction.defaultAsset
            if asset == "btc" || asset == getGdkNetwork("liquid").policyAsset {
                lblAmountExchange.isHidden = false
                if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
                    let (fiat, fiatCurrency) = balance.get(tag: !isFiat ? "fiat" : asset)
                    lblAmountExchange.text = "≈ \(fiat ?? "N.A.") \(fiatCurrency)"
                }
            }
        }
        // id_invalid_asset_id
        // id_invalid_address
        // id_invalid_amount
        // id_insufficient_funds
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

    func getDenomination() -> String {
        guard let assetId = recipient?.assetId else {
            return ""
        }
        let satoshi = wallet!.satoshi?[assetId] ?? 0
        let details = btc != assetId ? ["satoshi": satoshi, "asset_info": asset!.encode()!] : ["satoshi": satoshi]
        if let balance = Balance.convert(details: details) {
            let (_, denom) = balance.get(tag: isFiat ? "fiat" : assetId)
            return "\(denom)"
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
        guard let assetId = recipient?.assetId else {
            return
        }
        let satoshi = getSatoshi() ?? 0
        recipient?.isFiat = !isFiat
        let details = btc != assetId ? ["satoshi": satoshi, "asset_info": asset!.encode()!] : ["satoshi": satoshi]
        let (amount, _) = satoshi == 0 ? ("", "") : Balance.convert(details: details)?.get(tag: isFiat ? "fiat" : assetId) ?? ("", "")
        amountTextField.text = amount
    }

    @objc func triggerTextChange() {
        onChange()
        validateTransaction?()
    }

    @IBAction func recipientRemove(_ sender: Any) {
        removeRecipient?()
    }

    @IBAction func btnCancelAddress(_ sender: Any) {
        addressTextView.text = ""
        onChange()
        validateTransaction?()
    }

    @IBAction func btnPasteAddress(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            addressTextView.text = txt
        }
        onChange()
        validateTransaction?()
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
        tapSendAll?()
        if isFiat == true {
            convertAmount()
        }
        amountTextField.text = ""
        onChange()
        validateTransaction?()
    }

    @IBAction func btnCancelAmount(_ sender: Any) {
        amountTextField.text = ""
        onChange()
        validateTransaction?()
    }

    @IBAction func btnPasteAmount(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            amountTextField.text = txt
        }
        onChange()
        validateTransaction?()
    }

    @IBAction func btnConvert(_ sender: Any) {
        convertAmount()
        onChange()
        validateTransaction?()
    }

    @IBAction func amountDidChange(_ sender: Any) {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.triggerTextChange), object: nil)
        perform(#selector(self.triggerTextChange), with: nil, afterDelay: 0.5)
    }
}

extension RecipientCell: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        endEditing(true)
        return false
    }
}

extension RecipientCell: UITextViewDelegate {
    func textViewDidChange(_ textView: UITextView) {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.triggerTextChange), object: nil)
        perform(#selector(self.triggerTextChange), with: nil, afterDelay: 0.5)
    }

    func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        if text == "\n" {
            textView.resignFirstResponder()
            return false
        }
        return true
    }
}
