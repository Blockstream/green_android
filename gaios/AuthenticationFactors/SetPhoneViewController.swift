import Foundation
import UIKit
import NVActivityIndicatorView
import PromiseKit

class SetPhoneViewController: KeyboardViewController {

    @IBOutlet var content: SetPhoneView!
    var sms = false
    var phoneCall = false

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_enter_phone_number", comment: "")
        content.countryCodeField.becomeFirstResponder()
        content.countryCodeField.attributedPlaceholder = NSAttributedString(string: "+1", attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
        content.textField.attributedPlaceholder = NSAttributedString(string: "123456789", attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
        content.nextButton.setTitle(NSLocalizedString("id_get_code", comment: ""), for: .normal)
        content.nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.nextButton.setGradient(true)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        content.nextButton.updateGradientLayerFrame()
    }

    override func keyboardWillShow(notification: NSNotification) {
        let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
        content.nextButton.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height).isActive = true
    }

    @objc func click(_ sender: UIButton) {
        let bgq = DispatchQueue.global(qos: .background)
        let method = self.sms == true ? TwoFactorType.sms : TwoFactorType.phone
        guard let countryCode = content.countryCodeField.text, let phone = content.textField.text else { return }
        if countryCode.isEmpty || phone.isEmpty {
            Toast.show(NSLocalizedString("id_invalid_phone_number_format", comment: ""))
            return
        }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap {
            TwoFactorConfigItem(enabled: true, confirmed: true, data: countryCode + phone)
        }.compactMap(on: bgq) { config in
            try JSONSerialization.jsonObject(with: JSONEncoder().encode(config), options: .allowFragments) as? [String: Any]
        }.compactMap(on: bgq) { details in
            try getGAService().getSession().changeSettingsTwoFactor(method: method.rawValue, details: details)
        }.then(on: bgq) { call in
            call.resolve(self)
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            getGAService().reloadTwoFactor()
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
}

@IBDesignable
class SetPhoneView: UIView {
    @IBOutlet weak var countryCodeField: UITextField!
    @IBOutlet weak var textField: UITextField!
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var buttonConstraint: NSLayoutConstraint!

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
