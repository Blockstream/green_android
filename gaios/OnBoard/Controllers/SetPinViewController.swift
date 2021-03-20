import UIKit
import PromiseKit

enum ActionPin {
    case set
    case verify
}

class SetPinViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var deleteButton: UIButton!
    @IBOutlet var keyButton: [UIButton]?
    @IBOutlet var pinLabel: [UILabel]?
    @IBOutlet weak var btnNext: UIButton!

    var pinCodeToVerify = ""
    private var pinCode = ""

    var actionPin = ActionPin.set

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()

    }

    func setContent() {
        title = ""

        switch actionPin {
        case .set:
            lblTitle.text = "Set a PIN"
        case .verify:
            lblTitle.text = "Verify PIN"
        }

        lblHint.text = "You'll need your PIN to log in in to your wallet. This PIN secures the wallet on this device only"
    }

    func setStyle() {
        btnNext.cornerRadius = 4.0
    }

    func setActions() {

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        cancelButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        deleteButton.addTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        for button in keyButton!.enumerated() {
            button.element.addTarget(self, action: #selector(keyClick(sender:)), for: .touchUpInside)
        }
        reload()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        ScreenLocker.shared.startObserving()
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.Tor.rawValue), object: nil)

        cancelButton.removeTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        deleteButton.removeTarget(self, action: #selector(click(sender:)), for: .touchUpInside)
        for button in keyButton!.enumerated() {
            button.element.removeTarget(self, action: #selector(keyClick(sender:)), for: .touchUpInside)
        }
    }

    @objc func keyClick(sender: UIButton) {

        if pinCode.count == 6 { return }

        pinCode += (sender.titleLabel?.text)!
        reload()
        guard pinCode.count == 6 else {
            return
        }

        switch actionPin {
        case .set:
            moveToNext()
        case .verify:
            verifyPins()
        }
    }

    func verifyPins() {
        if pinCode == pinCodeToVerify {
            nextSetEnabled(true)
        } else {
            DropAlert().error(message: "PINs don't match")
        }

    }

    func reload() {
        pinLabel?.enumerated().forEach {(index, label) in
            if index < pinCode.count {
                label.textColor = UIColor.customMatrixGreen()
            } else {
                label.textColor = UIColor.black
            }
        }
        switch actionPin {
        case .set:
            nextSetEnabled(pinCode.count == 6)
        case .verify:
            nextSetEnabled(pinCode.count == 6 && (pinCode == pinCodeToVerify))
        }
    }

    func nextSetEnabled(_ isEnabled: Bool) {
        btnNext.backgroundColor = ( isEnabled ? UIColor.customMatrixGreen() : UIColor.customBtnOff())
        btnNext.isEnabled = isEnabled
        btnNext.setTitleColor(( isEnabled ? UIColor.white : UIColor.customGrayLight()), for: .normal)
    }

    @objc func click(sender: UIButton) {
        if sender == deleteButton {
            if pinCode.count > 0 {
                pinCode.removeLast()
            }
        } else if sender == cancelButton {
            pinCode = ""
        }
        reload()
    }

    func moveToNext() {
        switch actionPin {
        case .set:
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "SetPinViewController") as? SetPinViewController {
                vc.pinCodeToVerify = pinCode
                vc.actionPin = .verify
                navigationController?.pushViewController(vc, animated: true)
            }
        case .verify:
            register(pinCode)
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        moveToNext()
    }

    fileprivate func register(_ pin: String) {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            switch LandingViewController.flowType {
            case .add:
                self.startLoader(message: "Setting Up Your Wallet")
            case .restore:
                self.startLoader(message: "Finishing Up")
            }
            return Guarantee()
        }.compactMap(on: bgq) {
            try? AccountsManager.shared.current?.addPin(session: getSession(), pin: pin)
        }.ensure {
            self.stopLoader()
        }.done {
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "WalletSuccessViewController")
            self.navigationController?.pushViewController(vc, animated: true)
        }.catch { error in
            if let err = error as? GaError, err != GaError.GenericError {
                self.showError(NSLocalizedString("id_connection_failed", comment: ""))
            } else if let err = error as? AuthenticationTypeHandler.AuthError {
                self.showError(err.localizedDescription)
            } else if !error.localizedDescription.isEmpty {
                self.showError(NSLocalizedString(error.localizedDescription, comment: ""))
            } else {
                self.showError(NSLocalizedString("id_operation_failure", comment: ""))
            }
        }
    }
}
