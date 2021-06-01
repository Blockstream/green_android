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
        if LandingViewController.flowType == .add {
            // Register new user
            register()
        } else {
            // Test credential on server side
            checkCredential()
        }
    }

    @IBAction func nameDidChange(_ sender: Any) {
        updateUI()
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController")
        present(vc, animated: true) {
            //
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

    fileprivate func register() {
        let appDelegate = UIApplication.shared.delegate as? AppDelegate
        let bgq = DispatchQueue.global(qos: .background)
        let params = OnBoardManager.shared.params
        firstly {
            self.startLoader(message: NSLocalizedString("id_setting_up_your_wallet", comment: ""))
            return Guarantee()
        }.compactMap(on: bgq) {
            appDelegate?.disconnect()
            return try appDelegate?.connect(OnBoardManager.shared.networkName)
        }.then(on: bgq) {
            try getSession().registerUser(mnemonic: params?.mnemonic ?? "").resolve()
        }.then(on: bgq) { _ in
            try getSession().login(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword ?? "").resolve()
        }.ensure {
            self.stopLoader()
        }.done { _ in
            self.next()
        }.catch { error in
            switch error {
            case AuthenticationTypeHandler.AuthError.ConnectionFailed:
                DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
            default:
                DropAlert().error(message: NSLocalizedString("id_login_failed", comment: ""))
            }
        }
    }

    func checkCredential() {
        let params = OnBoardManager.shared.params
        let appDelegate = UIApplication.shared.delegate as? AppDelegate
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startLoader(message: NSLocalizedString("id_setting_up_your_wallet", comment: ""))
            return Guarantee()
        }.compactMap(on: bgq) {
            appDelegate?.disconnect()
            return try appDelegate?.connect(OnBoardManager.shared.networkName)
        }.then(on: bgq) {
            return try getSession().login(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword ?? "").resolve()
        }.ensure {
            self.stopLoader()
        }.done { _ in
            self.next()
        }.catch { error in
            switch error {
            case AuthenticationTypeHandler.AuthError.ConnectionFailed:
                DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
            case TwoFactorCallError.failure(_):
                DropAlert().error(message: NSLocalizedString("id_login_failed", comment: ""))
            default:
                DropAlert().error(message: NSLocalizedString("id_login_failed", comment: ""))
            }
        }
    }

    func next() {
        let account = OnBoardManager.shared.account()
        AccountsManager.shared.current = account
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "SetPinViewController")
        self.navigationController?.pushViewController(vc, animated: true)
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
