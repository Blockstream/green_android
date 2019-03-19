import Foundation
import UIKit
import NVActivityIndicatorView
import PromiseKit

class SetEmailViewController: KeyboardViewController {

    @IBOutlet var content: SetEmailView!

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_enter_your_email_address", comment: "")
        content.textField.becomeFirstResponder()
        content.textField.attributedPlaceholder = NSAttributedString(string: "email@domain.com",
                                                             attributes: [NSAttributedStringKey.foregroundColor: UIColor.customTitaniumLight()])
        content.nextButton.setTitle(NSLocalizedString("id_get_code", comment: ""), for: .normal)
        content.nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.nextButton.setGradient(true)
    }

    override func keyboardWillShow(notification: NSNotification) {
        let userInfo = notification.userInfo! as NSDictionary
        let keyboardFrame = userInfo.value(forKey: UIKeyboardFrameEndUserInfoKey) as! NSValue
        content.nextButton.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.cgRectValue.height).isActive = true
    }

    @objc func click(_ sender: UIButton) {
        let bgq = DispatchQueue.global(qos: .background)
        guard let text = content.textField.text else { return }
        let config = TwoFactorConfigItem(enabled: true, confirmed: true, data: text)
        firstly {
            startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try JSONSerialization.jsonObject(with: JSONEncoder().encode(config), options: .allowFragments) as? [String: Any]
        }.compactMap(on: bgq) { data in
            try getGAService().getSession().changeSettingsTwoFactor(method: TwoFactorType.email.rawValue, details: data)
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
class SetEmailView: UIView {
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
