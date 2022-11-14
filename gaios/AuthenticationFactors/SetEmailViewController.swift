import Foundation
import UIKit
import PromiseKit

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
        textField.becomeFirstResponder()
        textField.attributedPlaceholder = NSAttributedString(string: "email@domain.com",
                                                             attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
        nextButton.setTitle(NSLocalizedString("id_get_code", comment: ""), for: .normal)
        nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        nextButton.setStyle(.primary)
        setRecoveryLabel.text = isSetRecovery ?
            NSLocalizedString("id_set_up_an_email_to_get", comment: "") :
            NSLocalizedString("id_the_email_will_also_be_used_to", comment: "")
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
        if let data = notification.userInfo,
              let json = try? JSONSerialization.data(withJSONObject: data, options: []),
              let connection = try? JSONDecoder().decode(Connection.self, from: json) {
            self.connected = connection.connected
        }
    }

    @objc func click(_ sender: UIButton) {
        let bgq = DispatchQueue.global(qos: .background)
        guard let text = textField.text else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap {
            TwoFactorConfigItem(enabled: self.isSetRecovery ? false : true, confirmed: true, data: text)
        }.then(on: bgq) { config in
            self.session.changeSettingsTwoFactor(method: .email, config: config)
        }.then(on: bgq) { _ in
            self.session.loadTwoFactorConfig()
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
