import Foundation
import UIKit
import NVActivityIndicatorView
import PromiseKit

class TwoFactorLimitViewController: KeyboardViewController {

    @IBOutlet var content: TwoFactorLimitView!
    fileprivate var isFiat = false
    fileprivate var limits: TwoFactorConfigLimits!

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_twofactor_threshold", comment: "")
        content.nextButton.setTitle(NSLocalizedString("id_set_twofactor_threshold", comment: ""), for: .normal)
        content.nextButton.addTarget(self, action: #selector(nextClick), for: .touchUpInside)
        content.fiatButton.addTarget(self, action: #selector(currencySwitchClick), for: .touchUpInside)
        content.nextButton.setGradient(true)
        content.limitTextField.becomeFirstResponder()
        content.limitTextField.attributedPlaceholder = NSAttributedString(string: "0.00",
                                                                  attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
        content.limitTextField.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        let dataTwoFactorConfig = try? getSession().getTwoFactorConfig()
        guard dataTwoFactorConfig != nil else { return }
        guard let twoFactorConfig = try? JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig!, options: [])) else { return }
        guard let settings = getGAService().getSettings() else { return }
        limits = twoFactorConfig.limits
        isFiat = limits.isFiat
        let amount = isFiat ? limits.fiat : limits.get(TwoFactorConfigLimits.CodingKeys(rawValue: settings.denomination.rawValue)!)!
        let subtitle = isFiat ? String(format: "%@ %@", amount, settings.getCurrency()) : String(format: "%@ %@", amount, settings.denomination.toString())
        content.limitTextField.text = amount
        content.descriptionLabel.text = String(format: NSLocalizedString("id_your_twofactor_threshold_is_s", comment: ""), subtitle)
        refresh()
    }

    override func keyboardWillShow(notification: NSNotification) {
        let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
        content.nextButton.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height).isActive = true
    }

    func refresh() {
        let balance = Balance.convert(details: ["satoshi": getSatoshi()])
        let (amount, denom) = balance.get(tag: (isFiat ? "btc"  : "fiat"))
        content.convertedLabel.text = "≈ \(amount) \(denom)"
        content.fiatButton.setTitle(denom, for: UIControl.State.normal)
        content.fiatButton.backgroundColor = isFiat ? UIColor.clear : UIColor.customMatrixGreen()
    }

    @objc func currencySwitchClick(_ sender: UIButton) {
        let balance = Balance.convert(details: ["satoshi": getSatoshi()])
        let (amount, _) = balance.get(tag: (isFiat ? "btc"  : "fiat"))
        content.limitTextField.text = "≈ \(amount)"
        isFiat = !isFiat
        refresh()
    }

    @objc func nextClick(_ sender: UIButton) {
        guard let amountText = content.limitTextField.text else { return }
        guard let amount = Double(amountText.replacingOccurrences(of: ",", with: ".")) else { return }
        guard let settings = getGAService().getSettings() else { return }
        let details: [String: Any]
        if isFiat {
            details = ["is_fiat": isFiat, "fiat": String(amount)]
        } else {
            let denomination: String = settings.denomination.rawValue
            details = ["is_fiat": isFiat, denomination: String(amount)]
        }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try getSession().setTwoFactorLimit(details: details)
        }.then(on: bgq) { call in
            call.resolve(self)
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.navigationController?.popViewController(animated: true)
        }.catch { error in
            if let twofaError = error as? TwoFactorCallError {
                switch twofaError {
                case .failure(let localizedDescription), .cancel(let localizedDescription):
                    Toast.show(localizedDescription)
                }
            } else {
                Toast.show(error.localizedDescription)
            }
        }
    }

    @objc func textFieldDidChange(_ textField: UITextField) {
        refresh()
    }

    func getSatoshi() -> UInt64 {
        guard let text = content.limitTextField.text else { return 0 }
        let amount = text.replacingOccurrences(of: ",", with: ".")
        if Double(amount) == nil { return 0 }
        return Balance.convert(details: [(isFiat ? "fiat" : "btc"): amount]).satoshi
    }
}

@IBDesignable
class TwoFactorLimitView: UIView {
    @IBOutlet weak var limitTextField: UITextField!
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var fiatButton: UIButton!
    @IBOutlet weak var descriptionLabel: UILabel!
    @IBOutlet weak var convertedLabel: UILabel!
    @IBOutlet weak var limitButtonConstraint: NSLayoutConstraint!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        nextButton.updateGradientLayerFrame()
    }
}
