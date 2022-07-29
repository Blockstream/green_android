import UIKit
import PromiseKit

protocol UserSettingsViewControllerDelegate: AnyObject {
    func userLogout()
}

class UserSettingsViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    weak var delegate: UserSettingsViewControllerDelegate?

    var items = [UserSettingsItem]()
    var sections = [UserSettingsSections]()
    var data: [UserSettingsSections: Any] = [:]
    var account = { AccountsManager.shared.current }()
    var isWatchOnly: Bool { get { return account?.isWatchonly ?? false } }
    var username: String?
    var twoFactorConfig: TwoFactorConfig?
    var isResetActive: Bool {
        get {
            SessionsManager.current?.isResetActive ?? false
        }
    }
    var isLiquid: Bool { get { return account?.gdkNetwork?.liquid ?? false } }
    var isHW: Bool { get { return account?.isHW ?? false } }
    var isSingleSig: Bool { get { return account?.isSingleSig ?? false }}

    var headerH: CGFloat = 54.0
    var wallet: WalletItem?

    override func viewDidLoad() {
        super.viewDidLoad()

        title = NSLocalizedString("id_settings", comment: "")
        let btn = UIBarButtonItem(image: UIImage(named: "cancel")?.maskWithColor(color: .white), style: .plain, target: self, action: #selector(self.close))
        self.navigationItem.rightBarButtonItem  = btn
        view.accessibilityIdentifier = AccessibilityIdentifiers.SettingsScreen.view

        AnalyticsManager.shared.recordView(.walletSettings, sgmt: AnalyticsManager.shared.sessSgmt(AccountsManager.shared.current))
    }

    @objc func close() {
        dismiss(animated: true, completion: nil)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        reloadData()
        if !isWatchOnly {
            Guarantee()
                .compactMap(on: .global(qos: .background)) { try self.load() }
                .done { self.reloadData() }
                .catch { err in print(err) }
        }
    }

    func reloadData() {
        self.sections = self.getSections()
        self.items = self.getItems()
        self.data = Dictionary(grouping: self.items) { (item) in
            return item.section
        }
        self.tableView.reloadData()
    }

    func load() throws {
        if let session = SessionsManager.current {
            if let settings = try session.session?.getSettings() {
                SessionsManager.current?.settings = Settings.from(settings)
            }
            if let account = account, !(account.isSingleSig ?? false) {
                // watchonly available on multisig
                session.getWatchOnlyUsername().done {
                    self.username = $0
                }
            }
        }
    }

    func getSections() -> [UserSettingsSections] {
        if isWatchOnly {
            return [.logout, .about]
        } else if isResetActive {
            return [.logout, .general, .security, .recovery, .about]
        } else if isSingleSig {
            return [.logout, .general, .security, .recovery, .about]
        } else if isHW && isLiquid {
            return [.logout, .general, .security, .advanced, .about]
        }
        return UserSettingsSections.allCases
    }

    func getLogoutItems() -> [UserSettingsItem] {
        let logout = UserSettingsItem(
            title: String(format: NSLocalizedString("id_s_network", comment: ""), getNetwork()).localizedCapitalized,
            subtitle: NSLocalizedString("id_log_out", comment: ""),
            section: .logout,
            type: .Logout)
        return [logout]
    }

    func getGeneralItems() -> [UserSettingsItem] {
        var items = [UserSettingsItem]()
        let watchOnly = UserSettingsItem(
            title: "Watch-only credentials",
            subtitle: String(format: NSLocalizedString((username == nil || username!.isEmpty) ? "id_disabled" : "id_enabled_1s", comment: ""), username ?? ""),
            section: .general,
            type: .WatchOnly)
        if  isSingleSig || isWatchOnly || isResetActive {} else {
            items += [watchOnly]
        }
        if let settings = SessionsManager.current?.settings {
            let bitcoinDenomination = UserSettingsItem(
                title: NSLocalizedString("id_bitcoin_denomination", comment: ""),
                subtitle: settings.denomination.string,
                section: .general,
                type: .BitcoinDenomination)
            let referenceExchangeRate = UserSettingsItem(
                title: NSLocalizedString("id_reference_exchange_rate", comment: ""),
                subtitle: "\(settings.pricing["currency"]!)/\(settings.pricing["exchange"]!.capitalized)",
                section: .general,
                type: .ReferenceExchangeRate)
            if isWatchOnly && isResetActive {} else {
                items += [bitcoinDenomination, referenceExchangeRate]
            }

        }
        let defaultTransactionPriority = UserSettingsItem(
            title: NSLocalizedString("id_default_transaction_priority", comment: ""),
            subtitle: TransactionPriority.getPreference()?.text ?? "",
            section: .general,
            type: .DefaultTransactionPriority)
        items += [defaultTransactionPriority]

        return items
    }

    func getSecurityItems() -> [UserSettingsItem] {

        var items = [UserSettingsItem]()

        let changePin = UserSettingsItem(
            title: NSLocalizedString("id_change_pin", comment: ""),
            subtitle: "",
            section: .security,
            type: .ChangePin)

        let bioTitle = AuthenticationTypeHandler.supportsBiometricAuthentication() ? NSLocalizedString(AuthenticationTypeHandler.biometryType == .faceID ? "id_face_id" : "id_touch_id", comment: "") : NSLocalizedString("id_touchface_id_not_available", comment: "")
        let loginWithBiometrics = UserSettingsItem(
            title: bioTitle,
            subtitle: "",
            section: .security,
            type: .LoginWithBiometrics)

        if let account = account,
           !account.isEphemeral && !account.isHW &&
            !account.isWatchonly && !isResetActive {
            items += [changePin, loginWithBiometrics]
        }

        if let settings = SessionsManager.current?.settings {
            let autolock = UserSettingsItem(
                title: NSLocalizedString("id_auto_logout_timeout", comment: ""),
                subtitle: settings.autolock.string,
                section: .security,
                type: .AutoLogout)
            if !isWatchOnly && !isResetActive {
                items += [autolock]
            }
        }

        let twoFactorAuthentication = UserSettingsItem(
            title: NSLocalizedString("id_twofactor_authentication", comment: ""),
            subtitle: "",
            section: .security,
            type: .TwoFactorAuthentication)

        if !isWatchOnly && !isSingleSig {
            items += [twoFactorAuthentication]
        }

        return items
    }

    func getAdvancedItems() -> [UserSettingsItem] {

        var items = [UserSettingsItem]()
        let pgp = UserSettingsItem(
            title: NSLocalizedString("id_pgp_key", comment: ""),
            subtitle: "",
            section: .advanced,
            type: .Pgp)
        if !isWatchOnly {
            items += [pgp]
        }
        return items
    }

    func getAboutItems() -> [UserSettingsItem] {
        let versionSubtitle = String(format: NSLocalizedString("id_version_1s", comment: ""), Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? CVarArg ?? "")
        let version = UserSettingsItem(
            title: NSLocalizedString("id_version", comment: ""),
            subtitle: versionSubtitle,
            section: .about,
            type: .Version)
        let support = UserSettingsItem(
            title: NSLocalizedString("id_support", comment: ""),
            subtitle: "Copy support ID",
            section: .about,
            type: .SupportID)
        let termOfUse = UserSettingsItem(
            title: NSLocalizedString("id_terms_of_use", comment: ""),
            subtitle: "",
            section: .about,
            type: .TermsOfUse)
        let privacyPolicy = UserSettingsItem(
            title: NSLocalizedString("id_privacy_policy", comment: ""),
            subtitle: "",
            section: .about,
            type: .PrivacyPolicy)
        if account?.isSingleSig ?? false {
            return [version, termOfUse, privacyPolicy]
        }
        return [version, support, termOfUse, privacyPolicy]
    }

    func getRecoveryItems() -> [UserSettingsItem] {
        var items = [UserSettingsItem]()

        let backUpRecoveryPhrase = UserSettingsItem(
            title: NSLocalizedString("id_back_up_recovery_phrase", comment: ""),
            subtitle: "",
            section: .recovery,
            type: .BackUpRecoveryPhrase)
        if isHW {
        } else if !isWatchOnly {
            items += [backUpRecoveryPhrase]
        }

        if let settings = SessionsManager.current?.settings {
            var locktimeRecoveryEnable = false
            if let notifications = settings.notifications {
                locktimeRecoveryEnable = notifications.emailOutgoing == true
            }
            let locktimeRecovery = UserSettingsItem(
                title: NSLocalizedString("id_recovery_transactions", comment: ""),
                subtitle: locktimeRecoveryEnable ? NSLocalizedString("id_enabled", comment: "") : NSLocalizedString("id_disabled", comment: ""),
                section: .recovery,
                type: .RecoveryTransactions)

            if isWatchOnly || isLiquid || isSingleSig {
            } else {
                items += [locktimeRecovery]
            }
        }

        return items
    }

    func getItems() -> [UserSettingsItem] {

        var items = [UserSettingsItem]()

        items += getLogoutItems()
        items += getGeneralItems()
        items += getSecurityItems()
        items += getRecoveryItems()
        items += getAdvancedItems()
        items += getAboutItems()

        return items
    }

    func onBiometricSwitch(_ value: Bool) {
        if value == true {
            enableBioAuth()
        } else {
            disableBioAuth()
        }
    }

    func getSwitchValue() -> Bool {

        guard let screenlock = SessionsManager.current?.settings?.getScreenLock() else {
            DropAlert().error(message: NSLocalizedString("id_operation_failure", comment: ""))
            return false
        }
        if screenlock == .None {
            return false
        } else if screenlock == .All {
            return true
        } else if screenlock == .FaceID || screenlock == .TouchID {
            // this should never happen
            NSLog("no pin exists but faceid/touchid is enabled" )
            return true
        } else if screenlock == .Pin {
            return false
        }
        return false
    }
}

