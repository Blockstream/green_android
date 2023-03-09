import UIKit

enum HomeSection: Int, CaseIterable {
    case remoteAlerts = 0
    case swWallet = 1
    case ephWallet = 2
    case hwWallet = 3
    case device = 4
}

class HomeViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!

    @IBOutlet weak var btnAbout: UIButton!
    @IBOutlet weak var btnSettings: UIButton!

    var headerH: CGFloat = 44.0
    var footerH: CGFloat = 54.0

    private var remoteAlert: RemoteAlert?

    private var ephAccounts: [Account] {
        AccountsRepository.shared.ephAccounts.filter { account in
            account.isEphemeral && !WalletsRepository.shared.wallets.filter {$0.key == account.id }.isEmpty
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        updateUI()
        view.accessibilityIdentifier = AccessibilityIdentifiers.HomeScreen.view
        btnSettings.accessibilityIdentifier = AccessibilityIdentifiers.HomeScreen.appSettingsBtn

        tableView.register(UINib(nibName: "WalletListCell", bundle: nil), forCellReuseIdentifier: "WalletListCell")
        tableView.register(UINib(nibName: "WalletListHDCell", bundle: nil), forCellReuseIdentifier: "WalletListHDCell")
        tableView.register(UINib(nibName: "WalletListEmptyCell", bundle: nil), forCellReuseIdentifier: "WalletListEmptyCell")
        tableView.register(UINib(nibName: "AlertCardCell", bundle: nil), forCellReuseIdentifier: "AlertCardCell")

        self.remoteAlert = RemoteAlertManager.shared.alerts(screen: .home, networks: []).first

        AnalyticsManager.shared.delegate = self
        AnalyticsManager.shared.recordView(.home)
        AnalyticsManager.shared.appLoadingFinished()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        tableView.reloadData()
    }

    func setContent() {
        btnSettings.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)
        btnSettings.setTitleColor(.lightGray, for: .normal)
        btnAbout.setTitle(NSLocalizedString("id_about", comment: ""), for: .normal)
        btnAbout.setImage(UIImage(named: "ic_about")!, for: .normal)
        btnAbout.setTitleColor(.lightGray, for: .normal)
    }

    func setStyle() {
    }

    func updateUI() {
    }

    func enterWallet(_ account: Account) {
        AccountNavigator.goLogin(account: account)
    }

    func showHardwareWallet(_ index: Int) {
        let account = AccountsRepository.shared.devices[index]
        AccountNavigator.goHWLogin(isJade: account.isJade)
    }

    func remoteAlertDismiss() {
        remoteAlert = nil
        tableView.reloadData()
    }

    func remoteAlertLink() {
        SafeNavigationManager.shared.navigate(remoteAlert?.link)
    }

    @objc func didPressAddWallet() {
        AccountNavigator.goCreateRestore()
    }

    func walletDelete(_ index: Int) {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogWalletDeleteViewController") as? DialogWalletDeleteViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            vc.index = index
            present(vc, animated: false, completion: nil)
        }
    }

    func walletRename(_ index: Int) {
        guard let account = AccountsRepository.shared.swAccounts[safe: index] else { return }
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogWalletNameViewController") as? DialogWalletNameViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            vc.index = index
            vc.prefill = account.name
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnAbout(_ sender: Any) {

        let storyboard = UIStoryboard(name: "About", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AboutViewController") as? AboutViewController {
            self.navigationController?.pushViewController(vc, animated: true)
        }
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
        return HomeSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case HomeSection.remoteAlerts.rawValue:
            return remoteAlert != nil ? 1 : 0
        case HomeSection.swWallet.rawValue:
            return AccountsRepository.shared.swAccounts.count == 0 ? 1 : AccountsRepository.shared.swAccounts.count
        case HomeSection.ephWallet.rawValue:
            return ephAccounts.count
        case HomeSection.hwWallet.rawValue:
            return AccountsRepository.shared.hwAccounts.count
        case HomeSection.device.rawValue:
            return AccountsRepository.shared.devices.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {

        case HomeSection.remoteAlerts.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AlertCardCell", for: indexPath) as? AlertCardCell, let remoteAlert = self.remoteAlert {
                cell.configure(AlertCardCellModel(type: .remoteAlert(remoteAlert)),
                               onLeft: nil,
                               onRight: (remoteAlert.link ?? "" ).isEmpty ? nil : {[weak self] in
                    self?.remoteAlertLink() // to solve cylomatic complexity
                },
                               onDismiss: {[weak self] in
                    self?.remoteAlertDismiss()
                })
                cell.selectionStyle = .none
                return cell
            }
        case HomeSection.swWallet.rawValue:
            if AccountsRepository.shared.swAccounts.count == 0 {
                if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletListEmptyCell") as? WalletListEmptyCell {
                    cell.configure(NSLocalizedString("id_it_looks_like_you_have_no", comment: ""), UIImage(named: "ic_logo_green")!)
                    cell.selectionStyle = .none
                    return cell
                }
            } else {
                let account = AccountsRepository.shared.swAccounts[indexPath.row]
                if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletListCell") as? WalletListCell {
                    let selected = { () -> Bool in
                        return WalletsRepository.shared.get(for: account.id)?.activeSessions.count ?? 0 > 0
                    }
                    cell.configure(item: account,
                                   isSelected: selected(),
                                   onLongpress: { [weak self] () in
                        if !(WalletsRepository.shared.get(for: account.id)?.activeSessions.isEmpty == false) {
                            UINotificationFeedbackGenerator().notificationOccurred(.success)

                            let storyboard = UIStoryboard(name: "PopoverMenu", bundle: nil)
                            if let popover  = storyboard.instantiateViewController(withIdentifier: "PopoverMenuWalletViewController") as? PopoverMenuWalletViewController {
                                popover.delegate = self
                                popover.index = indexPath.row
                                popover.menuOptions = [.edit, .delete]
                                popover.modalPresentationStyle = .popover
                                let popoverPresentationController = popover.popoverPresentationController
                                popoverPresentationController?.backgroundColor = UIColor.customModalDark()
                                popoverPresentationController?.delegate = self
                                popoverPresentationController?.sourceView = cell
                                popoverPresentationController?.sourceRect = cell.bounds
                                self?.present(popover, animated: true)
                            }
                        }
                    })
                    cell.selectionStyle = .none
                    return cell
                }
            }
        case HomeSection.ephWallet.rawValue:
            let account = ephAccounts[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletListCell") as? WalletListCell {
                let selected = { () -> Bool in
                    return WalletsRepository.shared.get(for: account.id)?.activeSessions.count ?? 0 > 0
                }
                cell.configure(item: account, isSelected: selected())
                cell.selectionStyle = .none
                return cell
            }
        case HomeSection.hwWallet.rawValue:
            let account = AccountsRepository.shared.hwAccounts[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletListCell") as? WalletListCell {
                let selected = { () -> Bool in
                    return WalletsRepository.shared.get(for: account.id)?.activeSessions.count ?? 0 > 0
                }
                cell.configure(item: account, isSelected: selected())
                cell.selectionStyle = .none
                return cell
            }
        case HomeSection.device.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "WalletListHDCell") as? WalletListHDCell {
                let hw = AccountsRepository.shared.devices[indexPath.row]
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
        if section == HomeSection.ephWallet.rawValue && ephAccounts.isEmpty {
            return 0.1
        }
        if section == HomeSection.hwWallet.rawValue && AccountsRepository.shared.hwAccounts.isEmpty {
            return 0.1
        }
        if section == HomeSection.remoteAlerts.rawValue {
            return 0.1
        }
        return headerH
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        switch section {
        case HomeSection.swWallet.rawValue:
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
        case HomeSection.remoteAlerts.rawValue:
            return nil
        case HomeSection.swWallet.rawValue:
            return headerView(NSLocalizedString("id_wallets", comment: "").uppercased())
        case HomeSection.ephWallet.rawValue:
            if ephAccounts.isEmpty {
                return UIView()
            }
            return headerView(NSLocalizedString("id_ephemeral_wallets", comment: "").uppercased())
        case HomeSection.hwWallet.rawValue:
            if AccountsRepository.shared.hwAccounts.isEmpty {
                return UIView()
            }
            return headerView(NSLocalizedString("id_hardware_wallets", comment: "").uppercased())
        case HomeSection.device.rawValue:
            return headerView(NSLocalizedString("id_devices", comment: "").uppercased())
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        switch section {
        case HomeSection.swWallet.rawValue:
            return footerView(NSLocalizedString("id_add_wallet", comment: ""))
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        switch indexPath.section {
        case HomeSection.remoteAlerts.rawValue:
            break
        case HomeSection.swWallet.rawValue:
            if AccountsRepository.shared.swAccounts.count > 0 {
                let account = AccountsRepository.shared.swAccounts[indexPath.row]
                enterWallet(account)
            }
        case HomeSection.ephWallet.rawValue:
            let account = ephAccounts[indexPath.row]
            enterWallet(account)
        case HomeSection.hwWallet.rawValue:
            if AccountsRepository.shared.hwAccounts.count > 0 {
                let account = AccountsRepository.shared.hwAccounts[indexPath.row]
                enterWallet(account)
            }
        case HomeSection.device.rawValue:
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

extension HomeViewController: AnalyticsManagerDelegate {
    func remoteConfigIsReady() {
        DispatchQueue.main.async {
            self.remoteAlert = RemoteAlertManager.shared.alerts(screen: .home, networks: []).first
            self.tableView.reloadData()
        }
    }
}

extension HomeViewController: UIPopoverPresentationControllerDelegate {

    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }

    func presentationController(_ controller: UIPresentationController, viewControllerForAdaptivePresentationStyle style: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: controller.presentedViewController)
    }
}

extension HomeViewController: PopoverMenuWalletDelegate {
    func didSelectionMenuOption(menuOption: MenuWalletOption, index: Int?) {
        guard let index = index else { return }
        switch menuOption {
        case .edit:
            walletRename(index)
        case .delete:
            walletDelete(index)
        default:
            break
        }
    }
}

extension HomeViewController: DialogWalletNameViewControllerDelegate, DialogWalletDeleteViewControllerDelegate {
    func didRename(name: String, index: Int?) {
        if let index = index, var account = AccountsRepository.shared.swAccounts[safe: index] {
            account.name = name
            AccountsRepository.shared.upsert(account)
            AnalyticsManager.shared.renameWallet()
            tableView.reloadData()
        }
    }
    func didDelete(_ index: Int?) {
        guard let index = index, let account = AccountsRepository.shared.swAccounts[safe: index] else { return }
        AccountsRepository.shared.remove(account)
        AnalyticsManager.shared.deleteWallet()
        tableView.reloadData()
    }
    func didCancel() {
    }
}
