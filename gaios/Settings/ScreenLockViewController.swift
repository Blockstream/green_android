import NVActivityIndicatorView
import PromiseKit
import UIKit

class ScreenLockViewController: UIViewController {

    @IBOutlet var content: ScreenLockView!
    var network = { return getNetwork() }()

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_screen_lock", comment: "")
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
        content.pinSwitch.addTarget(self, action: #selector(click(_:)), for: .valueChanged)
        updateValues()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(true)
        content.bioSwitch.removeTarget(self, action: #selector(click(_:)), for: .valueChanged)
        content.pinSwitch.removeTarget(self, action: #selector(click(_:)), for: .valueChanged)
        updateValues()
    }

    func updateValues() {
        content.helpLabel.text = ""
        let screenlock = getGAService().getSettings()!.getScreenLock()
        if GreenAddressService.isTemporary {
            content.bioSwitch.isOn = false
            content.bioSwitch.isEnabled = false
            content.pinSwitch.isOn = false
            content.pinSwitch.isEnabled = false
            content.helpLabel.numberOfLines = 0
            content.helpLabel.text = NSLocalizedString("id_green_only_supports_one_pin_per", comment: "")
        } else if screenlock == .None {
            content.bioSwitch.isOn = false
            content.pinSwitch.isOn = false
        } else if screenlock == .All {
            content.bioSwitch.isOn = true
            content.pinSwitch.isOn = true
        } else if screenlock == .FaceID || screenlock == .TouchID {
            // this should never happen
            NSLog("no pin exists but faceid/touchid is enabled" )
            content.bioSwitch.isOn = true
            content.pinSwitch.isOn = false
        } else if screenlock == .Pin {
            content.bioSwitch.isOn = false
            content.pinSwitch.isOn = true
        }
    }

    func onAuthRemoval(_ sender: UISwitch, _ completionHandler: @escaping () -> Void) {
        let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: NSLocalizedString(sender == content.pinSwitch ? "id_deleting_your_pin_will_remove" : "id_your_pin_or_your_mnemonic_will", comment: ""), preferredStyle: .alert)
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
            try? AuthenticationTypeHandler.removePrivateKey(forNetwork: self.network)
            UserDefaults.standard.set(nil, forKey: "AuthKeyBiometricPrivateKey" + self.network)
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

    private func enablePinAuth() {
        self.performSegue(withIdentifier: "restorePin", sender: nil)
    }

    private func enableBioAuth() {
        // An auth key pin should be set before updating bio auth
        if !AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: self.network) {
            onAuthError(message: NSLocalizedString("id_please_enable_pin", comment: ""))
            return
        }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            startAnimating()
            return Guarantee()
        }.map(on: bgq) {
            if UserDefaults.standard.string(forKey: "AuthKeyBiometricPrivateKey" + self.network) == nil {
                try AuthenticationTypeHandler.generateBiometricPrivateKey(network: self.network)
            }
        }.compactMap(on: bgq) {
            let password = String.random(length: 14)
            let deviceid = String.random(length: 14)
            let mnemonics = try getSession().getMnemonicPassphrase(password: "")
            return (try getSession().setPin(mnemonic: mnemonics, pin: password, device: deviceid), password) as? ([String: Any], String)
        }.map { (data: [String: Any], password: String) -> Void in
            try AuthenticationTypeHandler.addBiometryType(data: data, extraData: password, forNetwork: self.network)
        }.ensure {
            self.stopAnimating()
        }.catch { error in
            if let err = error as? GaError, err != GaError.GenericError {
                self.onAuthError(message: NSLocalizedString("id_you_are_not_connected_to_the", comment: ""))
            } else if let err = error as? AuthenticationTypeHandler.AuthError {
                self.onBioAuthError(message: err.localizedDescription)
            } else if !error.localizedDescription.isEmpty {
                self.onAuthError(message: NSLocalizedString(error.localizedDescription, comment: ""))
            } else {
                self.onAuthError(message: NSLocalizedString("id_operation_failure", comment: ""))
            }
        }
    }

    private func disablePinAuth() {
        // Disable auth key Bio before removing auth key Pin
        if AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: self.network) {
            onAuthError(message: NSLocalizedString("id_please_disable_biometric", comment: ""))
            return
        }
        onAuthRemoval(self.content.pinSwitch) {
            removePinKeychainData()
        }
    }

    private func disableBioAuth() {
        onAuthRemoval(self.content.bioSwitch) {
            removeBioKeychainData()
        }
    }

    @objc func click(_ sender: UISwitch) {
        if sender == self.content.pinSwitch, sender.isOn {
            enablePinAuth()
        } else if sender == self.content.pinSwitch, !sender.isOn {
            disablePinAuth()
        } else if sender == self.content.bioSwitch, sender.isOn {
            enableBioAuth()
        } else {
            disableBioAuth()
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let nextController = segue.destination as? PinSetViewController {
            nextController.mode = .edit
        }
    }
}

@IBDesignable
class ScreenLockView: UIView {

    @IBOutlet weak var bioAuthLabel: UILabel!
    @IBOutlet weak var helpLabel: UILabel!
    @IBOutlet weak var bioSwitch: UISwitch!
    @IBOutlet weak var pinSwitch: UISwitch!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }
}
