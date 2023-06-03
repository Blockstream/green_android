import UIKit
import PromiseKit
import BreezSDK
import lightning
import gdk

enum AccountSection: Int, CaseIterable {
    case account
    case adding
    case disclose
    case inbound
    case assets
    case transaction
    case footer
}

protocol AccountViewControllerDelegate: AnyObject {
    func didArchiveAccount()
}

class AccountViewController: UIViewController {

    enum FooterType {
        case noTransactions
        case none
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var actionsBg: UIView!
    @IBOutlet weak var btnSend: UIButton!
    @IBOutlet weak var btnReceive: UIButton!
    @IBOutlet weak var btnScanView: UIView!
    @IBOutlet weak var divider: UIView!
    
    private var headerH: CGFloat = 54.0
    private var footerH: CGFloat = 54.0
    private var cardH: CGFloat = 64.0
    private var cardHc: CGFloat = 184.0

    weak var delegate: AccountViewControllerDelegate?
    var viewModel: AccountViewModel!

    private var sIdx: Int = 0
    private var notificationObservers: [NSObjectProtocol] = []

    private var hideBalance: Bool {
        return UserDefaults.standard.bool(forKey: AppStorage.hideBalance)
    }

    var showScan = true

    override func viewDidLoad() {
        super.viewDidLoad()

        btnScanView.isHidden = !showScan
        divider.isHidden = showScan

        register()
        setContent()
        setStyle()
        tableView.selectRow(at: IndexPath(row: sIdx, section: AccountSection.account.rawValue), animated: false, scrollPosition: .none)

        viewModel.reloadSections = reloadSections
        viewModel.getBalance()
        viewModel.getTransactions()

        AnalyticsManager.shared.recordView(.accountOverview, sgmt: AnalyticsManager.shared.sessSgmt(AccountsRepository.shared.current))
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        reloadSections([AccountSection.assets, AccountSection.adding, AccountSection.disclose], animated: false)

        EventType.allCases.forEach {
            let observer = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: $0.rawValue),
                                                                  object: nil,
                                                                  queue: .main,
                                                                  using: { [weak self] data in
                self?.viewModel.handleEvent(data)
            })
            notificationObservers.append(observer)
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        notificationObservers.forEach { observer in
            NotificationCenter.default.removeObserver(observer)
        }
        notificationObservers = []
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
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.5) {
            self.tableView.refreshControl?.endRefreshing()
        }
    }

    func reloadFromParent(_ model: AccountCellModel) {
        viewModel.accountCellModels = [model]
        viewModel.getBalance()
        viewModel.getTransactions(restart: true)
    }

    func register() {
        ["AccountCell", "WalletAssetCell", "TransactionCell",
         "AddingCell", "DiscloseCell", "LTInboundCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
    }

    func setContent() {

        navigationItem.rightBarButtonItems = []

        // setup right menu bar: settings
        let settingsBtn = UIButton(type: .system)
        settingsBtn.contentEdgeInsets = UIEdgeInsets(top: 7.0, left: 7.0, bottom: 7.0, right: 7.0)
        settingsBtn.setImage(UIImage(named: "ic_gear"), for: .normal)
        settingsBtn.addTarget(self, action: #selector(settingsBtnTapped), for: .touchUpInside)
        if !viewModel.watchOnly {
            navigationItem.rightBarButtonItems?.append( UIBarButtonItem(customView: settingsBtn) )
        }

        let ampHelpBtn = UIButton(type: .system)
        ampHelpBtn.setImage(UIImage(named: "ic_help"), for: .normal)
        ampHelpBtn.addTarget(self, action: #selector(ampHelp), for: .touchUpInside)
        if viewModel.ampEducationalMode == .header {
            navigationItem.rightBarButtonItems?.append( UIBarButtonItem(customView: ampHelpBtn) )
        }

        btnSend.setTitle( "id_send".localized, for: .normal )
        btnReceive.setTitle( "id_receive".localized, for: .normal )

        // Sweep is only supported in watch-only for btc multisig wallets
        if viewModel.watchOnly {
            if let account = AccountsRepository.shared.current,
               !account.gdkNetwork.electrum && !account.gdkNetwork.liquid {
                   btnSend.setTitle( "id_sweep".localized, for: .normal )
                   btnSend.setImage(UIImage(named: "qr_sweep"), for: .normal)
               } else {
                   btnSend.isEnabled = false
                   btnSend.setTitleColor(.white.withAlphaComponent(0.5), for: .normal)
               }
        }

        tableView.prefetchDataSource = self
        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(callPullToRefresh(_:)), for: .valueChanged)

    }

    func setStyle() {
        actionsBg.layer.cornerRadius = 5.0
        btnScanView.layer.cornerRadius = 10.0
    }

    // tableview refresh gesture
    @objc func callPullToRefresh(_ sender: UIRefreshControl? = nil) {
        viewModel?.getBalance()
        viewModel.getTransactions(restart: true)
    }

    // open settings
    @objc func settingsBtnTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: "Account Preferences", type: .accountPrefs, items: AccountPrefs.getItems(isLightning: viewModel.isLightning))
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    func twoFactorAuthenticatorDialog() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: "id_enable_2fa".localized, type: .enable2faPrefs, items: Enable2faPrefs.getItems())
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    // open send flow
    func sendfromWallet() {
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
            vc.viewModel = SendViewModel(account: viewModel.account,
                                         inputType: viewModel.watchOnly ? .sweep : .transaction,
                                         transaction: nil,
                                         input: nil)
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
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogRenameViewController") as? DialogRenameViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.isAccountRename = true
            vc.delegate = self
            vc.index = nil
            vc.prefill = viewModel.account.localizedName
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

    func removeSubaccount() {
        firstly { self.startLoader(); return Guarantee() }
            .then { self.viewModel.removeSubaccount() }
            .ensure { self.stopLoader() }
            .done {
                self.delegate?.didArchiveAccount()
                self.navigationController?.popViewController(animated: true)
            }
            .catch { err in self.showError(err) }
    }

    func archive() {
        firstly { self.startLoader(message: "Archiving"); return Guarantee() }
            .then { self.viewModel.archiveSubaccount() }
            .ensure { self.stopLoader() }
            .done {
//                DropAlert().success(message: "id_account_has_been_archived".localized)
                self.delegate?.didArchiveAccount()
                self.showDialog()
            } .catch { err in self.showError(err) }
    }

    func showDialog() {
        let storyboard = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountArchivedViewController") as? AccountArchivedViewController {
            vc.delegate = self
            vc.modalPresentationStyle = .overFullScreen
            self.present(vc, animated: false, completion: nil)
        }
    }

    func navigateTo2fa(_ account: WalletItem) {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "TwoFactorAuthenticationViewController") as? TwoFactorAuthenticationViewController {
            vc.showBitcoin = !account.gdkNetwork.liquid
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    func navigateToBip85Mnemonic() {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "OnBoardInfoViewController") as? OnBoardInfoViewController {
            vc.isSettingDisplay = true
            vc.showBip85 = true
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func presentNodeInfo() {
        guard let lightningSession = viewModel.account.lightningSession else { return }

        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogNodeViewController") as? DialogNodeViewController {
            vc.viewModel = DialogNodeViewModel(lightningSession: lightningSession)
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    func showExperimental() {
        let ltFlow = UIStoryboard(name: "LTFlow", bundle: nil)
        if let vc = ltFlow.instantiateViewController(withIdentifier: "LTExperimentalViewController") as? LTExperimentalViewController {
            vc.modalPresentationStyle = .overFullScreen
            self.present(vc, animated: false, completion: nil)
        }
    }

    @objc func ampHelp() {
        if let url = URL(string: "https://help.blockstream.com/hc/en-us/articles/5301732614169-How-do-I-receive-AMP-assets-") {
            UIApplication.shared.open(url)
        }
    }

    func onInboundInfo() {
        SafeNavigationManager.shared.navigate( ExternalUrls.helpReceiveCapacity )
    }

    @IBAction func btnSend(_ sender: Any) {
        sendfromWallet()
    }

    @IBAction func btnReceive(_ sender: Any) {
        receiveScreen()
    }

    @IBAction func btnQr(_ sender: Any) {
        if let vc = DialogScanViewController.vc {
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
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
        case .inbound:
            return viewModel.inboundCellModels.count
        case .assets:
            return viewModel.showAssets ? viewModel.assetCellModels.count : 0
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
                               hideBalance: hideBalance,
                               isLast: true,
                               onSelect: nil,
                               onCopy: onCopy,
                               onShield: nil,
                               onExperiental: {[weak self] in self?.showExperimental()})
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
        case .inbound:
            if let cell = tableView.dequeueReusableCell(withIdentifier: LTInboundCell.identifier, for: indexPath) as? LTInboundCell {
                cell.configure(model: viewModel.inboundCellModels[indexPath.row], onInboundInfo: { [weak self] in
                    self?.onInboundInfo()
                })
                cell.selectionStyle = .none
                return cell
            }
        case .assets:
            if let cell = tableView.dequeueReusableCell(withIdentifier: WalletAssetCell.identifier, for: indexPath) as? WalletAssetCell {
                cell.configure(model: viewModel.assetCellModels[indexPath.row], hideBalance: hideBalance)
                cell.selectionStyle = .none
                return cell
            }
        case .transaction:
            if let cell = tableView.dequeueReusableCell(withIdentifier: TransactionCell.identifier, for: indexPath) as? TransactionCell {
                cell.configure(model: viewModel.txCellModels[indexPath.row], hideBalance: hideBalance)
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
        case .transaction: // , .assets:
            return headerH
        default:
            return 0.1
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        switch AccountSection(rawValue: section) {
        case .transaction:
            return viewModel.txCellModels.count == 0 ? footerH : 1.0
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
            return headerView("id_latest_transactions".localized)
        case .assets:
            return nil // headerView( "Balance" )
        default:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        switch AccountSection(rawValue: section) {
        case .transaction:
            return viewModel.txCellModels.count == 0 ? footerView(.noTransactions) : footerView(.none)
        default:
            return footerView(.none)
        }
    }

    func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        switch AccountSection(rawValue: indexPath.section) {
        case .account:
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
            ampHelp()
        case .inbound:
            break
        case .assets:
            let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogDetailViewController") as? DialogDetailViewController {
                if let model = viewModel?.assetCellModels[indexPath.row] {
                    vc.asset = model.asset
                    vc.tag = model.asset?.assetId ?? ""
                    vc.satoshi = model.satoshi
                }
                vc.modalPresentationStyle = .overFullScreen
                present(vc, animated: false, completion: nil)
            }
        case .transaction:
            let transaction = viewModel?.txCellModels[indexPath.row].tx
            let storyboard = UIStoryboard(name: "Transaction", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "TransactionViewController") as? TransactionViewController, let account = transaction?.subaccountItem {
                vc.transaction = transaction
                vc.wallet = account
                vc.delegate = self
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
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 25),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: 20)
        ])

        return section
    }

    func footerView(_ type: FooterType) -> UIView {

        switch type {
        case .none:
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: 1.0))
            section.backgroundColor = .clear
            return section
        case .noTransactions:
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: footerH))
            section.backgroundColor = .clear

            let lblNoTransactions = UILabel(frame: .zero)
            lblNoTransactions.font = UIFont.systemFont(ofSize: 14, weight: .regular)
            lblNoTransactions.textColor = UIColor.gGrayTxt()
            lblNoTransactions.numberOfLines = 0
            lblNoTransactions.text = NSLocalizedString("id_your_transactions_will_be_shown", comment: "")
            lblNoTransactions.translatesAutoresizingMaskIntoConstraints = false
            section.addSubview(lblNoTransactions)

            var padding: CGFloat = 50.0
            lblNoTransactions.textAlignment = .left

            if !viewModel.fetchingTxs {
                padding = 25.0
            }

            NSLayoutConstraint.activate([
                lblNoTransactions.topAnchor.constraint(equalTo: section.topAnchor, constant: 0.0),
                lblNoTransactions.bottomAnchor.constraint(equalTo: section.bottomAnchor, constant: 0.0),
                lblNoTransactions.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: padding),
                lblNoTransactions.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: 0.0)
            ])

            if viewModel.fetchingTxs {
                let loader = UIActivityIndicatorView(style: .white)
                section.addSubview(loader)
                loader.startAnimating()
                loader.translatesAutoresizingMaskIntoConstraints = false
                let horizontalConstraint = NSLayoutConstraint(item: loader,
                                                              attribute: .left,
                                                              relatedBy: .equal,
                                                              toItem: section,
                                                              attribute: .left,
                                                              multiplier: 1,
                                                              constant: 20.0)
                let verticalConstraint = NSLayoutConstraint(item: loader,
                                                            attribute: .centerY,
                                                            relatedBy: .equal,
                                                            toItem: lblNoTransactions,
                                                            attribute: .centerY,
                                                            multiplier: 1,
                                                            constant: 0)
                NSLayoutConstraint.activate([horizontalConstraint, verticalConstraint])
            }
            return section
        }

    }
}

