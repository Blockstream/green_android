import PromiseKit
import UIKit

class ScreenLockViewController: UIViewController {

    @IBOutlet var content: ScreenLockView!
    var account = { return AccountsManager.shared.current }()

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_screen_lock", comment: "")
        content.pinLabel.text = NSLocalizedString("id_change_pin", comment: "")
        content.pinLabel.isUserInteractionEnabled = true
        content.pinLabel.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(pinLabelTapped)))

        let biometryType = AuthenticationTypeHandler.biometryType
        if biometryType == .faceID {
            content.bioAuthLabel.text = NSLocalizedString("id_face_id", comment: "")
        } else if biometryType == .touchID {
            content.bioAuthLabel.text = NSLocalizedString("id_touch_id", comment: "")
        } else {
            content.bioAuthLabel.text = NSLocalizedString("id_touchface_id_not_available", comment: "")
            content.bioSwitch.isEnabled = false
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(true)
        content.bioSwitch.addTarget(self, action: #selector(click(_:)), for: .valueChanged)
        updateValues()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(true)
        content.bioSwitch.removeTarget(self, action: #selector(click(_:)), for: .valueChanged)
        updateValues()
    }

    @objc func pinLabelTapped(_ recognizer: UITapGestureRecognizer) {
        self.performSegue(withIdentifier: "restorePin", sender: nil)
    }

    func updateValues() {
        content.helpLabel.text = ""
        guard let screenlock = Settings.shared?.getScreenLock() else {
            DropAlert().error(message: NSLocalizedString("id_operation_failure", comment: ""))
            return
        }
        if screenlock == .None {
            content.bioSwitch.isOn = false
        } else if screenlock == .All {
            content.bioSwitch.isOn = true
        } else if screenlock == .FaceID || screenlock == .TouchID {
            // this should never happen
            NSLog("no pin exists but faceid/touchid is enabled" )
            content.bioSwitch.isOn = true
        } else if screenlock == .Pin {
            content.bioSwitch.isOn = false
        }
    }

    func onAuthRemoval(_ sender: UISwitch, _ completionHandler: @escaping () -> Void) {
        let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: NSLocalizedString("id_your_pin_or_your_mnemonic_will", comment: ""), preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in
            DispatchQueue.main.async {
                self.navigationController?.popViewController(animated: true)
            }
        })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_ok", comment: ""), style: .default) { _ in
            DispatchQueue.main.async {
                completionHandler()
            }
        })
        DispatchQueue.main.async {
            self.present(alert, animated: true, completion: nil)
        }
    }

    func onBioAuthError(message: String) {
        let text = String(format: NSLocalizedString("id_snnreset_this_setting_and_then", comment: ""), message)
        let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: text, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .default) { _ in
            self.navigationController?.popViewController(animated: true)
        })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_reset", comment: ""), style: .destructive) { _ in
            removeBioKeychainData()
            try? AuthenticationTypeHandler.removePrivateKey(forNetwork: self.account!.keychain)
            UserDefaults.standard.set(nil, forKey: "AuthKeyBiometricPrivateKey" + self.account!.keychain)
            self.navigationController?.popViewController(animated: true)
        })
        DispatchQueue.main.async {
            self.present(alert, animated: true, completion: nil)
        }
    }

    func onAuthError(message: String) {
        let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .default) { _ in
            self.navigationController?.popViewController(animated: true)
        })
        DispatchQueue.main.async {
            self.present(alert, animated: true, completion: nil)
        }
    }

    private func enableBioAuth() {
        // An auth key pin should be set before updating bio auth
        if !AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: self.account!.keychain) {
            onAuthError(message: NSLocalizedString("id_please_enable_pin", comment: ""))
            return
        }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try self.account?.addBioPin(session: getSession())
        }.ensure {
            self.stopAnimating()
        }.catch { error in
            if let err = error as? GaError, err != GaError.GenericError {
                self.onAuthError(message: NSLocalizedString("id_connection_failed", comment: ""))
            } else if let err = error as? AuthenticationTypeHandler.AuthError {
                self.onBioAuthError(message: err.localizedDescription)
            } else if !error.localizedDescription.isEmpty {
                self.onAuthError(message: NSLocalizedString(error.localizedDescription, comment: ""))
            } else {
                self.onAuthError(message: NSLocalizedString("id_operation_failure", comment: ""))
            }
        }
    }

    private func disableBioAuth() {
        onAuthRemoval(self.content.bioSwitch) {
            self.account?.removeBioKeychainData()
        }
    }

    @objc func click(_ sender: UISwitch) {
        if sender == self.content.bioSwitch, sender.isOn {
            enableBioAuth()
        } else {
            disableBioAuth()
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let nextController = segue.destination as? SetPinViewController {
            nextController.pinFlow = .settings
        }
    }
}

@IBDesignable
class ScreenLockView: UIView {

    @IBOutlet weak var bioAuthLabel: UILabel!
    @IBOutlet weak var helpLabel: UILabel!
    @IBOutlet weak var pinLabel: UILabel!
    @IBOutlet weak var bioSwitch: UISwitch!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }
}
