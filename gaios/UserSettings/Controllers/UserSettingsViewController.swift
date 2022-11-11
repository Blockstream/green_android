import UIKit
import PromiseKit

protocol UserSettingsViewControllerDelegate: AnyObject {
    func userLogout()
    func refresh()
}

class UserSettingsViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    weak var delegate: UserSettingsViewControllerDelegate?

    var account = { AccountsManager.shared.current }()
    var session = { WalletManager.current?.prominentSession }()
    var headerH: CGFloat = 54.0
    var viewModel = UserSettingsViewModel()

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
    /*
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let item = viewModel.getCellModelsForSection(at: section)

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
    }*/

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
        case .BitcoinDenomination:
            showBitcoinDenomination()
        case .ReferenceExchangeRate:
            let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "CurrencySelectorViewController") as? CurrencySelectorViewController {
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        case .BackUpRecoveryPhrase:
            let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "MnemonicAuthViewController")
            navigationController?.pushViewController(vc, animated: true)
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
        case .Bitcoin:
            openMultisig(network: .bitcoin)
        case .Liquid:
            openMultisig(network: .liquid)
        case .Version:
            break
        case .SupportID:
            session?.subaccount(0).done { wallet in
                UIPasteboard.general.string = wallet.receivingId
                DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 1.0)
                UINotificationFeedbackGenerator().notificationOccurred(.success)
            }.catch { _ in }
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

    func openMultisig(network: AvailableNetworks) {
        guard network == .liquid || network == .bitcoin else {
            return
        }
        guard let session = WalletManager.current?.sessions[network.rawValue] else {
            showAlert(title: network.name(), message: "Multisig wallet not created")
            return
        }
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "MultisigSettingsViewController") as? MultisigSettingsViewController {
            vc.session = session
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func showBitcoinDenomination() {
        let list = [ .BTC, .MilliBTC, .MicroBTC, .Bits, .Sats].map { DenominationType.denominations[$0]! }
        guard let settings = session?.settings else { return }
        let selected = settings.denomination.string
        let alert = UIAlertController(title: NSLocalizedString("id_bitcoin_denomination", comment: ""), message: "", preferredStyle: .actionSheet)
        list.forEach { (item: String) in
            alert.addAction(UIAlertAction(title: item, style: item == selected  ? .destructive : .default) { _ in
                settings.denomination = DenominationType.from(item)
                self.changeSettings(settings)
                    .done {
                        self.viewModel.load()
                        self.delegate?.refresh()
                    }.catch { error in
                        self.showAlert(error)
                    }
            })
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        self.present(alert, animated: true, completion: nil)
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
            .ensure { self.stopAnimating() }
    }
}

extension UserSettingsViewController {
    private func enableBioAuth() {
        // An auth key pin should be set before updating bio auth
        if !AuthenticationTypeHandler.findAuth(method: .AuthKeyPIN, forNetwork: self.account!.keychain) {
            onAuthError(message: NSLocalizedString("id_please_enable_pin", comment: ""))
            return
        }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap {
            self.session
        }.compactMap(on: bgq) {
            self.account?.addBioPin(session: $0)
        }.ensure {
            self.stopAnimating()
        }.catch { error in
            if let _ = error as? GaError {
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
extension UserSettingsViewController: UserSettingsViewControllerDelegate, TwoFactorAuthenticationViewControllerDelegate {
    func userLogout() {
        self.delegate?.userLogout()
    }
    func refresh() {
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