extension AccountViewController: DialogListViewControllerDelegate {
    func didSelectIndex(_ index: Int, with type: DialogType) {
        switch type {
        case .accountPrefs:
            if viewModel.account.networkType != .lightning {
                switch index {
                case 0:
                    renameDialog()
                case 1:
                    archive()
                default:
                    break
                }
            } else {
                switch index {
                case 0:
                    navigateToBip85Mnemonic()
                case 1:
                    presentNodeInfo()
                case 2:
                    removeSubaccount()
                default:
                    break
                }
            }
        case .enable2faPrefs:
            switch Enable2faPrefs(rawValue: index) {
            case .add:
                let session = viewModel.account.session
                let enabled2FA = session?.twoFactorConfig?.anyEnabled ?? false
                let isSS = session?.gdkNetwork.electrum ?? false
                if isSS {
                    showError("Two-Factor authentication not available for singlesig accounts")
                    return
                } else if enabled2FA {
                    showError("Two factor authentication already enabled")
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

extension AccountViewController: DialogRenameViewControllerDelegate {
    func didRename(name: String, index: String?) {
        rename(name: name)
    }
    func didCancel() {
    }
}

extension AccountViewController: TransactionViewControllerDelegate {
    func onMemoEdit() {
        viewModel.getTransactions(restart: true)
    }
}

extension AccountViewController: AccountArchivedViewControllerDelegate {
    func onDismissArchived() {
        self.navigationController?.popViewController(animated: true)
    }

    func showArchived() {
        let storyboard = UIStoryboard(name: "Accounts", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountArchiveViewController") as? AccountArchiveViewController, var viewControllers = navigationController?.viewControllers, let nav = navigationController {
            viewControllers.removeLast()
            viewControllers.append(vc)
            nav.setViewControllers(viewControllers, animated: true)
        }
    }
}

extension AccountViewController: DialogScanViewControllerDelegate {

    func didScan(value: String, index: Int?) {
        var account = viewModel.account!
        let parser = Parser(selectedAccount: account, input: value, discoverable: true)
        parser.parse()
            .done { [self] _ in
                // open lightning auth
                switch parser.lightningType {
                case .lnUrlAuth(let data):
                    ltAuthViewController(requestData: data)
                    return
                case .lnUrlPay(_), .bolt11(_):
                    if let lightningSubaccount = parser.lightningSubaccount {
                        account = lightningSubaccount
                    }
                case .none:
                    break
                default:
                    DropAlert().warning(message: parser.error ?? "Invalid QR code")
                    return
                }
                // open send page
                let tx = parser.createTx?.tx
                let sendModel = SendViewModel(account: account,
                                              inputType: tx?.txType ?? .transaction,
                                              transaction: tx,
                                              input: value)
                self.sendViewController(model: sendModel)
            }.catch { _ in DropAlert().warning(message: parser.error ?? "Invalid QR code") }
    }

    func ltAuthViewController(requestData: LnUrlAuthRequestData) {
        let storyboard = UIStoryboard(name: "LTFlow", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "LTAuthViewController") as? LTAuthViewController {
            vc.requestData = requestData
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func sendViewController(model: SendViewModel) {
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
            vc.viewModel = model
            vc.fixedWallet = false
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func didStop() {
    }
}