extension UserSettingsViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return sections.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let section = sections[section]
        let itemsInSection = data[section] as? [UserSettingsItem]
        return itemsInSection?.count ?? 0
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        let section = sections[indexPath.section]
        let itemsInSection = data[section] as? [UserSettingsItem]
        let item = itemsInSection![indexPath.row]

        if let cell = tableView.dequeueReusableCell(withIdentifier: "UserSettingsCell") as? UserSettingsCell {
            cell.configure(item)

            // customize single cells
            if item.type == .LoginWithBiometrics {
                cell.selectionStyle = .none
                cell.actionSwitch.isHidden = false
                cell.actionSwitch.isEnabled = AuthenticationTypeHandler.supportsBiometricAuthentication()
                cell.actionSwitch.isOn = getSwitchValue()
                cell.onActionSwitch = { [weak self] in
                    self?.onBiometricSwitch(cell.actionSwitch.isOn)
                }
            } else if item.type == .Version {
                cell.selectionStyle = .none
                cell.actionSwitch.isHidden = true
            } else {
                cell.actionSwitch.isHidden = true
                let selectedView = UIView()
                selectedView.backgroundColor = UIColor.customModalDark()
                cell.selectedBackgroundView = selectedView
            }
            return cell
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch section {
        case 0:
            return 0
        default:
            return headerH
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        return headerView(UserSettingsSections.name(sections[section]))
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)

        let section = sections[indexPath.section]
        let itemsInSection = data[section] as? [UserSettingsItem]
        let item = itemsInSection![indexPath.row]

        switch item.type {

        case .Logout:
            logout()
        case .WatchOnly:

            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogWatchOnlySetUpViewController") as? DialogWatchOnlySetUpViewController {
                vc.modalPresentationStyle = .overFullScreen
                vc.delegate = self
                vc.wallet = wallet
                vc.username = username
                present(vc, animated: false, completion: nil)
            }
        case .BitcoinDenomination:
            showBitcoinDenomination()
        case .ReferenceExchangeRate:
            let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "CurrencySelectorViewController")
            navigationController?.pushViewController(vc, animated: true)
        case .DefaultTransactionPriority:
            showDefaultTransactionPriority()
        case .BackUpRecoveryPhrase:
            let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "MnemonicAuthViewController")
            navigationController?.pushViewController(vc, animated: true)
        case .RecoveryTransactions:
            showRecoveryTransactions()
        case .ChangePin:
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "SetPinViewController") as? SetPinViewController {
                vc.pinFlow = .settings
                navigationController?.pushViewController(vc, animated: true)
            }
        case.LoginWithBiometrics:
            break
        case .AutoLogout:
            showAutoLogout()
        case .TwoFactorAuthentication:
            let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "TwoFactorAuthenticationViewController") as? TwoFactorAuthenticationViewController {
                navigationController?.pushViewController(vc, animated: true)
                vc.delegate = self
                vc.wallet = wallet
            }

        case .Pgp:
            let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "PgpViewController")
            navigationController?.pushViewController(vc, animated: true)
        case .Version:
            break
        case .SupportID:
            SessionsManager.current?.subaccount(0).done { wallet in
                UIPasteboard.general.string = wallet.receivingId
                DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 1.0)
                UINotificationFeedbackGenerator().notificationOccurred(.success)
            }.catch { _ in }
        case .TermsOfUse:
            UIApplication.shared.open(URL(string: "https://blockstream.com/green/terms/")!)
        case .PrivacyPolicy:
            UIApplication.shared.open(URL(string: "https://blockstream.com/green/privacy/")!)
        default:
            break
        }
    }
}

