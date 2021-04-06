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
        OnBoardManager.shared.params?.walletName = fieldName.text ?? ""
        if LandingViewController.flowType == .add {
            // Register new user
            register()
        } else {
            // Test credential on server side
            checkCredential()
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
            return try appDelegate?.connect(params?.network ?? "mainnet")
        }.then(on: bgq) {
            try getSession().registerUser(mnemonic: params?.mnemonic ?? "").resolve()
        }.then(on: bgq) { _ in
            try getSession().login(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword ?? "").resolve()
        }.ensure {
            self.stopLoader()
        }.done { _ in
            self.next()
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

    func checkCredential() {
        let params = OnBoardManager.shared.params
        let appDelegate = UIApplication.shared.delegate as? AppDelegate
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startLoader(message: NSLocalizedString("id_setting_up_your_wallet", comment: ""))
            return Guarantee()
        }.compactMap(on: bgq) {
            appDelegate?.disconnect()
            return try appDelegate?.connect(params?.network ?? "mainnet")
        }.then(on: bgq) {
            try getSession().login(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword ?? "").resolve()
        }.then(on: bgq) { _ in
            Registry.shared.load()
        }.ensure {
            self.stopLoader()
        }.done { _ in
            self.next()
        }.catch { error in
            if let err = error as? GaError, err != GaError.GenericError {
                self.showError(NSLocalizedString("id_connection_failed", comment: ""))
            } else if let err = error as? AuthenticationTypeHandler.AuthError {
                self.showError(err.localizedDescription)
            } else if !error.localizedDescription.isEmpty {
                self.showError(NSLocalizedString(error.localizedDescription, comment: ""))
            } else {
                self.showError(NSLocalizedString("id_login_failed", comment: ""))
            }
        }
    }

    func next() {
        let account = OnBoardManager.shared.account()
        AccountsManager.shared.upsert(account)
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
