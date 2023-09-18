import Foundation
import UIKit

import gdk

class SetEmailViewController: KeyboardViewController {

    @IBOutlet weak var headerTitle: UILabel!
    @IBOutlet weak var setRecoveryLabel: UILabel!
    @IBOutlet weak var textField: UITextField!
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var buttonConstraint: NSLayoutConstraint!

    private var connected = true
    private var updateToken: NSObjectProtocol?

    var isSetRecovery: Bool = false
    var session: SessionManager!

    override func viewDidLoad() {
        super.viewDidLoad()
        headerTitle.text = NSLocalizedString("id_enter_your_email_address", comment: "")
        textField.attributedPlaceholder = NSAttributedString(string: "email@domain.com",
                                                             attributes: [NSAttributedString.Key.foregroundColor: UIColor.white.withAlphaComponent(0.6)])
        textField.setLeftPaddingPoints(10.0)
        textField.setRightPaddingPoints(10.0)
        nextButton.setTitle(NSLocalizedString("id_get_code", comment: ""), for: .normal)
        nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        nextButton.setStyle(.primaryDisabled)
        setRecoveryLabel.text = isSetRecovery ?
            NSLocalizedString("id_set_up_an_email_to_get", comment: "") :
            NSLocalizedString("id_the_email_will_also_be_used_to", comment: "")
        headerTitle.font = UIFont.systemFont(ofSize: 24.0, weight: .bold)
        setRecoveryLabel.font = UIFont.systemFont(ofSize: 12.0, weight: .regular)
        setRecoveryLabel.textColor = .white.withAlphaComponent(0.6)
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

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        textField.becomeFirstResponder()
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

    @objc func click(_ sender: UIButton) {
        guard let text = textField.text else { return }
        view.endEditing(true)
        self.startAnimating()
        Task {
            do {
                let config = TwoFactorConfigItem(enabled: self.isSetRecovery ? false : true, confirmed: true, data: text)
                try await session.changeSettingsTwoFactor(method: .email, config: config)
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

    @IBAction func editingChanged(_ sender: Any) {
        (textField.text ?? "").isValidEmailAddr() ? nextButton.setStyle(.primary) : nextButton.setStyle(.primaryDisabled)
    }
}