extension UserSettingsViewController {
    func headerView(_ txt: String) -> UIView {
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.customTitaniumDark()
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 20.0, weight: .heavy)
        title.text = txt
        title.textColor = .white
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.bottomAnchor.constraint(equalTo: section.bottomAnchor, constant: -10),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 24),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -24)
        ])

        return section
    }
}

extension UserSettingsViewController {

    func showAlert(_ error: Error) {
        let text: String
        if let error = error as? TwoFactorCallError {
            switch error {
            case .failure(let localizedDescription), .cancel(let localizedDescription):
                text = localizedDescription
            }
            self.showError(text)
        }
    }

    func logout() {
        delegate?.userLogout()
    }

    func showBitcoinDenomination() {
        let list = [ .BTC, .MilliBTC, .MicroBTC, .Bits, .Sats].map { DenominationType.denominations[$0]! }
        guard let settings = SessionsManager.current?.settings else { return }
        let selected = settings.denomination.string
        let alert = UIAlertController(title: NSLocalizedString("id_bitcoin_denomination", comment: ""), message: "", preferredStyle: .actionSheet)
        list.forEach { (item: String) in
            alert.addAction(UIAlertAction(title: item, style: item == selected  ? .destructive : .default) { _ in
                settings.denomination = DenominationType.from(item)
                self.changeSettings(settings)
            })
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        self.present(alert, animated: true, completion: nil)
    }

    func showWatchOnly() {
        let alert = UIAlertController(title: NSLocalizedString("id_set_up_watchonly", comment: ""), message: NSLocalizedString("id_allows_you_to_quickly_check", comment: ""), preferredStyle: .alert)
        alert.addTextField { (textField) in
            textField.placeholder = NSLocalizedString("id_username", comment: "")
            textField.accessibilityIdentifier = AccessibilityIdentifiers.SettingsScreen.usernameField
        }
        alert.addTextField { (textField) in
            textField.placeholder = NSLocalizedString("id_password", comment: "")
            textField.accessibilityIdentifier = AccessibilityIdentifiers.SettingsScreen.passwordField
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_save", comment: ""), style: .default) { _ in
            let username = alert.textFields![0].text!
            let password = alert.textFields![1].text!
            self.setWatchOnly(username: username, password: password)
        })
        self.present(alert, animated: true, completion: nil)
    }

