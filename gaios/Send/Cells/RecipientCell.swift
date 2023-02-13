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

    @IBOutlet weak var lblAccountAsset: UILabel!
    @IBOutlet weak var lblAddressHint: UILabel!
    @IBOutlet weak var addressContainer: UIView!
    @IBOutlet weak var addressTextView: UITextView!
    @IBOutlet weak var btnCancelAddress: UIButton!
    @IBOutlet weak var btnPasteAddress: UIButton!
    @IBOutlet weak var lblAddressError: UILabel!

    @IBOutlet weak var iconAsset: UIImageView!
    @IBOutlet weak var lblAssetName: UILabel!
    @IBOutlet weak var lblAccount: UILabel!
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

    weak var delegate: RecipientCellDelegate?
    var updateModel: ((RecipientCellModel?) -> Void)?
    var index: Int?
    var model: RecipientCellModel?

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

    func configure(cellModel: RecipientCellModel,
                   index: Int,
                   isMultiple: Bool) {
        lblRecipientNum.text = "#\(index + 1)"
        removeRecipientView.isHidden = !isMultiple
        model = cellModel
        addressTextView.delegate = self
        amountTextField.delegate = self
        self.index = index
        reload()
        onTransactionValidate()

        amountTextField.addDoneButtonToKeyboard(myAction: #selector(self.amountTextField.resignFirstResponder))
        addressTextView.addDoneButtonToKeyboard(myAction: #selector(self.addressTextView.resignFirstResponder))
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
        addressContainer.cornerRadius = 8.0
        addressContainer.borderWidth = 1.0
        addressContainer.borderColor = UIColor.gGrayCard()
        amountContainer.borderWidth = 1.0
        amountContainer.borderColor = UIColor.gGrayCard()
        btnSendAll.setStyle(.outlinedGray)
        amountContainer.cornerRadius = 8.0

        assetBox.cornerRadius = 8.0
        assetBox.borderWidth = 1.0
        assetBox.borderColor = UIColor.gGrayCard()
    }

    func setContent() {
        lblAvailableFunds.text = ""
        btnSendAll.setTitle("id_send_all_funds".localized, for: .normal)
        lblAmountHint.text = "id_amount".localized
        lblCurrency.text = ""
        lblRecipientNum.text = "#"
    }

    func reload() {
        guard let model = model else { return }
         // from model to view
        lblAccountAsset.text = "Asset & Account".localized
        addressTextView.text = model.address ?? ""
        amountTextField.text = model.amount ?? ""
        lblAssetName.text = "id_asset".localized
        iconAsset.image = model.assetImage ?? UIImage(named: "default_asset_icon")!
        lblAssetName.text = model.asset?.name ?? model.assetId ?? "id_asset".localized
        lblAccount.text = model.account.localizedName()
        lblAddressHint.text = model.inputType == .sweep ? "id_enter_a_private_key_to_sweep".localized : "id_enter_an_address".localized

        btnCancelAddress.isHidden = !(addressTextView.text.count > 0)
        btnPasteAddress.isHidden = (addressTextView.text.count > 0)
        btnCancelAmount.isHidden = !(amountTextField.text?.count ?? 0 > 0)
        btnPasteAmount.isHidden = (amountTextField.text?.count ?? 0 > 0)
        lblCurrency.text = model.ticker
        lblAvailableFunds.text = model.balance
        btnSendAll.isHidden = !model.isBtc
        btnChooseAsset.isUserInteractionEnabled = true
        assetBox.alpha = 1.0
        amountBox.alpha = 1.0
        btnConvert.alpha = 1.0

        if model.isSendAll {
            btnSendAll.setStyle(.primary)
            amountTextField.text = ""
            amountBox.alpha = 0.6
        } else {
            btnSendAll.setStyle(.outlinedGray)
        }
        amountFieldIsEnabled(!model.isSendAll)
        btnConvert.isUserInteractionEnabled = !model.isSendAll && model.isBtc
        btnPasteAmount.isUserInteractionEnabled = !model.isSendAll && model.isBtc
        btnCancelAmount.isUserInteractionEnabled = !model.isSendAll && model.isBtc
        btnConvert.isUserInteractionEnabled = model.isBtc
        btnConvert.alpha = model.isBtc ? 1.0 : 0.6

        if model.isBipAddress() {
            btnSendAll.isHidden = true
            btnConvert.isUserInteractionEnabled = false
            btnPasteAmount.isUserInteractionEnabled = false
            btnCancelAmount.isUserInteractionEnabled = false
            btnChooseAsset.isUserInteractionEnabled = false
            assetBox.alpha = 0.6
            amountFieldIsEnabled(false)
        }
        if model.inputType == .sweep {
            lblAvailableFunds.isHidden = true
            btnSendAll.isHidden = true
            btnPasteAmount.isUserInteractionEnabled = false
            btnCancelAmount.isUserInteractionEnabled = false
            amountFieldIsEnabled(false)
        }
        if model.inputType == .bumpFee {
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
        guard let model = model else { return }
        addressContainer.borderColor = UIColor.gGrayCard()
        amountContainer.borderColor = UIColor.gGrayCard()
        assetBox.borderColor = UIColor.gGrayCard()
        lblAddressError.isHidden = true
        lblAmountError.isHidden = true
        lblAmountExchange.isHidden = true

        if model.txError == "id_invalid_address" || model.txError == "id_invalid_private_key" {
            addressContainer.borderColor = UIColor.errorRed()
            lblAddressError.isHidden = false
            lblAddressError.text = NSLocalizedString(model.txError, comment: "")
        } else if model.txError == "id_invalid_amount" || model.txError == "id_insufficient_funds" {
            amountContainer.borderColor = UIColor.errorRed()
            lblAmountError.isHidden = false
            lblAmountError.text = NSLocalizedString(model.txError, comment: "")
        } else if model.txError == "id_invalid_payment_request_assetid" || model.txError == "id_invalid_asset_id" {
            assetBox.borderColor = UIColor.errorRed()
        } else if !(model.txError).isEmpty {
            print(model.txError)
        }

        if model.isBipAddress() {
            if model.txError == "id_invalid_payment_request_assetid" || model.txError == "id_invalid_asset_id" {
                iconAsset.image = UIImage(named: "default_asset_icon")
                lblAssetName.text = NSLocalizedString("id_asset", comment: "")
                lblCurrency.text = ""
                lblAvailableFunds.text = ""
                amountTextField.text = ""
            } else {
                iconAsset.image = model.assetImage
                lblAssetName.text = model.ticker
                lblCurrency.text = model.ticker
                lblAvailableFunds.text = model.balance
                amountTextField.text = model.amount
            }
        }
        if model.inputType == .sweep || model.isSendAll == true {
            amountTextField.text = model.amount
        }

        /*if let satoshi = getSatoshi(), model.txError.isEmpty {
            if model.isBtc,
                let balance = Balance.fromSatoshi(satoshi) {
                lblAmountExchange.isHidden = false
                if model.isFiat {
                    let (fiat, fiatCurrency) = balance.toValue()
                    lblAmountExchange.text = "≈ \(fiat) \(fiatCurrency)"
                } else {
                    let (fiat, fiatCurrency) = balance.toFiat()
                    lblAmountExchange.text = "≈ \(fiat) \(fiatCurrency)"
                }
            }
        }*/
    }

    @objc func triggerTextChange() {
        model?.amount = amountTextField.text
        model?.address = addressTextView.text
        updateModel?(model)
        delegate?.validateTx()
    }

    @IBAction func recipientRemove(_ sender: Any) {
        if let i = index {
            delegate?.removeRecipient(i)
        }
    }

    @IBAction func btnCancelAddress(_ sender: Any) {
        model?.address = nil
        reload()
        updateModel?(model)
        delegate?.validateTx()
    }

    @IBAction func btnPasteAddress(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            model?.address = txt
            reload()
            updateModel?(model)
            delegate?.validateTx()
        }
    }

    @IBAction func btnQr(_ sender: Any) {
        if let i = index {
            delegate?.qrScan(i)
        }
    }

    @IBAction func btnChooseAsset(_ sender: Any) {
        if let i = index {
            delegate?.chooseAsset(i)
        }
    }

    @IBAction func btnSendAll(_ sender: Any) {
        model?.isSendAll.toggle()
        model?.amount = nil
        reload()
        updateModel?(model)
        delegate?.tapSendAll()
        delegate?.validateTx()
    }

    @IBAction func btnCancelAmount(_ sender: Any) {
        model?.amount = nil
        reload()
        updateModel?(model)
        delegate?.validateTx()
    }

    @IBAction func btnPasteAmount(_ sender: Any) {
        if let txt = UIPasteboard.general.string {
            model?.amount = txt
            reload()
            updateModel?(model)
            delegate?.validateTx()
        }
    }

    @IBAction func btnConvert(_ sender: Any) {
        model?.isFiat.toggle()
        reload()
        updateModel?(model)
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
