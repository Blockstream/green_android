import UIKit

class HomeViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var lblVersion: UILabel!
    @IBOutlet weak var btnSettings: UIButton!

    var headerH: CGFloat = 44.0
    var footerH: CGFloat = 54.0

    private var ephAccounts: [Account] {
        AccountsManager.shared.ephAccounts.filter { account in
            account.isEphemeral && !SessionsManager.shared.filter {$0.key == account.id }.isEmpty
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        updateUI()
        view.accessibilityIdentifier = AccessibilityIdentifiers.HomeScreen.view
        btnSettings.accessibilityIdentifier = AccessibilityIdentifiers.HomeScreen.appSettingsBtn

        AnalyticsManager.shared.recordView(.home)
        AnalyticsManager.shared.appLoadingFinished()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        tableView.reloadData()
    }

    func setContent() {
        lblVersion.text = String(format: NSLocalizedString("id_version_1s", comment: ""), "\(Bundle.main.versionNumber)")
        btnSettings.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)
    }

    func setStyle() {
    }

    func updateUI() {
    }

    func enterWallet(_ account: Account) {
        if let session = SessionsManager.shared[account.id],
           session.connected && session.logged {
            session.subaccount(account.activeWallet).done { wallet in
                AccountsManager.shared.current = account
                let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
                let nav = storyboard.instantiateViewController(withIdentifier: "TabViewController") as? UINavigationController
                if let vc = nav?.topViewController as? ContainerViewController {
                    vc.presentingWallet = wallet
                }
                UIApplication.shared.keyWindow?.rootViewController = nav
            }.catch { err in
                print("subaccount error: \(err.localizedDescription)")
            }
            return
        }
        let homeS = UIStoryboard(name: "Home", bundle: nil)
        let onBoardS = UIStoryboard(name: "OnBoard", bundle: nil)
        if account.isWatchonly {
            if let nav = homeS.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController,
                let vc = onBoardS.instantiateViewController(withIdentifier: "WatchOnlyLoginViewController") as? WatchOnlyLoginViewController {
                    vc.account = account
                    nav.pushViewController(vc, animated: false)
                    UIApplication.shared.keyWindow?.rootViewController = nav
            }
            return
        }
        if let nav = homeS.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController,
            let vc = homeS.instantiateViewController(withIdentifier: "LoginViewController") as? LoginViewController {
                vc.account = account
                nav.pushViewController(vc, animated: false)
                UIApplication.shared.keyWindow?.rootViewController = nav
        }
    }

    func showHardwareWallet(_ index: Int) {
        let storyboard = UIStoryboard(name: "HWW", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "HWWScanViewController") as? HWWScanViewController {
            vc.jade = AccountsManager.shared.devices[index].isJade
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    @objc func didPressAddWallet() {

//        let stb = UIStoryboard(name: "Shared", bundle: nil)
//        if let vc = stb.instantiateViewController(withIdentifier: "DialogCountlyConsentViewController") as? DialogCountlyConsentViewController {
//            vc.modalPresentationStyle = .overFullScreen
//            present(vc, animated: false, completion: nil)
//        }
//        return

        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "LandingViewController")
        navigationController?.pushViewController(vc, animated: true)
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
            present(vc, animated: true) {}
        }
    }
}

