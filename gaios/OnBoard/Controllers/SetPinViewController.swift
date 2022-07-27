import UIKit
import PromiseKit

enum ActionPin {
    case set
    case verify
}

enum PinFlow {
    case onboard
    case settings
}

class SetPinViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var deleteButton: UIButton!
    @IBOutlet var keyButton: [UIButton]?
    @IBOutlet var pinLabel: [UILabel]?
    @IBOutlet weak var btnNext: UIButton!

    var pinFlow = PinFlow.onboard
    private var pinCodeToVerify = ""
    private var pinCode = ""
    private var actionPin = ActionPin.set

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()

        view.accessibilityIdentifier = AccessibilityIdentifiers.SetPinScreen.view
        keyButton![0].accessibilityIdentifier = AccessibilityIdentifiers.SetPinScreen.btn1
        keyButton![1].accessibilityIdentifier = AccessibilityIdentifiers.SetPinScreen.btn2
        btnNext.accessibilityIdentifier = AccessibilityIdentifiers.SetPinScreen.nextBtn

        if actionPin == .set {
            switch self.pinFlow {
            case .settings:
                AnalyticsManager.shared.recordView(.walletSettingsChangePIN, sgmt: AnalyticsManager.shared.sessSgmt(AccountsManager.shared.current))
            case .onboard:
                switch LandingViewController.flowType {
                case .add:
                    AnalyticsManager.shared.recordView(.onBoardPin, sgmt: AnalyticsManager.shared.onBoardSgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strCreate))
                case .restore:
                    AnalyticsManager.shared.recordView(.onBoardPin, sgmt: AnalyticsManager.shared.onBoardSgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strRestore))
                case .watchonly:
                    break
                }
            }
        }
    }

    func setContent() {
        title = ""

        switch actionPin {
        case .set:
            lblTitle.text = NSLocalizedString("id_set_a_pin", comment: "")
        case .verify:
            lblTitle.text = NSLocalizedString("id_verify_your_pin", comment: "")
        }

        lblHint.text = NSLocalizedString("id_youll_need_your_pin_to_log_in", comment: "")
        btnNext.setTitle(NSLocalizedString("id_continue", comment: ""), for: .normal)
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
            DropAlert().error(message: NSLocalizedString("id_pins_do_not_match_please_try", comment: ""))
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
                vc.pinFlow = self.pinFlow
                navigationController?.pushViewController(vc, animated: true)
            }
        case .verify:
            setPin(pinCode)
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        moveToNext()
    }

    fileprivate func setPin(_ pin: String) {
        let bgq = DispatchQueue.global(qos: .background)
        guard var account = AccountsManager.shared.current,
              let session = SessionsManager.get(for: account) else {
            fatalError("Error: No account or session found")
        }
        firstly {
            switch pinFlow {
            case .settings:
                self.startLoader(message: NSLocalizedString("id_setting_up_your_wallet", comment: ""))
            case .onboard:
                self.startLoader(message: NSLocalizedString("id_finishing_up", comment: ""))
            }
            return Guarantee()
        }.then(on: bgq) {
            session.getCredentials(password: "")
        }.then(on: bgq) {
            account.addPin(session: session, pin: pin, mnemonic: $0.mnemonic)
        }.ensure {
            self.stopLoader()
        }.done { _ in
            account.attempts = 0
            AccountsManager.shared.current = account
            switch self.pinFlow {
            case .settings:
                self.navigationController?.popToViewController(ofClass: UserSettingsViewController.self, animated: true)
            case .onboard:
                let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
                let vc = storyboard.instantiateViewController(withIdentifier: "WalletSuccessViewController")
                self.navigationController?.pushViewController(vc, animated: true)
            }
        }.catch { error in
            if let err = error as? GaError {
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
