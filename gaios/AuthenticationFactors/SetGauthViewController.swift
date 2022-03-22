import Foundation
import UIKit
import PromiseKit

class SetGauthViewController: UIViewController {

    @IBOutlet weak var subtitleLabel: UILabel!
    @IBOutlet weak var qrCodeImageView: UIImageView!
    @IBOutlet weak var warningLabel: UILabel!
    @IBOutlet weak var secretLabel: UILabel!
    @IBOutlet weak var copyImage: UIImageView!
    @IBOutlet weak var nextButton: UIButton!

    private var gauthData: String?
    private var connected = true
    private var updateToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_authenticator_qr_code", comment: "")

        let dataTwoFactorConfig = try? SessionsManager.current.getTwoFactorConfig()
        guard dataTwoFactorConfig != nil else { return }
        guard let twoFactorConfig = try? JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig!, options: [])) else { return }
        gauthData = twoFactorConfig.gauth.data
        guard let secret = twoFactorConfig.gauthSecret() else {
            DropAlert().error(message: NSLocalizedString("id_operation_failure", comment: ""))
            return
        }
        secretLabel.text = secret
        qrCodeImageView.image = QRImageGenerator.imageForTextWhite(text: gauthData!, frame: qrCodeImageView.frame)
        nextButton.setTitle(NSLocalizedString("id_get_code", comment: ""), for: .normal)
        subtitleLabel.text = NSLocalizedString("id_scan_the_qr_code_with_an", comment: "")
        warningLabel.text = NSLocalizedString("id_the_recovery_key_below_will_not", comment: "")
        secretLabel.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard)))
        copyImage.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard)))
        secretLabel.isUserInteractionEnabled = true
        copyImage.isUserInteractionEnabled = true
        nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        nextButton.setStyle(.primary)
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

    @objc func copyToClipboard(_ sender: UIButton) {
        UIPasteboard.general.string = secretLabel.text
        DropAlert().info(message: NSLocalizedString("id_copy_to_clipboard", comment: ""))
    }

    func updateConnection(_ notification: Notification) {
        let connected = notification.userInfo?["connected"] as? Bool
        self.connected = connected ?? false
    }

    @objc func click(_ sender: UIButton) {
        guard let gauth = gauthData else { return }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap {
            TwoFactorConfigItem(enabled: true, confirmed: true, data: gauth)
        }.compactMap(on: bgq) { config in
            try JSONSerialization.jsonObject(with: JSONEncoder().encode(config), options: .allowFragments) as? [String: Any]
        }.compactMap(on: bgq) { details in
            try SessionsManager.current.changeSettingsTwoFactor(method: TwoFactorType.gauth.rawValue, details: details)
        }.then(on: bgq) { call in
            call.resolve(connected: { self.connected })
        }.then(on: bgq) { _ in
            SessionsManager.current.loadTwoFactorConfig()
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
