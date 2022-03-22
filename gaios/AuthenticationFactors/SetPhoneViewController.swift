import Foundation
import UIKit
import PromiseKit

class SetPhoneViewController: KeyboardViewController {

    @IBOutlet weak var headerTitle: UILabel!
    @IBOutlet weak var countryCodeField: UITextField!
    @IBOutlet weak var textField: UITextField!
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var buttonConstraint: NSLayoutConstraint!

    var sms = false
    var phoneCall = false
    private var connected = true
    private var updateToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()
        headerTitle.text = NSLocalizedString("id_enter_phone_number", comment: "")
        countryCodeField.becomeFirstResponder()
        countryCodeField.attributedPlaceholder = NSAttributedString(string: "+1", attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
        textField.attributedPlaceholder = NSAttributedString(string: "123456789", attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
        nextButton.setTitle(NSLocalizedString("id_get_code", comment: ""), for: .normal)
        nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        nextButton.setStyle(.primary)
        textField.layer.cornerRadius = 5.0
        countryCodeField.layer.cornerRadius = 5.0
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: updateConnection)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = updateToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    override func keyboardWillShow(notification: Notification) {
        let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
        nextButton.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height).isActive = true
    }

    func updateConnection(_ notification: Notification) {
        let connected = notification.userInfo?["connected"] as? Bool
        self.connected = connected ?? false
    }

    @objc func click(_ sender: UIButton) {
        let bgq = DispatchQueue.global(qos: .background)
        let method = self.sms == true ? TwoFactorType.sms : TwoFactorType.phone
        guard let countryCode = countryCodeField.text, let phone = textField.text else { return }
        if countryCode.isEmpty || phone.isEmpty {
            DropAlert().warning(message: NSLocalizedString("id_invalid_phone_number_format", comment: ""))
            return
        }
        guard let session = SessionsManager.current else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap {
            TwoFactorConfigItem(enabled: true, confirmed: true, data: countryCode + phone)
        }.compactMap(on: bgq) { config in
            try JSONSerialization.jsonObject(with: JSONEncoder().encode(config), options: .allowFragments) as? [String: Any]
        }.compactMap(on: bgq) { details in
            try session.changeSettingsTwoFactor(method: method.rawValue, details: details)
        }.then(on: bgq) { call in
            call.resolve(connected: { self.connected })
        }.then(on: bgq) { _ in
            session.loadTwoFactorConfig()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.navigationController?.popViewController(animated: true)
        }.catch { error in
            if let twofaError = error as? TwoFactorCallError {
                switch twofaError {
                case .failure(let localizedDescription), .cancel(let localizedDescription):
                    DropAlert().error(message: localizedDescription)
                }
            } else {
                DropAlert().error(message: error.localizedDescription)
            }
        }
    }
}