extension HomeViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 4
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case 0:
            return AccountsManager.shared.swAccounts.count == 0 ? 1 : AccountsManager.shared.swAccounts.count
        case 1:
            return ephAccounts.count
        case 2:
            return AccountsManager.shared.hwAccounts.count
        case 3:
            return AccountsManager.shared.devices.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case 0:
            if AccountsManager.shared.swAccounts.count == 0 {
                if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletEmptyCell") as? WalletEmptyCell {
                    cell.configure(NSLocalizedString("id_it_looks_like_you_have_no", comment: ""), UIImage(named: "ic_logo_green")!)
                    cell.selectionStyle = .none
                    return cell
                }
            } else {
                let account = AccountsManager.shared.swAccounts[indexPath.row]
                if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletCell") as? WalletCell {
                    let selected = { () -> Bool in
                        if let session = SessionsManager.get(for: account) {
                            return session.connected && session.logged
                        }
                        return false
                    }
                    cell.configure(account, selected())
                    cell.selectionStyle = .none
                    return cell
                }
            }
        case 1: /// EPHEMERAL
            let account = ephAccounts[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletCell") as? WalletCell {
                let selected = { () -> Bool in
                    if let session = SessionsManager.get(for: account) {
                        return session.connected && session.logged
                    }
                    return false
                }
                cell.configure(account, selected())
                cell.selectionStyle = .none
                return cell
            }
        case 2:
            let account = AccountsManager.shared.hwAccounts[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletCell") as? WalletCell {
                let selected = { () -> Bool in
                    if let session = SessionsManager.get(for: account) {
                        return session.connected && session.logged
                    }
                    return false
                }
                cell.configure(account, selected())
                cell.selectionStyle = .none
                return cell
            }
        case 3:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletHDCell") as? WalletHDCell {
                let hw = AccountsManager.shared.devices[indexPath.row]
                let icon = UIImage(named: hw.isJade ? "blockstreamIcon" : "ledgerIcon")
                cell.configure(hw.name, icon ?? UIImage())
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        if section == 1 && ephAccounts.isEmpty {
            return 0.1
        }
        if section == 2 && AccountsManager.shared.hwAccounts.isEmpty {
            return 0.1
        }
        return headerH
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        switch section {
        case 0:
            return footerH
        default:
            return 0.1
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        switch section {
        case 0:
            return headerView(NSLocalizedString("id_wallets", comment: "").uppercased())
        case 1:
            if ephAccounts.isEmpty {
                return UIView()
            }
            return headerView(NSLocalizedString("id_ephemeral_wallets", comment: "").uppercased())
        case 2:
            if AccountsManager.shared.hwAccounts.isEmpty {
                return UIView()
            }
            return headerView(NSLocalizedString("id_hardware_wallets", comment: "").uppercased())
        case 3:
            return headerView(NSLocalizedString("id_devices", comment: "").uppercased())
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        switch section {
        case 0:
            return footerView(NSLocalizedString("id_add_wallet", comment: ""))
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        switch indexPath.section {
        case 0:
            if AccountsManager.shared.swAccounts.count > 0 {
                let account = AccountsManager.shared.swAccounts[indexPath.row]
                enterWallet(account)
            }
        case 1:
            let account = ephAccounts[indexPath.row]
            enterWallet(account)
        case 2:
            if AccountsManager.shared.hwAccounts.count > 0 {
                let account = AccountsManager.shared.hwAccounts[indexPath.row]
                enterWallet(account)
            }
        case 3:
            showHardwareWallet(indexPath.row)
        default:
            break
        }
    }
}

extension HomeViewController {
    func headerView(_ txt: String) -> UIView {
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.customTitaniumDark()
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 14.0, weight: .semibold)
        title.text = txt
        title.textColor = UIColor.customGrayLight()
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 24),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -24)
        ])

        return section
    }

    func footerView(_ txt: String) -> UIView {
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: footerH))
        section.backgroundColor = UIColor.customTitaniumDark()

        let icon = UIImageView(frame: .zero)
        icon.image = UIImage(named: "ic_plus")?.maskWithColor(color: .white)
        icon.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(icon)

        let title = UILabel(frame: .zero)
        title.text = txt
        title.textColor = .white
        title.font = .systemFont(ofSize: 17.0, weight: .semibold)
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            icon.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            icon.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 16),
            icon.widthAnchor.constraint(equalToConstant: 40.0),
            icon.heightAnchor.constraint(equalToConstant: 40.0)
        ])

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: (40 + 16 * 2)),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -24)
        ])

        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(didPressAddWallet))
        section.addGestureRecognizer(tapGesture)
        section.accessibilityIdentifier = AccessibilityIdentifiers.HomeScreen.addWalletView
        return section
    }
}
