import UIKit
import PromiseKit

enum AccountSection: Int, CaseIterable {
    case account
    case adding
    case disclose
    case assets
    case transaction
    case footer
}

enum AccountPreferences: String, CaseIterable {
    case Rename = "Rename"
    case Archive = "Archive"
    case EnhanceSecurity = "Enhance Security"
}

class AccountViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var actionsBg: UIView!
    @IBOutlet weak var btnSend: UIButton!
    @IBOutlet weak var btnReceive: UIButton!

    private var headerH: CGFloat = 54.0
    private var cardH: CGFloat = 64.0
    private var cardHc: CGFloat = 184.0

    private var sIdx: Int = 0

    var viewModel: AccountViewModel!

    override func viewDidLoad() {
        super.viewDidLoad()

        ["AccountCell", "WalletAssetCell", "TransactionCell", "AddingCell", "DiscloseCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }

        setContent()
        setStyle()
        tableView.selectRow(at: IndexPath(row: sIdx, section: AccountSection.account.rawValue), animated: false, scrollPosition: .none)

        viewModel?.reloadSections = reloadSections
        viewModel.getBalance()
        viewModel.getTransactions()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
    }

    func reloadSections(_ sections: [AccountSection], animated: Bool) {
        if animated {
            tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
        } else {
            UIView.performWithoutAnimation {
                tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            }
        }
        if sections.contains(AccountSection.account) {
            tableView.selectRow(at: IndexPath(row: sIdx, section: AccountSection.account.rawValue), animated: false, scrollPosition: .none)
        }
    }

    func setContent() {

        // setup right menu bar: settings
        let settingsBtn = UIButton(type: .system)
        settingsBtn.setImage(UIImage(named: "ic_gear"), for: .normal)
        settingsBtn.addTarget(self, action: #selector(settingsBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)

        btnSend.setTitle( "id_send".localized, for: .normal )
        btnReceive.setTitle( "id_receive".localized, for: .normal )

        tableView.prefetchDataSource = self
        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)

    }

    func setStyle() {
        actionsBg.layer.cornerRadius = 5.0
    }

    // tableview refresh gesture
    @objc func handleRefresh(_ sender: UIRefreshControl? = nil) {
    }

    // open settings
    @objc func settingsBtnTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: "Account Preferences", type: .accountPrefs, items: AccountPrefs.getItems())
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    func twoFactorAuthenticatorDialog() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: "Enable 2FA", type: .enable2faPrefs, items: Enable2faPrefs.getItems())
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    // open send flow
    func sendfromWallet() {
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
            vc.wallet = viewModel.account
            vc.fixedWallet = true
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    // open receive screen
    func receiveScreen() {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "ReceiveViewController") as? ReceiveViewController {
            guard let account = viewModel.account else { return }
            vc.viewModel = ReceiveViewModel(account: account,
                                            accounts: [account])
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func renameDialog() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogWalletNameViewController") as? DialogWalletNameViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.isAccountRename = true
            vc.delegate = self
            vc.index = nil
            present(vc, animated: false, completion: nil)
        }
    }

    func rename(name: String) {
        firstly { self.startLoader(); return Guarantee() }
            .then { self.viewModel.renameSubaccount(name: name) }
            .ensure { self.stopLoader() }
            .done { }
            .catch { err in self.showError(err) }
    }

    func archive() {
        firstly { self.startLoader(message: "Archiving"); return Guarantee() }
            .then { self.viewModel.archiveSubaccount() }
            .ensure { self.stopLoader() }
            .done {
                DropAlert().success(message: "Account Archived")
                self.navigationController?.popViewController(animated: true)
            } .catch { err in self.showError(err) }
    }

    func navigateTo2fa(_ account: WalletItem) {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "MultisigSettingsViewController") as? MultisigSettingsViewController {
            vc.session = account.session
            if let vc2 = storyboard.instantiateViewController(withIdentifier: "TwoFactorAuthenticationViewController") as? TwoFactorAuthenticationViewController {
                vc2.delegate = vc
                vc2.session = vc.session
                if var viewControllers = navigationController?.viewControllers {
                    viewControllers.append(vc)
                    viewControllers.append(vc2)
                    navigationController?.setViewControllers(viewControllers, animated: true)
                }
            }
        }
    }

    @IBAction func btnSend(_ sender: Any) {
        sendfromWallet()
    }

    @IBAction func btnReceive(_ sender: Any) {
        receiveScreen()
    }

}