    func setWatchOnly(username: String, password: String) {
        if username.isEmpty {
            self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: NSLocalizedString("id_enter_a_valid_username", comment: ""))
            return
        } else if password.isEmpty {
            self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: NSLocalizedString("id_the_password_cant_be_empty", comment: ""))
            return
        }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            SessionsManager.current?.setWatchOnly(username: username, password: password)
            try self.load()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.reloadData()
        }.catch { err in
            switch err {
            case GaError.GenericError(let desc):
                self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: desc ?? "")
            default:
                self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: err.localizedDescription)
            }
        }
    }

    func showAutoLogout() {
        guard let settings = SessionsManager.current?.settings else { return }
        let list = [AutoLockType.minute.string, AutoLockType.twoMinutes.string, AutoLockType.fiveMinutes.string, AutoLockType.tenMinutes.string, AutoLockType.sixtyMinutes.string]
        let selected = settings.autolock.string
        let alert = UIAlertController(title: NSLocalizedString("id_auto_logout_timeout", comment: ""), message: "", preferredStyle: .actionSheet)
        list.forEach { (item: String) in
            alert.addAction(UIAlertAction(title: item, style: item == selected  ? .destructive : .default) { _ in
                settings.autolock = AutoLockType.from(item)
                self.changeSettings(settings)
            })
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        self.present(alert, animated: true, completion: nil)
    }

    func changeSettings(_ settings: Settings) {
        guard let session = SessionsManager.current else { return }
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map {_ in
            self.startAnimating()
        }.then(on: bgq) { _ in
            session.changeSettings(settings: settings)
        }.compactMap(on: bgq) { _ in
            try self.load()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.reloadData()
            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "settings"), object: nil, userInfo: nil)
        }.catch { error in
            self.showAlert(error)
        }
    }

    func showRecoveryTransactions() {
        var enabled = false
        guard let settings = SessionsManager.current?.settings else { return }
        if let notifications = settings.notifications {
            enabled = notifications.emailOutgoing == true
        }
        let alert = UIAlertController(title: NSLocalizedString("id_recovery_transaction_emails", comment: ""), message: "", preferredStyle: .actionSheet)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_enable", comment: ""), style: enabled ? .destructive : .default) { _ in
            let notifications = ["email_incoming": true, "email_outgoing": true]
            let json = try! JSONSerialization.data(withJSONObject: notifications, options: [])
            settings.notifications = try! JSONDecoder().decode(SettingsNotifications.self, from: json)
            self.changeSettings(settings)
        })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_disable", comment: ""), style: !enabled ? .destructive : .default) { _ in
            let notifications = ["email_incoming": false, "email_outgoing": false]
            let json = try! JSONSerialization.data(withJSONObject: notifications, options: [])
            settings.notifications = try! JSONDecoder().decode(SettingsNotifications.self, from: json)
            self.changeSettings(settings)
        })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        self.present(alert, animated: true, completion: nil)
    }

    func showDefaultTransactionPriority() {
        let list = [TransactionPriority.High, TransactionPriority.Medium, TransactionPriority.Low]
        let selected: TransactionPriority? = TransactionPriority.getPreference()
        let alert = UIAlertController(title: NSLocalizedString("id_default_transaction_priority", comment: ""), message: "", preferredStyle: .actionSheet)
        list.forEach { (item: TransactionPriority) in
            alert.addAction(UIAlertAction(title: item.text, style: item == selected ? .destructive : .default) { [weak self] _ in
                TransactionPriority.setPreference(item)
                self?.reloadData()
            })
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        self.present(alert, animated: true, completion: nil)
    }
}

