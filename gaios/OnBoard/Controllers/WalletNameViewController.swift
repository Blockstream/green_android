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

    private var defaultName = ""

    override func viewDidLoad() {
        super.viewDidLoad()

        if let isSingleSig = OnBoardManager.shared.account.isSingleSig,
            let network: AvailableNetworks = (AvailableNetworks.allCases.filter { $0.rawValue == OnBoardManager.shared.account.network}).first {

            defaultName = AccountsManager.shared.getUniqueAccountName(
                securityOption: isSingleSig ? .single : .multi,
                network: network)
        }

        fieldName.delegate = self
        fieldName.text = OnBoardManager.shared.params?.walletName ?? ""
        fieldName.attributedPlaceholder = NSAttributedString(
            string: defaultName,
            attributes: [NSAttributedString.Key.foregroundColor: UIColor.lightGray])

        setContent()
        setStyle()
        updateUI()
        hideKeyboardWhenTappedAround()

        view.accessibilityIdentifier = AccessibilityIdentifiers.WalletNameScreen.view
        fieldName.accessibilityIdentifier = AccessibilityIdentifiers.WalletNameScreen.nameField
        btnNext.accessibilityIdentifier = AccessibilityIdentifiers.WalletNameScreen.nextBtn
        btnSettings.accessibilityIdentifier = AccessibilityIdentifiers.WalletNameScreen.settingsBtn

        switch LandingViewController.flowType {
        case .add:
            AnalyticsManager.shared.recordView(.onBoardWalletName, sgmt: AnalyticsManager.shared.onBoardSgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strCreate))
        case .restore:
            AnalyticsManager.shared.recordView(.onBoardWalletName, sgmt: AnalyticsManager.shared.onBoardSgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strRestore))
        case .watchonly:
            break
        }

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

    func getValidTxt() -> String? {
        if let txt = fieldName.text {
            if txt.count == 0 && !defaultName.isEmpty {
                return defaultName
            } else if txt.count > 2 {
                return txt
            }
        }
        return nil
    }

    func updateUI() {
        if getValidTxt() != nil {
            btnNext.setStyle(.primary)
            return
        }
        btnNext.setStyle(.primaryDisabled)
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
        let txt = getValidTxt()
        OnBoardManager.shared.params?.walletName = txt
        setup(restored: LandingViewController.flowType == .restore)
    }

    func setup(restored: Bool) {
        let bgq = DispatchQueue.global(qos: .background)
        let account =  OnBoardManager.shared.account
        let session = SessionsManager.new(for: account)
        let params = OnBoardManager.shared.params
        let credentials = Credentials(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword, bip39Passphrase: nil)
        firstly {
            self.startLoader(message: NSLocalizedString("id_setting_up_your_wallet", comment: ""))
            return Guarantee()
        }.then(on: bgq) { () -> Promise<Void> in
            if restored {
                return session.discover(mnemonic: params?.mnemonic ?? "", password: params?.mnemomicPassword)
                    .recover { err in
                        switch err {
                        case LoginError.walletNotFound:
                            if !(account.isSingleSig ?? false) {
                                throw err
                            }
                        default:
                            throw err
                        }
                    }.then {
                        session.restore(credentials)
                    }
            } else {
                return session.create(credentials)
            }
        }.ensure {
            self.stopLoader()
        }.done { _ in
            AccountsManager.shared.current = session.account
            if restored {
                AnalyticsManager.shared.restoreWallet(account: AccountsManager.shared.current)
            } else {
                AnalyticsManager.shared.createWallet(account: AccountsManager.shared.current)
            }
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
            case TwoFactorCallError.cancel(localizedDescription: let desc), TwoFactorCallError.failure(localizedDescription: let desc):
                self.error(session, message: desc)
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
