import Foundation
import UIKit
import NVActivityIndicatorView
import PromiseKit

class PinLoginViewController: UIViewController {

    @IBOutlet var content: PinLoginView!
    var pinCode = String()
    var pinConfirm = String()
    var setPinMode: Bool = false
    var editPinMode: Bool = false
    var restoreMode: Bool = false
    var isLogin: Bool { get { return !(setPinMode || restoreMode || editPinMode) } }

    private var confirmPin: Bool = false
    private let MAXATTEMPTS = 3

    var pinAttemptsPreference: Int {
        get {
            return UserDefaults.standard.integer(forKey: getNetwork() + "_pin_attempts")
        }
        set {
            UserDefaults.standard.set(newValue, forKey: getNetwork() + "_pin_attempts")
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        let network = getNetwork()
        let networkImage = network == "Mainnet".lowercased() ? UIImage(named: "btc") : UIImage(named: "btc_testnet")
        let navigationBarHeight: CGFloat =  navigationController!.navigationBar.frame.height
        let imageView = UIImageView(frame: CGRect(x: 0, y: 0, width: navigationBarHeight, height: navigationBarHeight))
        imageView.contentMode = .scaleAspectFit
        imageView.image = networkImage
        navigationItem.titleView = imageView
        navigationItem.setHidesBackButton(true, animated: false)

        // show title
        if setPinMode == true {
            content.title.text = NSLocalizedString("id_create_a_pin_to_access_your", comment: "")
        } else {
            content.title.text = NSLocalizedString("id_enter_pin", comment: "")
        }
        // set buttons
        content.skipButton.setTitle(NSLocalizedString("id_skip", comment: ""), for: .normal)
        content.cancelButton.setTitle(NSLocalizedString("id_clear", comment: "").uppercased(), for: .normal)
        content.deleteButton.contentMode = .center
        content.deleteButton.imageView?.contentMode = .scaleAspectFill

        // setup keypad button style
        let background = getBackgroundImage(UIColor.customMatrixGreenDark().cgColor)
        content.keyButton?.enumerated().forEach { (_, button) in
            button.setBackgroundImage(background, for: UIControlState.highlighted)
        }

        // show only in edit pin mode
        content.skipButton.isHidden = isLogin
        if isLogin {
            navigationItem.leftBarButtonItem = UIBarButtonItem(image: UIImage.init(named: "backarrow"), style: UIBarButtonItemStyle.plain, target: self, action: #selector(PinLoginViewController.click(sender:)))
        }
    }
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        content.cancelButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        content.deleteButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        content.skipButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        for button in content.keyButton!.enumerated() {
            button.element.addTarget(self, action: #selector(keyClick(sender:)), for: .touchUpInside)
        }
        resetEverything()
        updateView()
        updateAttemptsLabel()
    }

    override func viewDidAppear(_ animated: Bool) {
        if setPinMode || confirmPin {
            return
        }

        let network = getNetwork()
        let bioAuth = AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
        if bioAuth {
            loginWithPin(usingAuth: AuthenticationTypeHandler.AuthKeyBiometric, network: network, withPIN: nil)
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        guard content != nil else { return }
        content.cancelButton.removeTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        content.deleteButton.removeTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        content.skipButton.removeTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        for button in content.keyButton!.enumerated() {
            button.element.removeTarget(self, action: #selector(keyClick(sender:)), for: .touchUpInside)
        }
    }

    fileprivate func loginWithPin(usingAuth: String, network: String, withPIN: String?) {
        let bgq = DispatchQueue.global(qos: .background)
        let appDelegate = getAppDelegate()!
        let isBiometricLogin = usingAuth == AuthenticationTypeHandler.AuthKeyBiometric

        firstly {
            startAnimating(message: !isBiometricLogin ? NSLocalizedString("id_logging_in", comment: "") : "")
            return Guarantee()
        }.compactMap(on: bgq) {
            appDelegate.disconnect()
        }.compactMap(on: bgq) {
            try appDelegate.connect()
        }.get { _ in
            if isBiometricLogin {
                self.stopAnimating()
            }
        }.compactMap(on: bgq) {
            try AuthenticationTypeHandler.getAuth(method: usingAuth, forNetwork: network)
        }.get { _ in
            if isBiometricLogin {
                self.startAnimating(message: NSLocalizedString("id_logging_in", comment: ""))
            }
        }.map(on: bgq) {
            assert(withPIN != nil || isBiometricLogin)

            let jsonData = try JSONSerialization.data(withJSONObject: $0)
            let pin = withPIN ?? $0["plaintext_biometric"] as? String
            let pinData = String(data: jsonData, encoding: .utf8)
            try getSession().loginWithPin(pin: pin!, pin_data: pinData!)
        }.ensure {
            self.stopAnimating()
        }.done {
            GreenAddressService.restoreFromMnemonics = false
            self.pinAttemptsPreference = 0
            appDelegate.instantiateViewControllerAsRoot(identifier: "TabViewController")
        }.catch { error in
            var message = NSLocalizedString("id_login_failed", comment: "")
            if let authError = error as? AuthenticationTypeHandler.AuthError {
                if authError == AuthenticationTypeHandler.AuthError.CanceledByUser {
                    return
                }
            } else if let error = error as? GaError {
                switch error {
                case .NotAuthorizedError:
                    if withPIN != nil {
                        self.pinAttemptsPreference += 1
                        if self.pinAttemptsPreference == self.MAXATTEMPTS {
                            self.stopAnimating()
                            removeKeychainData()
                            self.pinAttemptsPreference = 0
                            appDelegate.instantiateViewControllerAsRoot(identifier: "InitialViewController")
                            return
                        }
                    }
                case .GenericError:
                    break
                default:
                    message = NSLocalizedString("id_you_are_not_connected_to_the", comment: "")
                }
            }
            self.updateAttemptsLabel()
            self.resetEverything()
            Toast.show(message, timeout: Toast.SHORT)
        }
    }

    fileprivate func setPin() {
        let bgq = DispatchQueue.global(qos: .background)

        firstly {
            startAnimating(message: "")
            return Guarantee()
        }.compactMap(on: bgq) {
            let mnemonics = try getSession().getMnemonicPassphrase(password: "")
            return try getSession().setPin(mnemonic: mnemonics, pin: self.pinCode, device: String.random(length: 14))
        }.map(on: bgq) { (data: [String: Any]) -> Void in
            let network = getNetwork()
            try AuthenticationTypeHandler.addPIN(data: data, forNetwork: network)
        }.ensure {
            self.stopAnimating()
        }.done {
            if self.editPinMode {
                self.navigationController?.popViewController(animated: true)
            } else if self.restoreMode {
                getAppDelegate()!.instantiateViewControllerAsRoot(identifier: "TabViewController")
            } else {
                self.performSegue(withIdentifier: "next", sender: self)
            }
        }.catch { error in
            let message: String
            if let err = error as? GaError, err != GaError.GenericError {
                message = NSLocalizedString("id_you_are_not_connected_to_the", comment: "")
            } else {
                message = NSLocalizedString("id_operation_failure", comment: "")
            }
            Toast.show(message, timeout: Toast.SHORT)
        }
    }

    func updateAttemptsLabel() {
        content.attempts.text = String(format: NSLocalizedString("id_attempts_remaining_d", comment: ""), MAXATTEMPTS - pinAttemptsPreference)
        content.attempts.isHidden = pinAttemptsPreference == 0
    }

    func updatePinMismatch() {
        content.attempts.text = NSLocalizedString("id_pins_do_not_match_please_try", comment: "")
        content.attempts.isHidden = false
    }

    @objc func keyClick(sender: UIButton) {
        pinCode += (sender.titleLabel?.text)!
        updateView()
        if pinCode.count < 6 {
            return
        }

        if setPinMode == true {
            if confirmPin == true {
                //set pin
                if pinCode != pinConfirm {
                    content.title.text = NSLocalizedString("id_set_a_new_pin", comment: "")
                    resetEverything()
                    updatePinMismatch()
                    content.skipButton.isHidden = true
                    return
                }
                setPin()
                return
            }
            confirmPin = true
            pinConfirm = pinCode
            pinCode = ""
            updateView()
            updateAttemptsLabel()
            //show confirm pin
            content.title.text = NSLocalizedString("id_verify_your_pin", comment: "")
            content.skipButton.isHidden = true
        } else {
            let network = getNetwork()
            loginWithPin(usingAuth: AuthenticationTypeHandler.AuthKeyPIN, network: network, withPIN: self.pinCode)
        }
    }

    func getBackgroundImage(_ color: CGColor) -> UIImage? {
        UIGraphicsBeginImageContext(CGSize(width: 1, height: 1))
        guard UIGraphicsGetCurrentContext() != nil else { return nil }
        UIGraphicsGetCurrentContext()!.setFillColor(color)
        UIGraphicsGetCurrentContext()!.fill(CGRect(x: 0, y: 0, width: 1, height: 1))
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return image
    }

    func resetEverything() {
        confirmPin = false
        pinCode = ""
        updateView()
    }

    func updateView() {
        content.pinLabel?.enumerated().forEach {(index, label) in
            if index < pinCode.count {
                label.text = "*"
                label.isHidden = false
            } else {
                label.isHidden = true
            }
        }
    }

    @objc func click(sender: Any?) {
        if sender is UIBarButtonItem {
            if setPinMode || editPinMode {
                self.navigationController?.popViewController(animated: true)
            } else {
                getAppDelegate()!.instantiateViewControllerAsRoot(identifier: "InitialViewController")
            }
        } else if let button = sender as? UIButton {
            if button == content.deleteButton {
                if pinCode.count > 0 {
                    pinCode.removeLast()
                    updateView()
                }
            } else if button == content.cancelButton {
                resetEverything()
            } else if button == content.skipButton {
                if restoreMode {
                    getAppDelegate()!.instantiateViewControllerAsRoot(identifier: "TabViewController")
                } else if editPinMode {
                    self.navigationController?.popViewController(animated: true)
                } else if setPinMode {
                    self.performSegue(withIdentifier: "next", sender: self)
                }
            }
        }
    }
}

@IBDesignable
class PinLoginView: UIView {
    @IBOutlet weak var attempts: UILabel!
    @IBOutlet weak var skipButton: UIButton!
    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var deleteButton: UIButton!
    @IBOutlet var keyButton: [UIButton]?
    @IBOutlet var pinLabel: [UILabel]?
    @IBOutlet weak var title: UILabel!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }
}
