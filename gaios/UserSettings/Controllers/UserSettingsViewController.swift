import UIKit
import PromiseKit
import gdk
import greenaddress

protocol UserSettingsViewControllerDelegate: AnyObject {
    func userLogout()
    func refresh()
}

class UserSettingsViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    weak var delegate: UserSettingsViewControllerDelegate?

    var session = { WalletManager.current?.prominentSession }()
    var multiSigSession = { WalletManager.current?.activeSessions.values.filter { !$0.gdkNetwork.electrum }.first }()
    var headerH: CGFloat = 54.0
    var viewModel = UserSettingsViewModel()
    var account: Account? { get { viewModel.wm?.account } }

    override func viewDidLoad() {
        super.viewDidLoad()

        title = NSLocalizedString("id_settings", comment: "")
        let btn = UIBarButtonItem(image: UIImage(named: "cancel")?.maskWithColor(color: .white), style: .plain, target: self, action: #selector(self.close))
        self.navigationItem.rightBarButtonItem  = btn
        view.accessibilityIdentifier = AccessibilityIdentifiers.SettingsScreen.view

        AnalyticsManager.shared.recordView(.walletSettings, sgmt: AnalyticsManager.shared.sessSgmt(account))

        initViewModel()
    }

    func initViewModel() {
        viewModel.reloadTableView = { [weak self] in
            DispatchQueue.main.async {
                self?.tableView.reloadData()
            }
        }
        viewModel.load()
    }

    @objc func close() {
        dismiss(animated: true, completion: nil)
    }

    func onBiometricSwitch(_ value: Bool) {
        if value == true {
            enableBioAuth()
        } else {
            disableBioAuth()
        }
    }

    func getSwitchValue() -> Bool {
        guard let screenlock = session?.settings?.getScreenLock() else {
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
        return viewModel.sections.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return viewModel.getCellModelsForSection(at: section)?.count ?? 0
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let vm = viewModel.getCellModel(at: indexPath)
        if let cell = tableView.dequeueReusableCell(withIdentifier: UserSettingsCell.identifier, for: indexPath) as? UserSettingsCell {
            cell.viewModel = vm
            cell.selectionStyle = .none
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
        return headerView(NSLocalizedString(viewModel.sections[section].rawValue, comment: ""))
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let item = viewModel.getCellModel(at: indexPath)
        switch item?.type {
        case .Logout:
            delegate?.userLogout()
        case .UnifiedDenominationExchange:
            showDenominationExchange()
        case .BackUpRecoveryPhrase:
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "OnBoardInfoViewController") as? OnBoardInfoViewController {
                vc.isSettingDisplay = true
                navigationController?.pushViewController(vc, animated: true)
            }
        case .ChangePin:
            viewModel.wm?.prominentSession?.getCredentials(password: "").done { credentials in
                    let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
                    if let vc = storyboard.instantiateViewController(withIdentifier: "SetPinViewController") as? SetPinViewController {
                        vc.pinFlow = .settings
                        vc.viewModel = SetPinViewModel(credentials: credentials, testnet: self.viewModel.wm?.testnet ?? false)
                        self.navigationController?.pushViewController(vc, animated: true)
                }
            }
        case.LoginWithBiometrics:
            let vm = viewModel.getCellModel(at: indexPath)
            if let value = vm?.switcher {
                onBiometricSwitch(!value)
            }
        case .AutoLogout:
            showAutoLogout()
        case .TwoFactorAuthication:
            openTwoFactorAuthentication()
        case .PgpKey:
            openPgp()
        case .Version:
            break
        case .SupportID:

            let multiSigSessions = { WalletManager.current?.activeSessions.values.filter { !$0.gdkNetwork.electrum } }()
            let msMainSession = multiSigSessions?.filter{ $0.gdkNetwork.liquid == false }.first
            let msLiquidSession = multiSigSessions?.filter{ $0.gdkNetwork.liquid == true }.first
            guard let uuid = UserDefaults.standard.string(forKey: AppStorage.analyticsUUID) else { return }

            var str = ""
            str += "id:\(uuid)"

            var promises: [Promise<WalletItem>] = []
            if let msMainSession = msMainSession { promises.append(msMainSession.subaccount(0)) }
            if let msLiquidSession = msLiquidSession { promises.append(msLiquidSession.subaccount(0)) }
            
            when(fulfilled: promises)
                .done{ list in
                    if let item = list.filter({$0.gdkNetwork.liquid == false}).first, item.receivingId != "" {
                        str += ",bitcoin:\(item.receivingId)"
                    }
                    if let item = list.filter({$0.gdkNetwork.liquid == true}).first, item.receivingId != ""  {
                        str += ",liquidnetwork:\(item.receivingId)"
                    }
                    UIPasteboard.general.string = str
                    DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 1.0)
                    UINotificationFeedbackGenerator().notificationOccurred(.success)
                }
                .catch { error in
                  print(error)
                }
        case .ArchievedAccounts:
            openArchivedAccounts()
        case .WatchOnly:
            let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "WatchOnlySettingsViewController") as? WatchOnlySettingsViewController {
                navigationController?.pushViewController(vc, animated: true)
            }
        case .none:
            break
        }
    }
}

