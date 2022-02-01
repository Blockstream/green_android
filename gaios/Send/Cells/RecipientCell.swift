import UIKit

protocol RecipientCellDelegate: AnyObject {
    func removeRecipient(_ index: Int)
    func needRefresh()
    func chooseAsset(_ index: Int)
    func qrScan(_ index: Int)
    func tapSendAll()
    func validateTx()
    func onFocus()
}

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
    @IBOutlet weak var assetBorder: UIView!
    @IBOutlet weak var btnChooseAsset: UIButton!
    @IBOutlet weak var assetBox: UIView!

    @IBOutlet weak var amountContainer: UIView!
    @IBOutlet weak var amountTextField: UITextField!
    @IBOutlet weak var lblAmountHint: UILabel!
    @IBOutlet weak var lblCurrency: UILabel!
    @IBOutlet weak var btnCancelAmount: UIButton!
    @IBOutlet weak var btnPasteAmount: UIButton!
    @IBOutlet weak var btnConvert: UIButton!
    @IBOutlet weak var lblAmountError: UILabel!
    @IBOutlet weak var lblAmountExchange: UILabel!
    @IBOutlet weak var amountBox: UIView!

    @IBOutlet weak var lblAvailableFunds: UILabel!
    @IBOutlet weak var btnSendAll: UIButton!

    var recipient: Recipient?
    var wallet: WalletItem?
    var isSendAll: Bool = false
    var inputType: InputType = .transaction

    weak var delegate: RecipientCellDelegate?
    var index: Int?

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
                   walletItem: WalletItem?,
                   isSendAll: Bool,
                   inputType: InputType
    ) {
        lblRecipientNum.text = "#\(index + 1)"
        removeRecipientView.isHidden = !isMultiple
        self.index = index
        self.recipient = recipient
        self.addressTextView.text = recipient.address
        self.addressTextView.delegate = self
        self.amountTextField.text = recipient.amount
        self.amountTextField.delegate = self
        self.wallet = walletItem
        self.isSendAll = isSendAll
        self.inputType = inputType

        lblAddressError.isHidden = true
        lblAmountError.isHidden = true
        lblAmountExchange.isHidden = true

        lblAddressHint.text = NSLocalizedString(inputType == .sweep ? "id_enter_a_private_key_to_sweep" : "id_enter_an_address", comment: "")
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
        addressTextView.textContainer.maximumNumberOfLines = 10
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
        btnChooseAsset.isUserInteractionEnabled = true
        assetBox.alpha = 1.0
        amountBox.alpha = 1.0

        if isSendAll {
            btnSendAll.setStyle(.primary)
            recipient?.amount = nil
            amountTextField.text = ""
            amountBox.alpha = 0.6
        } else {
            btnSendAll.setStyle(.outlinedGray)
            recipient?.amount = amountTextField.text
        }
        amountFieldIsEnabled(!isSendAll && recipient?.assetId != nil)
        btnConvert.isUserInteractionEnabled = !isSendAll && recipient?.assetId != nil
        btnPasteAmount.isUserInteractionEnabled = !isSendAll && recipient?.assetId != nil
        btnCancelAmount.isUserInteractionEnabled = !isSendAll && recipient?.assetId != nil

        btnConvert.isHidden = !(recipient?.assetId == "btc" || recipient?.assetId == getGdkNetwork("liquid").policyAsset)

        if let address = addressTextView.text {
            if address.starts(with: "bitcoin:") || address.starts(with: "liquidnetwork:") {
                btnSendAll.isHidden = true
                btnConvert.isHidden = true
                btnPasteAmount.isUserInteractionEnabled = false
                btnCancelAmount.isUserInteractionEnabled = false
                btnChooseAsset.isUserInteractionEnabled = false
                assetBox.alpha = 0.6
                amountFieldIsEnabled(false)
            }
            if inputType == .sweep {
                lblAvailableFunds.isHidden = true
                btnSendAll.isHidden = true
                btnConvert.isHidden = true
                btnPasteAmount.isUserInteractionEnabled = false
                btnCancelAmount.isUserInteractionEnabled = false
                amountFieldIsEnabled(false)
            }
        }
        if inputType == .bumpFee {
            isUserInteractionEnabled = false
            bg.alpha = 0.6
        }

        delegate?.needRefresh()
    }

    func amountFieldIsEnabled(_ value: Bool) {
        amountTextField.isUserInteractionEnabled = value
        amountBox.alpha = value ? 1.0 : 0.6
    }

    func onTransactionValidate(_ tx: Transaction?) {
        addressContainer.borderColor = UIColor.customTextFieldBg()
        amountContainer.borderColor = UIColor.customTextFieldBg()
        assetBorder.backgroundColor = UIColor.customTitaniumLight()
        lblAddressError.isHidden = true
        lblAmountError.isHidden = true
        lblAmountExchange.isHidden = true

        if tx?.error == "id_invalid_address" || tx?.error == "id_invalid_private_key" {
            addressContainer.borderColor = UIColor.errorRed()
            lblAddressError.isHidden = false
            lblAddressError.text = NSLocalizedString(tx?.error ?? "Error", comment: "")
        } else if tx?.error == "id_invalid_amount" || tx?.error == "id_insufficient_funds" {
            amountContainer.borderColor = UIColor.errorRed()
            lblAmountError.isHidden = false
            lblAmountError.text = NSLocalizedString(tx?.error ?? "Error", comment: "")
        } else if tx?.error == "id_invalid_payment_request_assetid" || tx?.error == "id_invalid_asset_id" {
            assetBorder.backgroundColor = UIColor.errorRed()
        } else if !(tx?.error ?? "").isEmpty {
            print(tx?.error ?? "Error")
        }

        if let transaction = tx, tx?.sendAll == true {
            updateUISendAll(transaction)
        }

        if let transaction = tx, isBipAddress() {
            updateUIBipAddress(transaction)
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
    }

    func updateUISendAll(_ transaction: Transaction) {
        let asset = transaction.defaultAsset
        let info = Registry.shared.infos[asset] ?? AssetInfo(assetId: asset, name: "", precision: 0, ticker: "")
        if asset == "btc" {
            if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
                let (value, _) = balance.get(tag: btc)
                amountTextField.text = value ?? ""
            }
        } else {
            if transaction.error.isEmpty {
                let details = ["satoshi": transaction.amounts[asset]!, "asset_info": info.encode()!] as [String: Any]
                if let balance = Balance.convert(details: details) {
                    let (amount, _) = balance.get(tag: transaction.defaultAsset)
                    amountTextField.text = amount ?? ""
                }
            }
        }
    }

    func updateUIBipAddress(_ transaction: Transaction) {
        recipient?.assetId = transaction.defaultAsset
        if transaction.error == "id_invalid_payment_request_assetid" || transaction.error == "id_invalid_asset_id" {
            iconAsset.image = UIImage(named: "default_asset_icon")
            lblAssetName.text = "Asset"
            lblCurrency.text = ""
            lblAvailableFunds.text = ""
            amountTextField.text = ""
        } else {
            iconAsset.image = Registry.shared.image(for: recipient?.assetId)
            lblAssetName.text = getDenomination()
            lblCurrency.text = getDenomination()
            lblAvailableFunds.text = getBalance()
            let asset = transaction.defaultAsset
            if asset == "btc" {
                if let balance = Balance.convert(details: ["satoshi": transaction.satoshi]) {
                    let (value, _) = balance.get(tag: btc)
                    amountTextField.text = value ?? ""
                }
            } else {
                let info = Registry.shared.infos[asset] ?? AssetInfo(assetId: asset, name: "", precision: 0, ticker: "")
                let details = ["satoshi": transaction.amounts[asset]!, "asset_info": info.encode()!] as [String: Any]
                if let balance = Balance.convert(details: details) {
                    let (amount, _) = balance.get(tag: transaction.defaultAsset)
                    amountTextField.text = amount ?? ""
                }
            }
        }
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

    func isBipAddress() -> Bool {
        return addressTextView.text.starts(with: "bitcoin:") || addressTextView.text.starts(with: "liquidnetwork:")
    }

    @objc func triggerTextChange() {
        onChange()
        delegate?.validateTx()
    }

    @IBAction func recipientRemove(_ sender: Any) {
        if let i = index {
            delegate?.removeRecipient(i)
        }
    }

    @IBAction func btnCancelAddress(_ sender: Any) {
        addressTextView.text = ""
        onChange()
        delegate?.validateTx()
    }

    @IBAction func btnPasteAddress(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            addressTextView.text = txt
        }
        onChange()
        delegate?.validateTx()
    }

    @IBAction func btnQr(_ sender: Any) {
        if let i = index {
            delegate?.qrScan(i)
        }
    }

    @IBAction func btnChooseAsset(_ sender: Any) {
        if isLiquid {
            if let i = index {
                delegate?.chooseAsset(i)
            }
        }
    }

    @IBAction func btnSendAll(_ sender: Any) {
        delegate?.tapSendAll()
        if isFiat == true {
            convertAmount()
        }
        amountTextField.text = ""
        onChange()
        delegate?.validateTx()
    }

    @IBAction func btnCancelAmount(_ sender: Any) {
        amountTextField.text = ""
        onChange()
        delegate?.validateTx()
    }

    @IBAction func btnPasteAmount(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            amountTextField.text = txt
        }
        onChange()
        delegate?.validateTx()
    }

    @IBAction func btnConvert(_ sender: Any) {
        convertAmount()
        onChange()
        delegate?.validateTx()
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

    func textFieldDidBeginEditing(_ textField: UITextField) {
        if textField == amountTextField {
            delegate?.onFocus()
        }
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
