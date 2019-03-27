import NVActivityIndicatorView
import PromiseKit
import UIKit

class ScreenLockViewController: UIViewController {

    @IBOutlet var content: ScreenLockView!

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
        guard let settings = getGAService().getSettings() else {
            return
        }
        content.helpLabel.text = ""
        let screenlock = settings.getScreenLock()
        if GreenAddressService.restoreFromMnemonics && isPinEnabled(network: getNetwork()) {
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
                sender.setOn(true, animated: true)
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

    func verifyAuth(message: String, _ sender: UISwitch) {
        let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_ok", comment: ""), style: .default) { _ in })
        DispatchQueue.main.async {
            self.present(alert, animated: true, completion: nil)
            sender.setOn(!sender.isOn, animated: true)
        }
    }

    @objc func click(_ sender: UISwitch) {
        if sender == content.bioSwitch {
            if !AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: getNetwork()) {
                verifyAuth(message: NSLocalizedString("id_please_enable_pin", comment: ""), sender)
            } else if !sender.isOn {
                onAuthRemoval(sender) {
                    removeBioKeychainData()
                }
            } else {
                let bgq = DispatchQueue.global(qos: .background)
                firstly {
                    startAnimating()
                    return Guarantee()
                }.map(on: bgq) {
                    let network = getNetwork()
                    if UserDefaults.standard.string(forKey: "AuthKeyBiometricPrivateKey" + network) == nil {
                        _ = AuthenticationTypeHandler.generateBiometricPrivateKey(network: network)
                    }
                }.compactMap(on: bgq) {
                    let password = String.random(length: 14)
                    let deviceid = String.random(length: 14)
                    let mnemonics = try getSession().getMnemonicPassphrase(password: "")
                    return (try getSession().setPin(mnemonic: mnemonics, pin: password, device: deviceid), password) as? ([String : Any], String)
                }.done { (data: [String: Any], password: String) -> Void in
                    try AuthenticationTypeHandler.addBiometryType(data: data, extraData: password, forNetwork: getNetwork())
                }.catch { _ in
                }.finally {
                    self.stopAnimating()
                }
            }
        } else if sender == content.pinSwitch {
            if sender.isOn {
                self.performSegue(withIdentifier: "restorePin", sender: nil)
            } else {
                if AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: getNetwork()) {
                    verifyAuth(message: NSLocalizedString("id_please_disable_biometric", comment: ""), sender)
                } else {
                    onAuthRemoval(sender) {
                        removePinKeychainData()
                    }
                }
            }
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let nextController = segue.destination as? PinLoginViewController {
            nextController.editPinMode = true
            nextController.setPinMode = true
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