extension UserSettingsViewController {
    func headerView(_ txt: String) -> UIView {
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.gBlackBg()
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 15.0, weight: .heavy)
        title.text = txt
        title.textColor = .white.withAlphaComponent(0.4)
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.bottomAnchor.constraint(equalTo: section.bottomAnchor, constant: -5),
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

    func openPgp() {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "PgpViewController") as? PgpViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func openArchivedAccounts() {
        let storyboard = UIStoryboard(name: "Accounts", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountArchiveViewController") as? AccountArchiveViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func openTwoFactorAuthentication() {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "TwoFactorAuthenticationViewController") as? TwoFactorAuthenticationViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func showDenominationExchange() {
        let ltFlow = UIStoryboard(name: "DenominationExchangeFlow", bundle: nil)
        if let vc = ltFlow.instantiateViewController(withIdentifier: "DenominationExchangeViewController") as? DenominationExchangeViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            self.present(vc, animated: false, completion: nil)
        }
    }

    func showAutoLogout() {
        guard let settings = session?.settings else { return }
        let list = [AutoLockType.minute.string, AutoLockType.twoMinutes.string, AutoLockType.fiveMinutes.string, AutoLockType.tenMinutes.string, AutoLockType.sixtyMinutes.string]
        let selected = settings.autolock.string
        let alert = UIAlertController(title: NSLocalizedString("id_auto_logout_timeout", comment: ""), message: "", preferredStyle: .actionSheet)
        list.forEach { (item: String) in
            alert.addAction(UIAlertAction(title: item, style: item == selected  ? .destructive : .default) { _ in
                settings.autolock = AutoLockType.from(item)
                self.changeSettings(settings)
                    .done { self.viewModel.load() }
                    .catch { error in self.showAlert(error) }
            })
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        self.present(alert, animated: true, completion: nil)
    }

    func changeSettings(_ settings: Settings) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().map { _ in self.startAnimating() }
            .compactMap { self.session }
            .then(on: bgq) { $0.changeSettings(settings: settings) }
            .asVoid()
            .ensure { self.stopAnimating() }
    }
}

extension UserSettingsViewController {
    private func enableBioAuth() {
        guard let account = account else { return }
        // An auth key pin should be set before updating bio auth
        if !AuthenticationTypeHandler.findAuth(method: .AuthKeyPIN, forNetwork: account.keychain) {
            onAuthError(message: NSLocalizedString("id_please_enable_pin", comment: ""))
            return
        }
        guard let session = self.session else { return }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then {
            session.getCredentials(password: "")
        }.then(on: bgq) {
            account.addBiometrics(session: session, credentials: $0)
        }.ensure {
            self.stopAnimating()
        }.done {
            self.viewModel.load()
        }.catch { error in
            if error is GaError {
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
            self?.viewModel.load()
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
            self.account?.removeBioKeychainData()
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
extension UserSettingsViewController: UserSettingsViewControllerDelegate, TwoFactorAuthenticationViewControllerDelegate {
    func userLogout() {
        self.delegate?.userLogout()
    }
    func refresh() {
        self.viewModel.load()
        self.delegate?.refresh()
    }
}

extension UserSettingsViewController: DialogWatchOnlySetUpViewControllerDelegate {
    func watchOnlyDidUpdate(_ action: WatchOnlySetUpAction) {
        switch action {
        case .save, .delete:
            viewModel.load()
        default:
            break
        }
    }
}

extension UserSettingsViewController: DenominationExchangeViewControllerDelegate {
    func onDenominationExchangeSave() {
        self.viewModel.load()
        self.delegate?.refresh()
    }
}
