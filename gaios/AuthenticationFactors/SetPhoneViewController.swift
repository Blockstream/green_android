import Foundation
import UIKit

import gdk

class SetPhoneViewController: KeyboardViewController {

    @IBOutlet weak var headerTitle: UILabel!
    @IBOutlet weak var countryCodeField: UITextField!
    @IBOutlet weak var textField: UITextField!
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var buttonConstraint: NSLayoutConstraint!

    var sms = false
    var phoneCall = false
    var session: SessionManager!
    private var connected = true
    private var updateToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()
        headerTitle.text = NSLocalizedString("id_enter_phone_number", comment: "")
        countryCodeField.attributedPlaceholder = NSAttributedString(string: "+1", attributes: [NSAttributedString.Key.foregroundColor: UIColor.white.withAlphaComponent(0.6)])
        textField.attributedPlaceholder = NSAttributedString(string: "123456789", attributes: [NSAttributedString.Key.foregroundColor: UIColor.white.withAlphaComponent(0.6)])
        nextButton.setTitle(NSLocalizedString("id_get_code", comment: ""), for: .normal)
        nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        nextButton.setStyle(.primary)
        textField.layer.cornerRadius = 5.0
        countryCodeField.layer.cornerRadius = 5.0
        headerTitle.font = UIFont.systemFont(ofSize: 24.0, weight: .bold)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: updateConnection)
        countryCodeField.addTarget(self, action: #selector(onTapCountry), for: UIControl.Event.touchDown)

    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        countryCodeField.becomeFirstResponder()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = updateToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    override func keyboardWillShow(notification: Notification) {
        let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
        buttonConstraint.constant = keyboardFrame.height
    }

    func updateConnection(_ notification: Notification) {
        if let data = notification.userInfo,
              let json = try? JSONSerialization.data(withJSONObject: data, options: []),
              let connection = try? JSONDecoder().decode(Connection.self, from: json) {
            self.connected = connection.connected
        }
    }

    @objc func onTapCountry(textField: UITextField) {
        print("country")
    }

    @objc func click(_ sender: UIButton) {
        let method = self.sms == true ? TwoFactorType.sms : TwoFactorType.phone
        guard let countryCode = countryCodeField.text, let phone = textField.text else { return }
        if countryCode.isEmpty || phone.isEmpty {
            DropAlert().warning(message: NSLocalizedString("id_invalid_phone_number_format", comment: ""))
            return
        }
        view.endEditing(true)
        if method == .sms && ["001", "+1"].contains(countryCode) {
            showError("SMS delivery is unreliable in the US due to network operator policy changes. You can try to get your 2FA code via call instead.")
            return
        }
        self.startAnimating()
        Task {
            do {
                let config = TwoFactorConfigItem(enabled: true, confirmed: true, data: countryCode + phone)
                try await session.changeSettingsTwoFactor(method: method, config: config)
                try await session.loadTwoFactorConfig()
                
                self.navigationController?.popViewController(animated: true)
            } catch {
                if let twofaError = error as? TwoFactorCallError {
                    switch twofaError {
                    case .failure(let localizedDescription), .cancel(let localizedDescription):
                        DropAlert().error(message: localizedDescription)
                    }
                } else {
                    DropAlert().error(message: error.localizedDescription)
                }
            }
            self.stopAnimating()
        }
    }
}
