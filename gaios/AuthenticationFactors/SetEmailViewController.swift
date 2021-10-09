import Foundation
import UIKit
import PromiseKit

class SetEmailViewController: KeyboardViewController {

    @IBOutlet var content: SetEmailView!
    private var connected = true
    private var updateToken: NSObjectProtocol?

    var isSetRecovery: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()
        content.headerTitle.text = NSLocalizedString("id_enter_your_email_address", comment: "")
        content.textField.becomeFirstResponder()
        content.textField.attributedPlaceholder = NSAttributedString(string: "email@domain.com",
                                                             attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
        content.nextButton.setTitle(NSLocalizedString("id_get_code", comment: ""), for: .normal)
        content.nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.nextButton.setGradient(true)
        content.setRecoveryLabel.text = isSetRecovery ?
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
        content.nextButton.bottomAnchor.constraint(equalTo: self.view.bottomAnchor, constant: -keyboardFrame.height).isActive = true
    }

    func updateConnection(_ notification: Notification) {
        let connected = notification.userInfo?["connected"] as? Bool
        self.connected = connected ?? false
    }

    @objc func click(_ sender: UIButton) {
        let bgq = DispatchQueue.global(qos: .background)
        guard let text = content.textField.text else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap {
            TwoFactorConfigItem(enabled: self.isSetRecovery ? false : true, confirmed: true, data: text)
        }.compactMap(on: bgq) { config in
            try JSONSerialization.jsonObject(with: JSONEncoder().encode(config), options: .allowFragments) as? [String: Any]
        }.compactMap(on: bgq) { details in
            try SessionManager.shared.changeSettingsTwoFactor(method: TwoFactorType.email.rawValue, details: details)
        }.then(on: bgq) { call in
            call.resolve(connected: { self.connected })
        }.then(on: bgq) { _ in
            SessionManager.shared.loadTwoFactorConfig()
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

@IBDesignable
class SetEmailView: UIView {
    @IBOutlet weak var textField: UITextField!
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var buttonConstraint: NSLayoutConstraint!
    @IBOutlet weak var headerTitle: UILabel!
    @IBOutlet weak var setRecoveryLabel: UILabel!

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
