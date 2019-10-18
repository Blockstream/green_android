import Foundation
import UIKit
import PromiseKit

enum PinMode {
    case restore
    case create
    case edit
}

class PinSetViewController: UIViewController {

    @IBOutlet var content: PinView!
    var pinCode = ""
    var pinConfirm = ""
    var mode = PinMode.restore

    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.setHidesBackButton(true, animated: false)
        content.cancelButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        content.deleteButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        for button in content.keyButton!.enumerated() {
            button.element.addTarget(self, action: #selector(keyClick(sender:)), for: .touchUpInside)
        }
        content.title.text = NSLocalizedString("id_create_a_pin_to_access_your", comment: "")
        content.attempts.text = NSLocalizedString("id_pins_do_not_match_please_try", comment: "")
        content.attempts.isHidden = true
        content.reload()
        reload()
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

    @objc func click(sender: UIButton) {
        if sender == content.deleteButton {
            if pinCode.count == 0 { return }
            pinCode.removeLast()
            reload()
        } else if sender == content.cancelButton {
            pinCode = ""
            reload()
        }
    }

    func segue() {
        if self.mode == .edit {
            self.navigationController?.popViewController(animated: true)
        } else if self.mode == .restore {
            getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        } else {
            self.performSegue(withIdentifier: "next", sender: self)
        }
    }

    @objc func keyClick(sender: UIButton) {
        pinCode += (sender.titleLabel?.text)!
        reload()
        if pinCode.count < 6 {
            // pin insertion
        } else if pinConfirm.isEmpty {
            // switch from pin to confirm pin
            content.title.text = NSLocalizedString("id_verify_your_pin", comment: "")
            content.attempts.isHidden = true
            pinConfirm = pinCode
            pinCode = ""
            reload()
        } else if pinConfirm != pinCode {
            // pin mismatch
            content.title.text = NSLocalizedString("id_set_a_new_pin", comment: "")
            content.attempts.isHidden = false
            pinCode = ""
            pinConfirm = ""
            reload()
        } else {
            // pin match
             setPin(self.pinCode)
        }
    }

    fileprivate func setPin(_ pin: String) {
        let bgq = DispatchQueue.global(qos: .background)
        let network = getNetwork()

        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            let mnemonics = try getSession().getMnemonicPassphrase(password: "")
            return try getSession().setPin(mnemonic: mnemonics, pin: pin, device: String.random(length: 14))
        }.map(on: bgq) { (data: [String: Any]) -> Void in
            try AuthenticationTypeHandler.addPIN(data: data, forNetwork: network)
        }.ensure {
            self.stopAnimating()
        }.done {
            self.segue()
        }.catch { error in
            let message: String
            if let err = error as? GaError, err != GaError.GenericError {
                message = NSLocalizedString("id_you_are_not_connected_to_the", comment: "")
            } else if let err = error as? AuthenticationTypeHandler.AuthError {
                message = err.localizedDescription
            } else if !error.localizedDescription.isEmpty {
                message = NSLocalizedString(error.localizedDescription, comment: "")
            } else {
                message = NSLocalizedString("id_operation_failure", comment: "")
            }
            let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_ok", comment: ""), style: .default) { _ in })
            self.present(alert, animated: true, completion: nil)
        }
    }
}
