import UIKit
import gdk

enum LTAmountCellState: Int {
    case valid
    case validFunding
    case tooHigh
    case tooLow
    case disabled
    case invalidAmount
    case disconnected
}

protocol LTAmountCellDelegate: AnyObject {
    func textFieldDidChange(_ satoshi: Int64?, isFiat: Bool)
    func textFieldEnabled()
    func onFeeInfo()
    func onInputDenomination()
    func stateDidChange(_ state: LTAmountCellState)
}

class LTAmountCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var textField: UITextField!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblInfo: UILabel!
    @IBOutlet weak var infoPanel: UIView!

    @IBOutlet weak var lblMoreInfo: UILabel!
    @IBOutlet weak var btnCancel: UIButton!
    @IBOutlet weak var btnPaste: UIButton!
    @IBOutlet weak var btnSwitch: UIButton!
    @IBOutlet weak var btnEdit: UIButton!
    @IBOutlet weak var btnFeeInfo: UIButton!
    @IBOutlet weak var lblToReceiveTitle: UILabel!
    @IBOutlet weak var lblToReceiveHint: UILabel!
    
    var state: LTAmountCellState = .valid
    weak var delegate: LTAmountCellDelegate?
    var model: LTAmountCellModel!
    var enabled: Bool = true

    static var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        bg.borderWidth = 1.0
        infoPanel.cornerRadius = 5.0
        lblAmount.setStyle(.txtCard)
        lblInfo.setStyle(.txtCard)
        lblMoreInfo.setStyle(.txtCard)
        lblAsset.setStyle(.txtBigger)
        lblMoreInfo.text = "For more information,".localized
        btnFeeInfo.setTitle("read more".localized, for: .normal)
        btnFeeInfo.setStyle(.inlineGray)
        [lblToReceiveTitle, lblToReceiveHint].forEach {
            $0?.setStyle(.sectionTitle)
        }
        lblToReceiveTitle.text = "id_amount_to_receive".localized
        lblToReceiveHint.text = ""
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: LTAmountCellModel, delegate: LTAmountCellDelegate?, enabled: Bool) {
        self.delegate = delegate
        self.model = model
        self.enabled = enabled
        textField.text = model.amountText
        lblAsset.attributedText = model.denomUnderlineText
        textField.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        if enabled {
//            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.5) {
//                self.textField.becomeFirstResponder()
//            }
        } else {
            state = .disabled
        }
        reload()
    }

    func reload() {
        textField.isEnabled = enabled
        btnEdit.isHidden = enabled
        btnCancel.isHidden = !enabled
        btnPaste.isHidden = !enabled
        btnCancel.isHidden = !(textField.text?.count ?? 0 > 0)
        btnPaste.isHidden = textField.text?.count ?? 0 > 0
        let balance = "\(model?.maxLimitAmount ?? "") \(model?.denomText ?? "")"
        lblAmount.text = String(format: "id_max_limit_s".localized, balance)
        lblAsset.attributedText = model?.denomUnderlineText
        updateState()
    }

    func toReceiveAmount(show: Bool) {
        [lblToReceiveTitle, lblToReceiveHint].forEach{
            $0?.isHidden = !show
        }
    }

    @objc func triggerTextChange() {
        if let value = textField.text {
            if model.isFiat {
                let balance = Balance.fromFiat(value)
                model.satoshi = balance?.satoshi
            } else {
                let balance = Balance.fromDenomination(value, assetId: AssetInfo.btcId, denomination: model.inputDenomination)
                model.satoshi = balance?.satoshi
            }
            reload()
        }
        delegate?.textFieldDidChange(model.satoshi, isFiat: model.isFiat)
        delegate?.stateDidChange(model.state)
    }
    
    @objc func textFieldDidChange(_ textField: UITextField) {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.triggerTextChange), object: nil)
        perform(#selector(self.triggerTextChange), with: nil, afterDelay: 0.5)
    }

    @IBAction func onEdit(_ sender: Any) {
        enabled = true
        reload()
        textField.becomeFirstResponder()
        delegate?.textFieldEnabled()
    }

    @IBAction func onSwitch(_ sender: Any) {
        delegate?.onInputDenomination()
    }

    @IBAction func btnPaste(_ sender: Any) {
        if let text = UIPasteboard.general.string {
            textField.text = text
            textFieldDidChange(textField)
        }
    }

    @IBAction func btnCancel(_ sender: Any) {
        textField.text = ""
        textFieldDidChange(textField)
    }
    
    func errorState(text: String) {
        bg.borderColor = UIColor.gRedFluo()
        infoPanel.backgroundColor = UIColor.gRedFluo().withAlphaComponent(0.2)
        lblInfo.text = text
        lblInfo.isHidden = false
        btnFeeInfo.isHidden = false
        lblMoreInfo.isHidden = false
        lblAmount.isHidden = true
        toReceiveAmount(show: false)
    }

    func disableState() {
        bg.borderColor = UIColor.gBlackBg()
        infoPanel.backgroundColor = UIColor.clear
        //lblInfo.isHidden = true
        lblInfo.text = " "
        btnFeeInfo.isHidden = true
        lblMoreInfo.isHidden = true
        lblAmount.isHidden = false
        toReceiveAmount(show: false)
    }

    func updateState() {
        switch model.state {
        case .invalidAmount:
            let text = "id_invalid_amount".localized
            errorState(text: text)
        case .valid:
            disableState()
            lblAmount.isHidden = false
        case .validFunding:
            bg.borderColor = UIColor.gGreenFluo()
            infoPanel.backgroundColor = UIColor.gGreenFluo().withAlphaComponent(0.2)
            let amount = model.openChannelFee
            lblInfo.text = String(format: "id_a_set_up_funding_fee_of_s_s".localized, model.toBtcText(amount) ?? "", model.toFiatText(amount) ?? "")
            lblInfo.isHidden = false
            btnFeeInfo.isHidden = false
            lblMoreInfo.isHidden = false
            lblAmount.isHidden = true
            toReceiveAmount(show: true)
            lblToReceiveHint.text = model.toReceiveAmountStr
        case .tooHigh:
            let amount = Int64(model.nodeState?.maxReceivableSatoshi ?? 0)
            let text = String(format: "id_you_cannot_receive_more_than_s".localized, model.toBtcText(amount) ?? "", model.toFiatText(amount) ?? "")
            errorState(text: text)
        case .tooLow:
            let amount = model.openChannelFee
            let text = String(format: "id_this_amount_is_below_the".localized, model.toBtcText(amount) ?? "", model.toFiatText(amount) ?? "")
            errorState(text: text)
        case .disabled:
            disableState()
        case .disconnected:
            let text = "No LSP connected".localized
            errorState(text: text)
        }
    }

    @IBAction func btnFeeInfo(_ sender: Any) {
        delegate?.onFeeInfo()
    }

    @IBAction func btnInputDenomination(_ sender: Any) {
        delegate?.onInputDenomination()
    }
}
