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
    var inputType: InputType = .transaction

    weak var delegate: RecipientCellDelegate?
    var index: Int?

    var isSendAll: Bool {
        return recipient?.isSendAll == true
    }

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
            return SessionsManager.current?.registry?.info(for: assetId)
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

    func configure(recipient: Recipient,
                   index: Int,
                   isMultiple: Bool,
                   walletItem: WalletItem?,
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
        self.inputType = inputType

        lblAddressError.isHidden = true
        lblAmountError.isHidden = true
        lblAmountExchange.isHidden = true

        lblAddressHint.text = NSLocalizedString(inputType == .sweep ? "id_enter_a_private_key_to_sweep" : "id_enter_an_address", comment: "")
        iconAsset.image = UIImage(named: "default_asset_icon")!
        lblAssetName.text = NSLocalizedString("id_asset", comment: "")
        iconAsset.image = SessionsManager.current?.registry?.image(for: asset?.assetId ?? "")
        lblAssetName.text = SessionsManager.current?.registry?.info(for: asset?.assetId ?? "").name
        onChange()
        amountTextField.addDoneButtonToKeyboard(myAction: #selector(self.amountTextField.resignFirstResponder))
        addressTextView.textContainer.maximumNumberOfLines = 10

        btnPasteAddress.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.pasteAddressBtn
        amountTextField.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.amountField
        btnChooseAsset.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.chooseAssetBtn
        btnSendAll.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.sendAllBtn
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
        lblAssetHint.text = NSLocalizedString("id_asset", comment: "")
        lblAvailableFunds.text = ""
        btnSendAll.setTitle(NSLocalizedString("id_send_all_funds", comment: ""), for: .normal)
        lblAmountHint.text = NSLocalizedString("id_amount", comment: "")
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

    func onTransactionValidate() {
        addressContainer.borderColor = UIColor.customTextFieldBg()
        amountContainer.borderColor = UIColor.customTextFieldBg()
        assetBorder.backgroundColor = UIColor.customTitaniumLight()
        lblAddressError.isHidden = true
        lblAmountError.isHidden = true
        lblAmountExchange.isHidden = true

        if recipient?.txError == "id_invalid_address" || recipient?.txError == "id_invalid_private_key" {
            addressContainer.borderColor = UIColor.errorRed()
            lblAddressError.isHidden = false
            lblAddressError.text = NSLocalizedString(recipient?.txError ?? "Error", comment: "")
        } else if recipient?.txError == "id_invalid_amount" || recipient?.txError == "id_insufficient_funds" {
            amountContainer.borderColor = UIColor.errorRed()
            lblAmountError.isHidden = false
            lblAmountError.text = NSLocalizedString(recipient?.txError ?? "Error", comment: "")
        } else if recipient?.txError == "id_invalid_payment_request_assetid" || recipient?.txError == "id_invalid_asset_id" {
            assetBorder.backgroundColor = UIColor.errorRed()
        } else if !(recipient?.txError ?? "").isEmpty {
            print(recipient?.txError ?? "Error")
        }

        if isBipAddress() {
            if recipient?.txError == "id_invalid_payment_request_assetid" || recipient?.txError == "id_invalid_asset_id" {
                iconAsset.image = UIImage(named: "default_asset_icon")
                lblAssetName.text = NSLocalizedString("id_asset", comment: "")
                lblCurrency.text = ""
                lblAvailableFunds.text = ""
                amountTextField.text = ""
            } else {
                iconAsset.image = SessionsManager.current?.registry?.image(for: recipient?.assetId ?? "")
                lblAssetName.text = getDenomination()
                lblCurrency.text = getDenomination()
                lblAvailableFunds.text = getBalance()
                updateAmountTextField()
            }
        }
        if inputType == .sweep || isSendAll {
            updateAmountTextField()
        }

        if let satoshi = getSatoshi(), (recipient?.txError ?? "").isEmpty, let asset = recipient?.assetId {
            if asset == "btc" || asset == getGdkNetwork("liquid").policyAsset,
                let balance = Balance.fromSatoshi(satoshi) {
                lblAmountExchange.isHidden = false
                if isFiat {
                    let (fiat, fiatCurrency) = balance.toFiat()
                    lblAmountExchange.text = "≈ \(fiat) \(fiatCurrency)"
                } else {
                    let (fiat, fiatCurrency) = balance.toValue()
                    lblAmountExchange.text = "≈ \(fiat) \(fiatCurrency)"
                }
            }
        }
    }

    func updateAmountTextField() {
        amountTextField.text = recipient?.amount
    }

    func getBalance() -> String {
        guard let assetId = recipient?.assetId else {
            return ""
        }
        let satoshi = wallet!.satoshi?[assetId] ?? 0
        if let balance = Balance.fromSatoshi(satoshi, asset: asset) {
            let (amount, denom) = isFiat ? balance.toFiat() : balance.toValue()
            return "\(amount) \(denom)"
        }
        return ""
    }

    func getDenomination() -> String {
        guard let assetId = recipient?.assetId else {
            return ""
        }
        let satoshi = wallet!.satoshi?[assetId] ?? 0
        if let balance = Balance.fromSatoshi(satoshi, asset: asset) {
            let (_, denom) = isFiat ? balance.toFiat() : balance.toValue()
            return "\(denom)"
        }
        return ""
    }

    func getSatoshi() -> UInt64? {
        var amountText = amountTextField.text ?? ""
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else { return nil }
        let balance = isFiat ? Balance.fromFiat(amountText) :
            Balance.fromDenomination(amountText)
        return balance?.satoshi
    }

    func convertAmount() {
        if recipient?.assetId != nil {
            return
        }
        let satoshi = getSatoshi() ?? 0
        recipient?.isFiat = !isFiat
        if isFiat {
            amountTextField.text = Balance.fromSatoshi(satoshi, asset: asset)?.toFiat().0
        } else {
            amountTextField.text = Balance.fromSatoshi(satoshi, asset: asset)?.toValue().0
        }
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
        recipient?.isSendAll.toggle()
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
