import UIKit
import PromiseKit

class WalletNameViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var fieldName: UITextField!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblSubtitle: UILabel!
    @IBOutlet weak var lblSubtitleHint: UILabel!
    @IBOutlet weak var btnSettings: UIButton!
    @IBOutlet weak var btnNext: UIButton!

    private var networkSettings: [String: Any] {
        get {
            UserDefaults.standard.value(forKey: "network_settings") as? [String: Any] ?? [:]
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        fieldName.delegate = self
        fieldName.text = OnBoardManager.shared.params?.walletName ?? ""
        setContent()
        setStyle()
        updateUI()
        hideKeyboardWhenTappedAround()

        view.accessibilityIdentifier = AccessibilityIdentifiers.WalletNameScreen.view
        fieldName.accessibilityIdentifier = AccessibilityIdentifiers.WalletNameScreen.nameField
        btnNext.accessibilityIdentifier = AccessibilityIdentifiers.WalletNameScreen.nextBtn
        btnSettings.accessibilityIdentifier = AccessibilityIdentifiers.WalletNameScreen.settingsBtn
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_wallet_name", comment: "")
        lblHint.text = NSLocalizedString("id_choose_a_name_for_your_wallet", comment: "")
        lblSubtitle.text = NSLocalizedString("id_app_settings", comment: "")
        lblSubtitleHint.text = NSLocalizedString("id_you_can_change_these_later_on", comment: "")
        btnSettings.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)
        btnNext.setTitle(NSLocalizedString("id_continue", comment: ""), for: .normal)
    }

    func setStyle() {
        fieldName.setLeftPaddingPoints(10.0)
        fieldName.setRightPaddingPoints(10.0)

        btnSettings.cornerRadius = 4.0
        btnSettings.borderWidth = 1.0
        btnSettings.borderColor = UIColor.customGrayLight()
        btnSettings.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
    }

    func updateUI() {
        if fieldName.text?.count ?? 0 > 2 {
            btnNext.setStyle(.primary)
        } else {
            btnNext.setStyle(.primaryDisabled)
        }
    }

    func nameIsSet() {
        OnBoardManager.shared.params?.walletName = fieldName.text ?? ""
        setup(restored: LandingViewController.flowType == .restore)
    }

    @IBAction func nameDidChange(_ sender: Any) {
        updateUI()
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
            vc.account = OnBoardManager.shared.account
            present(vc, animated: true) {}
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        if networkSettings["tor"] as? Bool ?? false &&
            OnBoardManager.shared.params?.singleSig ?? false &&
            !UserDefaults.standard.bool(forKey: AppStorage.dontShowTorAlert) {
            presentDialogTorUnavailable()
        } else {
            nameIsSet()
        }
    }

    func presentDialogTorUnavailable() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogTorSingleSigViewController") as? DialogTorSingleSigViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                self.present(vc, animated: false, completion: nil)
            }
        }
    }

    func setup(restored: Bool) {
        let bgq = DispatchQueue.global(qos: .background)
        let session = SessionsManager.new(for: OnBoardManager.shared.account)
        let params = OnBoardManager.shared.params
        firstly {
            self.startLoader(message: NSLocalizedString("id_setting_up_your_wallet", comment: ""))
            return Guarantee()
        }.then(on: bgq) { () -> Promise<Void> in
            if restored {
                return session.restore(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword, hwDevice: nil)
            } else {
                return session.create(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword, hwDevice: nil)
            }
        }.ensure {
            self.stopLoader()
        }.done { _ in
            AccountsManager.shared.current = session.account
            self.next()
        }.catch { error in
            switch error {
            case LoginError.walletNotFound:
                self.error(session, message: NSLocalizedString("id_wallet_not_found", comment: ""))
            case LoginError.walletsJustRestored:
                self.error(session, message: NSLocalizedString("id_wallet_already_restored", comment: ""))
            case LoginError.invalidMnemonic:
                self.error(session, message: NSLocalizedString("id_invalid_recovery_phrase", comment: ""))
            case LoginError.connectionFailed:
                self.error(session, message: NSLocalizedString("id_connection_failed", comment: ""))
            default:
                self.error(session, message: error.localizedDescription)
            }
        }
    }

    func error(_ session: SessionManager, message: String) {
        DropAlert().error(message: NSLocalizedString(message, comment: ""))
        session.destroy()
    }

    func next() {
        DispatchQueue.main.async {
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "SetPinViewController")
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

}

extension WalletNameViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
}

extension WalletNameViewController: DialogTorSingleSigViewControllerDelegate {
    func didContinue() {
        nameIsSet()
    }
}