extension AccountViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return AccountSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch AccountSection(rawValue: section) {
        case .account:
            return viewModel.accountCellModels.count
        case .adding:
            return viewModel.addingCellModels.count
        case .disclose:
            return viewModel.discloseCellModels.count
        case .assets:
            return viewModel.assetCellModels.count
        case .transaction:
            return viewModel.txCellModels.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch AccountSection(rawValue: indexPath.section) {
        case .account:
            if let cell = tableView.dequeueReusableCell(withIdentifier: AccountCell.identifier, for: indexPath) as? AccountCell {
                let model = viewModel.accountCellModels[indexPath.row]

                let onCopy: (() -> Void)? = {
                    UIPasteboard.general.string = model.account.receivingId
                    DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 2.0)
                }
                cell.configure(model: model,
                               cIdx: indexPath.row,
                               sIdx: sIdx,
                               isLast: true,
                               onSelect: nil,
                               onCopy: onCopy,
                               onShield: nil)
                cell.selectionStyle = .none
                return cell
            }
        case .adding:
            if let cell = tableView.dequeueReusableCell(withIdentifier: AddingCell.identifier, for: indexPath) as? AddingCell {
                cell.configure(model: viewModel.addingCellModels[indexPath.row])
                cell.selectionStyle = .none
                return cell
            }
        case .disclose:
            if let cell = tableView.dequeueReusableCell(withIdentifier: DiscloseCell.identifier, for: indexPath) as? DiscloseCell {
                cell.configure(model: viewModel.discloseCellModels[indexPath.row])
                cell.selectionStyle = .none
                return cell
            }
        case .assets:
            if let cell = tableView.dequeueReusableCell(withIdentifier: WalletAssetCell.identifier, for: indexPath) as? WalletAssetCell {
                cell.configure(model: viewModel.assetCellModels[indexPath.row])
                cell.selectionStyle = .none
                return cell
            }
        case .transaction:
            if let cell = tableView.dequeueReusableCell(withIdentifier: TransactionCell.identifier, for: indexPath) as? TransactionCell {
                cell.configure(model: viewModel.txCellModels[indexPath.row])
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch AccountSection(rawValue: section) {
        case .transaction, .assets:
            return headerH
        default:
            return 0.1
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        switch AccountSection(rawValue: section) {
        case .footer:
            return 100.0
        default:
            return 0.1
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {

        switch AccountSection(rawValue: indexPath.section) {
        case .account:
            return indexPath.row == sIdx ? cardHc : cardH
        default:
            return UITableView.automaticDimension
        }
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {

        switch AccountSection(rawValue: section) {
        case .transaction:
            return headerView( "Latest transactions" )
        case .assets:
            return headerView( "Balance" )
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {

        return nil
    }

    func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        switch AccountSection(rawValue: indexPath.section) {
        case .account, .assets:
            return nil
        default:
            return indexPath
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        switch AccountSection(rawValue: indexPath.section) {
        case .account:
            break
        case .adding:
            twoFactorAuthenticatorDialog()
        case .disclose:
            if let url = URL(string: "https://help.blockstream.com/hc/en-us/articles/5301732614169-How-do-I-receive-AMP-assets-") {
                UIApplication.shared.open(url)
            }
        case .assets:
            break
        case .transaction:
            let transaction = viewModel?.cachedTransactions[indexPath.row]
            let storyboard = UIStoryboard(name: "Transaction", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "TransactionViewController") as? TransactionViewController {
                vc.transaction = transaction
                navigationController?.pushViewController(vc, animated: true)
            }
        default:
            break
        }
        tableView.deselectRow(at: indexPath, animated: false)
        tableView.selectRow(at: IndexPath(row: sIdx, section: AccountSection.account.rawValue), animated: false, scrollPosition: .none)
    }
}

extension AccountViewController: UITableViewDataSourcePrefetching {
   // incremental transactions fetching from gdk
    func tableView(_ tableView: UITableView, prefetchRowsAt indexPaths: [IndexPath]) {
        let filteredIndexPaths = indexPaths.filter { $0.section == AccountSection.transaction.rawValue }
        let row = filteredIndexPaths.last?.row ?? 0
        if viewModel.page > 0 && row > (viewModel.txCellModels.count - 3) {
            viewModel.getTransactions(restart: false, max: nil)
        }
    }
}
extension AccountViewController: UIPopoverPresentationControllerDelegate {

    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }

    func presentationController(_ controller: UIPresentationController, viewControllerForAdaptivePresentationStyle style: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: controller.presentedViewController)
    }
}

extension AccountViewController {

    func headerView(_ txt: String) -> UIView {

        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.clear
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 18.0, weight: .heavy)
        title.text = txt
        title.textColor = .white
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor, constant: 10.0),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 20),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: 20)
        ])

        return section
    }
}

extension AccountViewController: DialogListViewControllerDelegate {
    func didSelectIndex(_ index: Int, with type: DialogType) {
        switch type {
        case .accountPrefs:
            switch AccountPrefs(rawValue: index) {
            case .rename:
                renameDialog()
            case .archive:
                archive()
            //case .enhanceSecurity:
            //    twoFactorAuthenticatorDialog()
            case .none:
                break
            }
        case .enable2faPrefs:
            switch Enable2faPrefs(rawValue: index) {
            case .add:
                let session = viewModel.account.session
                let enabled2FA = session?.twoFactorConfig?.anyEnabled ?? false
                let isSS = session?.gdkNetwork.electrum ?? false
                if isSS {
                    showError("Two factor authentication is not availabled for singlesig account")
                    return
                } else if enabled2FA {
                    showError("Two factor authentication is just enabled")
                    return
                }
                navigateTo2fa(viewModel.account)
//                let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
//                if let vc = storyboard.instantiateViewController(withIdentifier: "MultisigSettingsViewController") as? MultisigSettingsViewController {
//                    vc.session = viewModel.account.session
//                    navigationController?.pushViewController(vc, animated: true)
//                }
            default:
                break
            }
        default:
            break
        }
    }
}

extension AccountViewController: DialogWalletNameViewControllerDelegate {
    func didRename(name: String, index: Int?) {
        rename(name: name)
    }
    func didCancel() {
    }
}