extension UserSettingsViewController {
    private func enableBioAuth() {
        // An auth key pin should be set before updating bio auth
        if !AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: self.account!.keychain) {
            onAuthError(message: NSLocalizedString("id_please_enable_pin", comment: ""))
            return
        }
        let bgq = DispatchQueue.global(qos: .background)
        guard let session = SessionsManager.current else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try self.account?.addBioPin(session: session)
        }.ensure {
            self.stopAnimating()
        }.catch { error in
            if let err = error as? GaError {
                self.onAuthError(message: NSLocalizedString("id_connection_failed", comment: ""))
            } else if let err = error as? AuthenticationTypeHandler.AuthError {
                self.onBioAuthError(message: err.localizedDescription)
            } else if !error.localizedDescription.isEmpty {
                self.onAuthError(message: NSLocalizedString(error.localizedDescription, comment: ""))
            } else {
                self.onAuthError(message: NSLocalizedString("id_operation_failure", comment: ""))
            }
        }
    }

    private func disableBioAuth() {
        onAuthRemoval { [weak self] in
            self?.account?.removeBioKeychainData()
        }
    }

    func onAuthRemoval(_ completionHandler: @escaping () -> Void) {
        let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: NSLocalizedString("id_your_pin_or_your_mnemonic_will", comment: ""), preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { [weak self] _ in
            DispatchQueue.main.async {
                self?.tableView.reloadData()
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

    func onBioAuthError(message: String) {
        let text = String(format: NSLocalizedString("id_snnreset_this_setting_and_then", comment: ""), message)
        let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: text, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .default) { _ in
            self.navigationController?.popViewController(animated: true)
        })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_reset", comment: ""), style: .destructive) { _ in
            removeBioKeychainData()
            try? AuthenticationTypeHandler.removePrivateKey(forNetwork: self.account!.keychain)
            UserDefaults.standard.set(nil, forKey: "AuthKeyBiometricPrivateKey" + self.account!.keychain)
            self.navigationController?.popViewController(animated: true)
        })
        DispatchQueue.main.async {
            self.present(alert, animated: true, completion: nil)
        }
    }

    func onAuthError(message: String) {
        let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .default) { _ in
            self.navigationController?.popViewController(animated: true)
        })
        DispatchQueue.main.async {
            self.present(alert, animated: true, completion: nil)
        }
    }
}
extension UserSettingsViewController: TwoFactorAuthenticationViewControllerDelegate {
    func userLogout() {
        self.logout()
    }
}

extension UserSettingsViewController: DialogWatchOnlySetUpViewControllerDelegate {
    func watchOnlyDidUpdate(_ action: WatchOnlySetUpAction) {
        switch action {
        case .save, .delete:
            Guarantee()
                .compactMap(on: .global(qos: .background)) { try self.load() }
                .done { self.reloadData() }
                .catch { err in print(err) }
        default:
            break
        }
    }
}
