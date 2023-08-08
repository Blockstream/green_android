import UIKit
import gdk

enum LTAmountCellState: Int {
    case valid
    case invalid
    case disabled
}

protocol LTAmountCellDelegate: AnyObject {
    func textFieldDidChange(_ satoshi: Int64?, isFiat: Bool)
    func textFieldEnabled()
    func onFeeInfo()
    func onInputDenomination()
}

class LTAmountCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var textField: UITextField!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblLimit: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblInfo: UILabel!
    @IBOutlet weak var infoPanel: UIView!
    @IBOutlet weak var iconInfoErr: UIImageView!

    @IBOutlet weak var btnCancel: UIButton!
    @IBOutlet weak var btnPaste: UIButton!
    @IBOutlet weak var btnSwitch: UIButton!
    @IBOutlet weak var btnEdit: UIButton!
    @IBOutlet weak var btnFeeInfo: UIButton!

    var state: LTAmountCellState = .valid
    weak var delegate: LTAmountCellDelegate?
    var model: LTAmountCellModel?
    var enabled: Bool = true

    static var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        bg.borderWidth = 1.0
        infoPanel.cornerRadius = 5.0
        [lblAmount, lblLimit].forEach{ $0?.setStyle(.txtCard)}
        lblInfo.setStyle(.txt)
        lblAsset.setStyle(.txtBigger)
        btnFeeInfo.setTitle("id_more_info".localized, for: .normal)
        btnFeeInfo.setStyle(.inline)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: LTAmountCellModel, delegate: LTAmountCellDelegate?, enabled: Bool) {
        self.delegate = delegate
        self.model = model
        self.enabled = enabled
        textField.text = model.amountText
        lblAsset.attributedText = model.denomText
        textField.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        if enabled {
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.5) {
                self.textField.becomeFirstResponder()
            }
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

        if enabled {
            btnCancel.isHidden = !(textField.text?.count ?? 0 > 0)
            btnPaste.isHidden = textField.text?.count ?? 0 > 0
        }

        if model?.isFiat ?? false {
            lblAmount.text = "≈\(model?.amount ?? "") \(model?.denom ?? "")"
        } else {
            lblAmount.text = "≈\(model?.fiat ?? "") \(model?.currency ?? "")"
        }
        lblLimit.text = "Max Limit: \(model?.maxLimitAmount ?? "") \(model?.denom ?? "")"
        lblAsset.attributedText = model?.denomText
        lblAmount.isHidden = lblAmount.text == "≈ "
        updateState()
    }

    @objc func textFieldDidChange(_ textField: UITextField) {
        if let value = textField.text {
            if model?.isFiat ?? false {
                if let balance = Balance.fromFiat(value) {
                    model?.satoshi = balance.satoshi
                } else {
                    model?.satoshi = nil
                }
            } else {
                if let balance = Balance.fromDenomination(value, assetId: AssetInfo.btcId) {
                    model?.satoshi = balance.satoshi
                } else {
                    model?.satoshi = nil
                }
            }
            reload()
        }
        delegate?.textFieldDidChange(model?.satoshi, isFiat: model?.isFiat ?? false)
    }

    @IBAction func onEdit(_ sender: Any) {
        enabled = true
        reload()
        textField.becomeFirstResponder()
        delegate?.textFieldEnabled()
    }

    @IBAction func onSwitch(_ sender: Any) {
        /// not testable yet
        //delegate?.onInputDenomination()
        model?.isFiat.toggle()
        if let value = textField.text {
            if model?.isFiat ?? false {
                if let balance = Balance.fromDenomination(value, assetId: AssetInfo.btcId) {
                    textField.text = balance.toFiat().0
                }
            } else {
                if let balance = Balance.fromFiat(value) {
                    textField.text = (balance.toUnlocaleDenom().0)
                }
            }
        }
        textFieldDidChange(textField)
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

    func updateState() {
        switch model?.state {
        case .valid:
            bg.borderColor = UIColor.gGreenFluo()
            infoPanel.backgroundColor = UIColor.gGreenFluo().withAlphaComponent(0.2)
            lblInfo.text = "A funding fee of \(model?.channelFeePercent ?? 0)% (minimum \(model?.channelMinFee ?? 0)) sats is applied when receiving amounts above your current receive capacity: \(model?.inboundLiquidity ?? 0) sats"
            lblInfo.textColor = .white
            lblInfo.alpha = 0.55
            lblInfo.isHidden = false
            iconInfoErr.isHidden = true
            btnFeeInfo.isHidden = false
        case .invalid:
            bg.borderColor = UIColor.gRedFluo()
            infoPanel.backgroundColor = UIColor.gRedFluo().withAlphaComponent(0.2)
            lblInfo.text = "The amount you requested is above your max limit."
            lblInfo.textColor = UIColor.gRedFluo()
            lblInfo.alpha = 1.0
            lblInfo.isHidden = false
            iconInfoErr.isHidden = false
            btnFeeInfo.isHidden = true
        case .disabled:
            bg.borderColor = UIColor.gBlackBg()
            infoPanel.backgroundColor = UIColor.clear
            //lblInfo.isHidden = true
            lblInfo.text = " "
            lblInfo.textColor = .white
            lblInfo.alpha = 0.55
            iconInfoErr.isHidden = true
            btnFeeInfo.isHidden = true
        case .none:
            break
        }
    }

    @IBAction func btnFeeInfo(_ sender: Any) {
        delegate?.onFeeInfo()
    }

    @IBAction func btnInputDenomination(_ sender: Any) {
        /// not testable yet
        //delegate?.onInputDenomination()
    }
}
