import Foundation
import UIKit
import PromiseKit

class SetGauthViewController: UIViewController {

    @IBOutlet var content: SetGauthView!
    private var gauthData: String?
    private var connected = true
    private var updateToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_authenticator_qr_code", comment: "")

        let dataTwoFactorConfig = try? getSession().getTwoFactorConfig()
        guard dataTwoFactorConfig != nil else { return }
        guard let twoFactorConfig = try? JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig!, options: [])) else { return }
        gauthData = twoFactorConfig.gauth.data
        guard let secret = twoFactorConfig.gauthSecret() else {
            DropAlert().error(message: NSLocalizedString("id_operation_failure", comment: ""))
            return
        }
        content.secretLabel.text = secret
        content.qrCodeImageView.image = QRImageGenerator.imageForTextWhite(text: gauthData!, frame: content.qrCodeImageView.frame)
        content.nextButton.setTitle(NSLocalizedString("id_get_code", comment: ""), for: .normal)
        content.subtitleLabel.text = NSLocalizedString("id_scan_the_qr_code_with_an", comment: "")
        content.warningLabel.text = NSLocalizedString("id_the_recovery_key_below_will_not", comment: "")
        content.secretLabel.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard)))
        content.copyImage.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard)))
        content.secretLabel.isUserInteractionEnabled = true
        content.copyImage.isUserInteractionEnabled = true
        content.nextButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.nextButton.setGradient(true)
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
        UIPasteboard.general.string = content.secretLabel.text
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
            try getGAService().getSession().changeSettingsTwoFactor(method: TwoFactorType.gauth.rawValue, details: details)
        }.then(on: bgq) { call in
            call.resolve(connected: { self.connected })
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            getGAService().reloadTwoFactor()
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
class SetGauthView: UIView {
    @IBOutlet weak var qrCodeImageView: UIImageView!
    @IBOutlet weak var secretLabel: UILabel!
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var subtitleLabel: UILabel!
    @IBOutlet weak var warningLabel: UILabel!
    @IBOutlet weak var copyImage: UIImageView!

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
