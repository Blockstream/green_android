import Foundation
import UIKit
import NVActivityIndicatorView
import PromiseKit

class PinLoginViewController: UIViewController {

    @IBOutlet var content: PinView!
    var pinCode = ""
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
        let network = getGdkNetwork(getNetwork())
        let navigationBarHeight: CGFloat =  navigationController!.navigationBar.frame.height
        let imageView = UIImageView(frame: CGRect(x: 0, y: 0, width: navigationBarHeight, height: navigationBarHeight))
        imageView.contentMode = .scaleAspectFit
        let imageName = network.liquid ? "btc_liquid_title" : network.icon
        imageView.image = UIImage(named: imageName!)
        navigationItem.titleView = imageView
        navigationItem.setHidesBackButton(true, animated: false)
        navigationItem.leftBarButtonItem = UIBarButtonItem(image: UIImage.init(named: "backarrow"), style: UIBarButtonItem.Style.plain, target: self, action: #selector(PinLoginViewController.back))
        content.title.text = NSLocalizedString("id_enter_pin", comment: "")
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        ScreenLocker.shared.stopObserving()
        content.cancelButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        content.deleteButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        for button in content.keyButton!.enumerated() {
            button.element.addTarget(self, action: #selector(keyClick(sender:)), for: .touchUpInside)
        }
        updateAttemptsLabel()
        reload()
    }

    override func viewDidAppear(_ animated: Bool) {
        let network = getNetwork()
        let bioAuth = AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
        if bioAuth {
            loginWithPin(usingAuth: AuthenticationTypeHandler.AuthKeyBiometric, network: network, withPIN: nil)
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        ScreenLocker.shared.startObserving()
        if content == nil { return }
        content.cancelButton.removeTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        content.deleteButton.removeTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        for button in content.keyButton!.enumerated() {
            button.element.removeTarget(self, action: #selector(keyClick(sender:)), for: .touchUpInside)
        }
    }

    fileprivate func loginWithPin(usingAuth: String, network: String, withPIN: String?) {
        let bgq = DispatchQueue.global(qos: .background)
        let appDelegate = getAppDelegate()!

        firstly {
            return Guarantee()
        }.compactMap {
            try AuthenticationTypeHandler.getAuth(method: usingAuth, forNetwork: network)
        }.get { _ in
            self.startAnimating(message: NSLocalizedString("id_logging_in", comment: ""))
        }.get(on: bgq) { _ in
            appDelegate.disconnect()
        }.get(on: bgq) { _ in
            try appDelegate.connect()
        }.map(on: bgq) {
            let jsonData = try JSONSerialization.data(withJSONObject: $0)
            let pin = withPIN ?? $0["plaintext_biometric"] as? String
            let pinData = String(data: jsonData, encoding: .utf8)
            try getSession().loginWithPin(pin: pin!, pin_data: pinData!)
        }.ensure {
            self.stopAnimating()
        }.done {
            self.pinAttemptsPreference = 0
            appDelegate.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        }.catch { error in
            var message = NSLocalizedString("id_login_failed", comment: "")
            if let authError = error as? AuthenticationTypeHandler.AuthError {
                switch authError {
                case .CanceledByUser:
                    return
                default:
                    message = authError.localizedDescription
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
                            appDelegate.instantiateViewControllerAsRoot(storyboard: "Main", identifier: "InitialViewController")
                            return
                        }
                    }
                case .GenericError:
                    break
                default:
                    message = NSLocalizedString("id_you_are_not_connected_to_the", comment: "")
                }
            }
            self.pinCode = ""
            self.updateAttemptsLabel()
            self.reload()
            Toast.show(message, timeout: Toast.SHORT)
        }
    }

    func updateAttemptsLabel() {
        if MAXATTEMPTS - pinAttemptsPreference == 1 {
            content.attempts.text = NSLocalizedString("id_last_attempt_if_failed_you_will", comment: "")
        } else {
            content.attempts.text = String(format: NSLocalizedString("id_attempts_remaining_d", comment: ""), MAXATTEMPTS - pinAttemptsPreference)
        }
        content.attempts.isHidden = pinAttemptsPreference == 0
    }

    @objc func keyClick(sender: UIButton) {
        pinCode += (sender.titleLabel?.text)!
        reload()
        guard pinCode.count == 6 else {
            return
        }
        let network = getNetwork()
        loginWithPin(usingAuth: AuthenticationTypeHandler.AuthKeyPIN, network: network, withPIN: self.pinCode)
    }

    func reload() {
        content.pinLabel?.enumerated().forEach {(index, label) in
            if index < pinCode.count {
                label.text = "*"
                label.isHidden = false
            } else {
                label.isHidden = true
            }
        }
    }

    @objc func back(sender: UIBarButtonItem) {
        getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Main", identifier: "InitialViewController")
    }

    @objc func click(sender: UIButton) {
        if sender == content.deleteButton {
            if pinCode.count > 0 {
                pinCode.removeLast()
            }
        } else if sender == content.cancelButton {
            pinCode = ""
        }
        reload()
    }
}
