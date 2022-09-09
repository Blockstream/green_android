import UIKit
import PromiseKit

enum OverviewSection: Int, CaseIterable {
    case account = 0
    case accountId = 1
    case card = 2
    case asset = 3
    case transaction = 4
}

class OverviewViewController: UIViewController {

    // UI outlets
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var actionsView: UIStackView!
    @IBOutlet weak var sendView: UIView!
    @IBOutlet weak var sendLabel: UILabel!
    @IBOutlet weak var sendImage: UIImageView!
    @IBOutlet weak var receiveView: UIView!
    @IBOutlet weak var receiveLabel: UILabel!
    @IBOutlet weak var receiveImage: UIImageView!

    // UI dimensions
    var headerH: CGFloat = 44.0
    var footerH: CGFloat = 54.0
    var footerHandleAccountH: CGFloat = 118.0

    // token for notifications
    private var blockToken: NSObjectProtocol?
    private var transactionToken: NSObjectProtocol?
    private var assetsUpdatedToken: NSObjectProtocol?
    private var settingsUpdatedToken: NSObjectProtocol?
    private var tickerUpdatedToken: NSObjectProtocol?
    private var networkToken: NSObjectProtocol?
    private var reset2faToken: NSObjectProtocol?

    // alerts data for tableviews
    private var alertCards: [AlertCardType] = []

    // subaccount balance for tableviews
    private var assets = [(key: String, value: Int64)]()

    // transactions data for tableviews
    private var transactions: [Transaction] = []
    private var fetchTxs: Promise<Void>?
    private var callPage: UInt32 = 0
    private var isTxLoading = false

    // subaccounts data for tableviews
    private var allSubaccounts = [WalletItem]()
    private var showSubaccounts = false
    private var archivedSubaccount: Int { allSubaccounts.filter { $0.hidden == true }.count }
    private var subaccounts: [WalletItem] {
        get {
            if allSubaccounts.count == 0 {
                return []
            }
            return allSubaccounts
                .filter { $0.hashValue == wm?.currentSubaccount?.hashValue } +
            allSubaccounts
                .filter { $0.hashValue != wm?.currentSubaccount?.hashValue && $0.hidden == false}
        }
    }

    // current wallet
    var presentingWallet: WalletItem? { wm?.currentSubaccount }

    // global variables
    private var account = AccountsManager.shared.current
    private var wm: WalletManager? { WalletManager.shared[account?.id ?? ""] }
    private var session: SessionManager? { wm?.currentSession }
    private var isLiquid: Bool { session?.gdkNetwork.liquid ?? false }
    private var isAmp: Bool { presentingWallet?.type == AccountType.amp }
    private var btc: String { return session?.gdkNetwork.getFeeAsset() ?? "btc" }
    private var color: UIColor = .clear
    private var userWillLogout = false
    private var analyticsDone = false
    private var isLoading = false

    private var remoteAlert: RemoteAlert?

    // first load
    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()

        tableView.accessibilityIdentifier = AccessibilityIdentifiers.OverviewScreen.view
        sendView.accessibilityIdentifier = AccessibilityIdentifiers.OverviewScreen.sendView
        receiveView.accessibilityIdentifier = AccessibilityIdentifiers.OverviewScreen.receiveView

        tableView.register(UINib(nibName: "AlertCardCell", bundle: nil), forCellReuseIdentifier: "AlertCardCell")

        self.remoteAlert = RemoteAlertManager.shared.getAlert(screen: .overview, network: AccountsManager.shared.current?.networkName)

        startAnimating()
        AnalyticsManager.shared.recordView(.overview, sgmt: AnalyticsManager.shared.sessSgmt(account))

        AnalyticsManager.shared.getSurvey { [weak self] widget in
            if let widget = widget {
                DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 1.0) {
                    self?.surveyUI(widget)
                }
            }
        }
    }

    func surveyUI(_ widget: CountlyWidget) {
        let storyboard = UIStoryboard(name: "Survey", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SurveyViewController") as? SurveyViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.widget = widget
            present(vc, animated: false, completion: nil)
        }
    }

    func reloadSections(_ sections: [OverviewSection], animated: Bool) {
        if animated {
            tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
        } else {
            UIView.performWithoutAnimation {
                tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            }
        }
    }

    func setContent() {
        // setup left menu bar: wallet selector
        let networkSelector = ((Bundle.main.loadNibNamed("NetworkSelectorBarItem", owner: self, options: nil)![0] as? NetworkSelectorBarItem)!)
        networkSelector.configure(isEphemeral: account?.isEphemeral ?? false, {[weak self] () in
            self?.switchNetwork()
        })
        let leftItem: UIBarButtonItem = UIBarButtonItem(customView: networkSelector)
        navigationItem.leftBarButtonItem = leftItem

        // setup right menu bar: settings
        let settingsBtn = UIButton(type: .system)
        settingsBtn.setImage(UIImage(named: "settings"), for: .normal)
        settingsBtn.addTarget(self, action: #selector(settingsBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
        settingsBtn.accessibilityIdentifier = AccessibilityIdentifiers.OverviewScreen.settingsBtn

        // setup labels and gestures
        sendLabel.text = NSLocalizedString("id_send", comment: "").capitalized
        receiveLabel.text = NSLocalizedString("id_receive", comment: "").capitalized
        sendView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.sendfromWallet)))
        receiveView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.receiveToWallet)))

        // setup tableview
        tableView.prefetchDataSource = self
        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)
    }

    func setStyle() {
        actionsView.layer.cornerRadius = 6.0
        sendImage.tintColor = UIColor.customMatrixGreen()
        receiveImage.tintColor = UIColor.customMatrixGreen()

        if account?.network == AvailableNetworks.bitcoin.rawValue { color = AvailableNetworks.bitcoin.color() }
        if account?.network == AvailableNetworks.liquid.rawValue { color = AvailableNetworks.liquid.color() }
        if account?.network == AvailableNetworks.testnet.rawValue { color = AvailableNetworks.testnet.color() }
        if account?.network == AvailableNetworks.testnetLiquid.rawValue { color = AvailableNetworks.testnetLiquid.color() }
    }

    // open wallet selector drawer
    @objc func switchNetwork() {
        let storyboard = UIStoryboard(name: "DrawerNetworkSelection", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DrawerNetworkSelection") as? DrawerNetworkSelectionViewController {
            vc.transitioningDelegate = self
            vc.modalPresentationStyle = .custom
            vc.delegate = self
            present(vc, animated: true, completion: nil)
        }
    }

    // open settings
    @objc func settingsBtnTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        let nvc = storyboard.instantiateViewController(withIdentifier: "UserSettingsNavigationController")
        if let nvc = nvc as? UINavigationController {
            if let vc = nvc.viewControllers.first as? UserSettingsViewController {
                vc.delegate = self
                vc.wallet = presentingWallet
                nvc.modalPresentationStyle = .fullScreen
                present(nvc, animated: true, completion: nil)
            }
        }
    }

    // open send flow
    @objc func sendfromWallet(_ sender: UIButton) {
        if account?.isWatchonly ?? false {
            let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""),
                                          message: "Send is disabled in watch-only mode",
                                          preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .default) { _ in })
            self.present(alert, animated: true, completion: nil)
            return
        }

        let feeAsset = getGdkNetwork(presentingWallet?.network ?? "mainnet").getFeeAsset()
        if presentingWallet?.satoshi?[feeAsset] ?? 0 == 0 {
            let message = isLiquid ? NSLocalizedString("id_insufficient_lbtc_to_send_a", comment: "") : NSLocalizedString("id_you_have_no_coins_to_send", comment: "")
            let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
            if isLiquid {
                alert.addAction(UIAlertAction(title: NSLocalizedString("id_learn_more", comment: ""), style: .default) { _ in
                    let url = URL(string: "https://help.blockstream.com/hc/en-us/articles/900000630846-How-do-I-get-Liquid-Bitcoin-L-BTC-")
                    UIApplication.shared.open(url!, options: [:])
                })
            } else {
                alert.addAction(UIAlertAction(title: NSLocalizedString("id_receive", comment: ""), style: .default) { [weak self ]_ in
                    self?.receiveScreen()
                })
            }
            self.present(alert, animated: true, completion: nil)
            return
        }

        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
            vc.wallet = presentingWallet
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    // open receive flow
    @objc func receiveToWallet(_ sender: UIButton) {
        receiveScreen()
    }

    // open receive screen
    func receiveScreen() {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "ReceiveViewController") as? ReceiveViewController {
            vc.wallet = presentingWallet
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    // open system message view
    func systemMessageScreen(text: String) {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SystemMessageViewController") as? SystemMessageViewController {
            vc.text = text
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    // refresh and reload alert cards
    func reloadAlertCards() {
        var cards: [AlertCardType] = []
        if session?.isResetActive ?? false {
            // Wallet in reset status
            if session?.twoFactorConfig?.twofactorReset.isDisputeActive ?? false {
                cards.append(AlertCardType.dispute)
            } else {
                let resetDaysRemaining = session?.twoFactorConfig?.twofactorReset.daysRemaining
                cards.append(AlertCardType.reset(resetDaysRemaining ?? 0))
            }
        }
        if account?.isEphemeral == true {
            // Bip39 ephemeral wallet
            cards.append(AlertCardType.ephemeralWallet)
        }
        if let network = account?.gdkNetwork, !network.mainnet {
            // Testnet wallet
            cards.append(AlertCardType.testnetNoValue)
        }
        if Balance.fromSatoshi(0)?.toFiat().0 == "n/a" {
            // Price provider not available
            cards.append(AlertCardType.fiatMissing)
        }
        if let remoteAlert = remoteAlert {
            cards.append(AlertCardType.remoteAlert(remoteAlert))
        }
        self.alertCards = cards
        self.reloadSections([OverviewSection.card], animated: false)

        // load system messages
        let bgq = DispatchQueue.global(qos: .background)
        guard let session = session else {
            return
        }
        Guarantee().then(on: bgq) {
            session.loadSystemMessage()
        }.done { text in
            if let text = text, !text.isEmpty {
                cards.append(AlertCardType.systemMessage(text))
            }
            self.alertCards = cards
            self.reloadSections([OverviewSection.card], animated: false)
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    // dismiss remote alert
    func remoteAlertDismiss() {
        remoteAlert = nil
        reloadAlertCards()
    }

    // tableview refresh gesture
    @objc func handleRefresh(_ sender: UIRefreshControl? = nil) {
        reloadData(discovery: showSubaccounts, refreshBalance: true, scrollTop: true)
    }

    // reload all tableview data
    func reloadData(discovery: Bool = false, refreshBalance: Bool = false, scrollTop: Bool = false) {
        if isLoading == true {
            // avoid too many reloading events
            return
        }
        isLoading = true
        let contentOffset = tableView.contentOffset
        firstly { Guarantee() }
            .compactMap { self.reloadAlertCards() }
            .then { self.reloadSubaccounts(refresh: refreshBalance, discovery: discovery) }
            .then { self.reloadWallet() }
            .compactMap {
                if self.tableView.refreshControl?.isRefreshing ?? false {
                    self.tableView.refreshControl?.endRefreshing()
                }
                self.stopAnimating()
            }
            .compactMap { _ in self.reloadAssets() }
            .then { _ in self.reloadTransactions(untilPage: scrollTop ? 0 : self.callPage) }
            .ensure { self.isLoading = false }
            .done {
                self.tableView.layoutIfNeeded()
                if !scrollTop {
                    self.tableView.setContentOffset(contentOffset, animated: false)
                }
                self.callAnalytics() }
            .catch { err in
                self.tableView.reloadData()
                print(err.localizedDescription) }
    }

    // send analytics
    func callAnalytics() {

        if analyticsDone == true { return }
        analyticsDone = true

        var accountsFunded: Int = 0
        allSubaccounts.forEach { item in
            let assets = item.satoshi ?? [:]
            for (_, value) in assets where value > 0 {
                    accountsFunded += 1
                    break
            }
        }
        let walletFunded: Bool = accountsFunded > 0
        let accounts: Int = allSubaccounts.count
        let accountsTypes: String = Array(Set(allSubaccounts.map { $0.type.rawValue })).sorted().joined(separator: ",")

        AnalyticsManager.shared.activeWallet(account: account, walletData: AnalyticsManager.WalletData(walletFunded: walletFunded, accountsFunded: accountsFunded, accounts: accounts, accountsTypes: accountsTypes))

        if AnalyticsManager.shared.consent == .notDetermined {
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.3) {
                let storyboard = UIStoryboard(name: "Shared", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "DialogCountlyConsentViewController") as? DialogCountlyConsentViewController {
                    vc.modalPresentationStyle = .overFullScreen
                    self.present(vc, animated: true, completion: nil)
                }
            }
        }
    }

    // refresh and reload the current wallet
    func reloadWallet() -> Promise<Void> {
        return wm!.balances(subaccounts: [presentingWallet!])
            .compactMap { balance in
                self.presentingWallet?.satoshi = balance
                self.reloadSections([OverviewSection.account, OverviewSection.accountId], animated: true)
            }.asVoid()
    }

    // reset and reload the transaction list until selected page
    func reloadTransactions(untilPage: UInt32) -> Promise<Void> {
        guard let session = WalletManager.current?.currentSession else { return Promise().asVoid() }
        self.transactions.removeAll()
        self.callPage = 0
        self.isTxLoading = true
        self.reloadSections([OverviewSection.transaction], animated: false)
        func step() -> Promise<Void> {
            return session
                .transactions(subaccount: presentingWallet?.pointer ?? 0, first: UInt32(self.transactions.count))
                .then { txs -> Promise<Void> in
                    self.transactions += txs.list
                    self.callPage += 1
                    if self.callPage <= untilPage {
                        return step()
                    }
                    self.isTxLoading = false
                    self.reloadSections([OverviewSection.transaction], animated: false)
                    return Promise().asVoid()
                }
        }
        return step()
    }

    // received notification new block
    func onNewBlock(_ notification: Notification) {
        // update txs only if pending txs > 0
        if transactions.filter({ $0.blockHeight == 0 }).first != nil {
            reloadData()
        }
    }

    // received notification assets update
    func onAssetsUpdated(_ notification: Notification) {
        // reload tableview with current data
        reloadSections([.account, .asset, .transaction], animated: false)
    }

    // received notification new transaction
    func onNewTransaction(_ notification: Notification) {
        reloadData(refreshBalance: true)
    }

    // received notification reconnection
    @objc func onNetworkEvent(_ notification: Notification) {
        guard let dict = notification.userInfo as NSDictionary? else { return }
        guard let connected = dict["connected"] as? Bool else { return }
        guard let loginRequired = dict["login_required"] as? Bool else { return }
        if connected == true && loginRequired == false {
            DispatchQueue.main.async { [weak self] in
                self?.reloadData(refreshBalance: true)
            }
        }
    }

    // reload tableview
    func refresh(_ notification: Notification) {
        reloadSections([OverviewSection.account, OverviewSection.asset, OverviewSection.transaction], animated: true)
        reloadAlertCards()
    }

    // reload in tableview all subaccounts with balance
    func reloadSubaccounts(refresh: Bool, discovery: Bool) -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee()
            .then(on: bgq) { self.wm!.subaccounts(discovery) }
            .get(on: bgq) { self.wm!.balances(subaccounts: $0) }
            .compactMap { wallets in
                self.allSubaccounts = wallets
                self.reloadSections([OverviewSection.account], animated: false)
            }.asVoid()
    }

    // reload in tableview assets of current wallet
    func reloadAssets() {
        assets = [(key: String, value: Int64)]()
        if let wallet = presentingWallet {
            assets = Transaction.sort(wallet.satoshi ?? [:])
        }
        self.reloadSections([OverviewSection.asset], animated: false)
    }

    // open add account flow
    @objc func addAccount() {
        let storyboard = UIStoryboard(name: "Accounts", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountCreateSelectTypeViewController") as? AccountCreateSelectTypeViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    // open view archived accounts page
    @objc func viewArchivedAccounts() {
        let storyboard = UIStoryboard(name: "Accounts", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountArchiveViewController") as? AccountArchiveViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    // TODO: fix this delegate
    func onAccountChange() {
        reloadData(refreshBalance: true)
    }

    // show account id when required
    func showAccountId() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogAccountIdViewController") as? DialogAccountIdViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.wallet = presentingWallet
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                self.present(vc, animated: false, completion: nil)
            }
        }
    }
}

extension OverviewViewController: UIViewControllerTransitioningDelegate {
    func presentationController(forPresented presented: UIViewController, presenting: UIViewController?, source: UIViewController) -> UIPresentationController? {
        if let presented = presented as? DrawerNetworkSelectionViewController {
            return DrawerPresentationController(presentedViewController: presented, presenting: presenting)
        }
        return ModalPresentationController(presentedViewController: presented, presenting: presenting)
    }

    func animationController(forPresented presented: UIViewController, presenting: UIViewController, source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        if presented as? DrawerNetworkSelectionViewController != nil {
            return DrawerAnimator(isPresenting: true)
        } else {
            return ModalAnimator(isPresenting: true)
        }
    }

    func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        if dismissed as? DrawerNetworkSelectionViewController != nil {
            return DrawerAnimator(isPresenting: false)
        } else {
            return ModalAnimator(isPresenting: false)
        }
    }
}

extension OverviewViewController: DrawerNetworkSelectionDelegate {

    // accounts drawer: add new waller
    func didSelectAddWallet() {
        AccountNavigator.goCreateRestore()
    }

    // accounts drawer: select another account
    func didSelectAccount(account: Account) {
        // don't switch if same account selected
        if account.id == self.account?.id ?? "" {
            return
        }
        AccountNavigator.goLogin(account: account)
    }

    // accounts drawer: select hw account
    func didSelectHW(account: Account) {
        AccountNavigator.goHWLogin(isJade: account.isJade)
    }

    // accounts drawer: select app settings
    func didSelectSettings() {
        self.presentedViewController?.dismiss(animated: true, completion: {
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
                self.present(vc, animated: true) {}
            }
        })
    }

    func didSelectAbout() {
        self.presentedViewController?.dismiss(animated: true, completion: {
            let storyboard = UIStoryboard(name: "About", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "AboutViewController") as? AboutViewController {
                self.navigationController?.pushViewController(vc, animated: true)
            }
        })
    }
}

extension OverviewViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return OverviewSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch OverviewSection(rawValue: section) {
        case .account:
            return showSubaccounts ? subaccounts.count : 1
        case .accountId:
            return 1
        case .card:
            return alertCards.count
        case .asset:
            return assets.count
        case .transaction:
            return transactions.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch OverviewSection(rawValue: indexPath.section) {
        case .account:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "OverviewAccountCell") as? OverviewAccountCell {
                var action: VoidToVoid?
                if showSubaccounts && account?.isWatchonly == false {
                    action = { [weak self] in
                        self?.presentAccountMenu(frame: cell.frame, index: indexPath.row)
                    }
                }
                if let subaccount = showSubaccounts ? subaccounts[indexPath.row] : presentingWallet {
                    cell.configure(account: subaccount, action: action, showAccounts: showSubaccounts)
                }
                cell.selectionStyle = .none
                return cell
            }
        case .accountId:
            if isLiquid && isAmp {
                if let cell = tableView.dequeueReusableCell(withIdentifier: "OverviewAccountIdCell") as? OverviewAccountIdCell {
                    cell.configure(onAction: {
                        [weak self] in
                        self?.showAccountId()
                    })
                    cell.selectionStyle = .none
                    return cell
                }
            }
        case .card:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AlertCardCell", for: indexPath) as? AlertCardCell {
                let alertCard = alertCards[indexPath.row]
                switch alertCard {
                case .reset, .dispute, .reactivate:
                    cell.configure(alertCards[indexPath.row],
                                   onLeft: {[weak self] in
                        self?.performSegue(withIdentifier: "overviewReactivate2fa", sender: self)
                    },
                                   onRight: {[weak self] in
                        self?.performSegue(withIdentifier: "overviewLeaarnMore2fa", sender: self)
                    },
                                   onDismiss: nil)
                case .systemMessage(let text):
                    cell.configure(alertCards[indexPath.row],
                                   onLeft: nil,
                                   onRight: {[weak self] in
                        self?.systemMessageScreen(text: text)
                    },
                                   onDismiss: nil)
                case .fiatMissing:
                    cell.configure(alertCards[indexPath.row],
                                   onLeft: nil,
                                   onRight: nil,
                                   onDismiss: nil)
                case .testnetNoValue:
                    cell.configure(alertCards[indexPath.row],
                                   onLeft: nil,
                                   onRight: nil,
                                   onDismiss: nil)
                case .ephemeralWallet:
                    cell.configure(alertCards[indexPath.row],
                                   onLeft: nil,
                                   onRight: nil,
                                   onDismiss: nil)
                case .remoteAlert:
                    cell.configure(alertCards[indexPath.row],
                                   onLeft: nil,
                                   onRight: (remoteAlert?.link ?? "" ).isEmpty ? nil : {[weak self] in
                        if let url = URL(string: self?.remoteAlert?.link ?? "") {
                            UIApplication.shared.open(url)
                        }
                    },
                                   onDismiss: {[weak self] in
                        self?.remoteAlertDismiss()
                    })
                }
                cell.selectionStyle = .none
                return cell
            }
        case .asset:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "OverviewAssetCell", for: indexPath) as? OverviewAssetCell {
                let tag = assets[indexPath.row].key
                let info = WalletManager.current?.currentSession?.registry?.info(for: tag)
                let icon = WalletManager.current?.currentSession?.registry?.image(for: tag)
                let satoshi = assets[indexPath.row].value
                cell.configure(tag: tag, info: info, icon: icon, satoshi: satoshi, isLiquid: isLiquid)
                return cell
            }
        case .transaction:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "OverviewTransactionCell", for: indexPath) as? OverviewTransactionCell {
                let transaction = transactions[indexPath.row]
                cell.setup(transaction: transaction, network: account?.network)
                cell.checkBlockHeight(transaction: transaction, blockHeight: WalletManager.current?.currentSession?.notificationManager?.blockHeight ?? 0)
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch OverviewSection(rawValue: section) {
        case .transaction:
            return headerH
        case .asset:
            if isLiquid {
                return headerH
            } else {
                return 1
            }
        default:
            return 1
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        switch OverviewSection(rawValue: section) {
        case .account:
            return showSubaccounts ? footerHandleAccountH : 1.0
        case .transaction:
            return transactions.count == 0 ? footerH : 1.0
        default:
            return 1.0
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {

        switch OverviewSection(rawValue: indexPath.section) {
        case .accountId:
            return isLiquid && isAmp ? UITableView.automaticDimension : 0.0
        default:
            return UITableView.automaticDimension
        }
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {

        switch OverviewSection(rawValue: section) {
        case .transaction:
            return headerView(NSLocalizedString("id_transactions", comment: ""))
        case .asset:
            if isLiquid {
                return headerView(NSLocalizedString("id_assets", comment: ""))
            } else {
                return headerView("")
            }
        default:
            return headerView("")
        }

    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        switch OverviewSection(rawValue: section) {
        case .account:
            let isWatchonly = account?.isWatchonly ?? false
            let isResetActive = WalletManager.current?.currentSession?.isResetActive ?? false
            return showSubaccounts && !isWatchonly && !isResetActive ? footerView(.handleAccount) : footerView(.none)
        case .transaction:
            return transactions.count == 0 ? footerView(.noTransactions) : footerView(.none)
        default:
            return footerView(.none)
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        switch OverviewSection(rawValue: indexPath.section) {
        case .account:
            UIView.setAnimationsEnabled(true)
            showSubaccounts = !showSubaccounts
            reloadSections([OverviewSection.account], animated: true)
            if indexPath.row > 0 {
                wm?.currentSubaccount = subaccounts[indexPath.row]
                assets.removeAll()
                transactions.removeAll()
                reloadSections([.asset, .transaction], animated: false)
                reloadData(scrollTop: true)
            }
        case .asset:
            if !isLiquid { return }
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogAssetDetailViewController") as? DialogAssetDetailViewController {
                let tag = assets[indexPath.row].key
                vc.tag = tag
                vc.asset = WalletManager.current?.currentSession?.registry?.info(for: tag)
                vc.satoshi = presentingWallet?.satoshi?[tag]
                vc.modalPresentationStyle = .overFullScreen
                present(vc, animated: false, completion: nil)
            }
        case .transaction:
            let transaction = transactions[indexPath.row]
            let storyboard = UIStoryboard(name: "Transaction", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "TransactionViewController") as? TransactionViewController {
                vc.transaction = transaction
                vc.wallet = presentingWallet
                navigationController?.pushViewController(vc, animated: true)
            }
        default:
            break
        }
    }
}

extension OverviewViewController: UITableViewDataSourcePrefetching {
    // incremental transactions fetching from gdk
    func tableView(_ tableView: UITableView, prefetchRowsAt indexPaths: [IndexPath]) {
        if fetchTxs != nil && fetchTxs!.isPending {
            // null or pending
            return
        }
        let filteredIndexPaths = indexPaths.filter { $0.section == OverviewSection.transaction.rawValue }
        let row = filteredIndexPaths.last?.row ?? 0
        if self.callPage > 0 && row > (self.callPage - 1) * Constants.trxPerPage {
            let session = SessionsManager.shared[account?.id ?? ""]
            let offset = self.transactions.count
            self.fetchTxs = session?.transactions(subaccount: presentingWallet?.pointer ?? 0, first: UInt32(offset))
                .map { page in
                    self.transactions += page.list
                    self.callPage += 1
                    var paths: [IndexPath] = []
                    for i in offset..<(offset + page.list.count) {
                        paths.append(IndexPath(row: i, section: OverviewSection.transaction.rawValue))
                    }
                    self.tableView.performBatchUpdates({
                        self.tableView.insertRows(at: paths, with: .none)
                    }, completion: nil)
                }
        }
    }
}

extension OverviewViewController {
    enum FooterType {
        case noTransactions
        case handleAccount
        case none
    }

    func headerView(_ txt: String) -> UIView {
        if txt == "" {
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: 1.0))
            section.backgroundColor = .clear
            return section
        }
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
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 24),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -24)
        ])

        return section
    }

    func footerView(_ type: FooterType) -> UIView {

        switch type {
        case .none:
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: 1.0))
            section.backgroundColor = .clear
            return section
        case .handleAccount:
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: footerHandleAccountH))
            section.backgroundColor = .clear

            let addBtn = UIButton(frame: .zero)
            addBtn.setTitle(NSLocalizedString("id_add_new_account", comment: ""), for: .normal)
            addBtn.titleLabel?.font = UIFont.systemFont(ofSize: 16, weight: .regular)
            addBtn.setStyle(.outlinedGray)
            addBtn.addTarget(self, action: #selector(addAccount), for: .touchUpInside)

            let archiveBtn = UIButton(frame: .zero)
            let btnTitle = archivedSubaccount > 0 ? String(format: NSLocalizedString("id_view_archived_accounts_d", comment: ""), archivedSubaccount) : NSLocalizedString("id_no_archived_accounts", comment: "")
            archiveBtn.setTitle(btnTitle, for: .normal)
            archiveBtn.titleLabel?.font = UIFont.systemFont(ofSize: 16, weight: .regular)
            archiveBtn.setStyle(.outlinedGray)
            archiveBtn.addTarget(self, action: #selector(viewArchivedAccounts), for: .touchUpInside)
            archiveBtn.isEnabled = archivedSubaccount > 0
            archiveBtn.alpha = archivedSubaccount > 0 ? 1.0 : 0.5

            let stackView   = UIStackView()
            stackView.axis  = NSLayoutConstraint.Axis.vertical
            stackView.distribution = UIStackView.Distribution.fillEqually
            stackView.alignment = UIStackView.Alignment.center
            stackView.spacing = 10.0

            stackView.addArrangedSubview(addBtn)
            stackView.addArrangedSubview(archiveBtn)
            stackView.translatesAutoresizingMaskIntoConstraints = false

            section.addSubview(stackView)

            NSLayoutConstraint.activate([
                stackView.topAnchor.constraint(equalTo: section.topAnchor, constant: 10.0),
                stackView.bottomAnchor.constraint(equalTo: section.bottomAnchor, constant: -10.0),
                stackView.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 20.0),
                stackView.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -20.0),
                addBtn.leadingAnchor.constraint(equalTo: stackView.leadingAnchor, constant: 0.0),
                addBtn.trailingAnchor.constraint(equalTo: stackView.trailingAnchor, constant: 0.0),
                archiveBtn.leadingAnchor.constraint(equalTo: stackView.leadingAnchor, constant: 0.0),
                archiveBtn.trailingAnchor.constraint(equalTo: stackView.trailingAnchor, constant: 0.0)
            ])

            return section
        case .noTransactions:
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: footerH))
            section.backgroundColor = .clear

            let lblNoTransactions = UILabel(frame: .zero)
            lblNoTransactions.font = UIFont.systemFont(ofSize: 16, weight: .regular)
            lblNoTransactions.textColor = UIColor.customTitaniumLight()
            lblNoTransactions.numberOfLines = 0
            lblNoTransactions.textAlignment = .center
            lblNoTransactions.text = NSLocalizedString("id_your_transactions_will_be_shown", comment: "")
            lblNoTransactions.translatesAutoresizingMaskIntoConstraints = false
            section.addSubview(lblNoTransactions)

            NSLayoutConstraint.activate([
                lblNoTransactions.topAnchor.constraint(equalTo: section.topAnchor, constant: 0.0),
                lblNoTransactions.bottomAnchor.constraint(equalTo: section.bottomAnchor, constant: 0.0),
                lblNoTransactions.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 40.0),
                lblNoTransactions.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -40.0)
            ])

            if isTxLoading {
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

extension OverviewViewController {

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        if !userWillLogout {
            transactionToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil, queue: .main, using: onNewTransaction)
            blockToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Block.rawValue), object: nil, queue: .main, using: onNewBlock)
            assetsUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue), object: nil, queue: .main, using: onAssetsUpdated)
            settingsUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Settings.rawValue), object: nil, queue: .main, using: refresh)
            tickerUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Ticker.rawValue), object: nil, queue: .main, using: refresh)
            networkToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: onNetworkEvent)
            reset2faToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.TwoFactorReset.rawValue), object: nil, queue: .main, using: refresh)

            reloadData()
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = transactionToken {
            NotificationCenter.default.removeObserver(token)
            transactionToken = nil
        }
        if let token = blockToken {
            NotificationCenter.default.removeObserver(token)
            blockToken = nil
        }
        if let token = assetsUpdatedToken {
            NotificationCenter.default.removeObserver(token)
            assetsUpdatedToken = nil
        }
        if let token = settingsUpdatedToken {
            NotificationCenter.default.removeObserver(token)
            settingsUpdatedToken = nil
        }
        if let token = tickerUpdatedToken {
            NotificationCenter.default.removeObserver(token)
            tickerUpdatedToken = nil
        }
        if let token = networkToken {
            NotificationCenter.default.removeObserver(token)
            networkToken = nil
        }
        if let token = reset2faToken {
            NotificationCenter.default.removeObserver(token)
            reset2faToken = nil
        }
    }
}

extension OverviewViewController: DialogWalletNameViewControllerDelegate {

    func didRename(name: String, index: Int?) {
        guard let index = index else {
            return
        }
        let bgq = DispatchQueue.global(qos: .background)
        guard let session = WalletManager.current?.currentSession else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            session.renameSubaccount(subaccount: self.subaccounts[index].pointer, newName: name)
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.reloadData()
            AnalyticsManager.shared.renameAccount(account: self.account, walletType: self.presentingWallet?.type)
        }.catch { e in
            DropAlert().error(message: e.localizedDescription)
            print(e.localizedDescription)
        }
    }
    func didCancel() {
    }
}

extension OverviewViewController: UserSettingsViewControllerDelegate, Learn2faViewControllerDelegate {
    func userLogout() {
        userWillLogout = true
        self.presentedViewController?.dismiss(animated: true, completion: {
            DispatchQueue.main.async {
                self.startLoader(message: NSLocalizedString("id_logout", comment: ""))
                SessionsManager.remove(for: self.account?.id ?? "")
                self.stopLoader()
                let storyboard = UIStoryboard(name: "Home", bundle: nil)
                let nav = storyboard.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController
                UIApplication.shared.keyWindow?.rootViewController = nav
            }
        })
    }
}

extension OverviewViewController: UIPopoverPresentationControllerDelegate {

    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return .none
    }

    func presentationController(_ controller: UIPresentationController, viewControllerForAdaptivePresentationStyle style: UIModalPresentationStyle) -> UIViewController? {
        return UINavigationController(rootViewController: controller.presentedViewController)
    }
}

extension OverviewViewController: PopoverMenuAccountDelegate {
    // subaccounts section: select menu options
    func didSelectionMenuOption(option: MenuAccountOption, index: Int) {
        switch option {
        case .rename:
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogWalletNameViewController") as? DialogWalletNameViewController {
                vc.modalPresentationStyle = .overFullScreen
                vc.isAccountRename = true
                vc.delegate = self
                vc.index = index
                present(vc, animated: false, completion: nil)
            }
        case .archive:
            archiveSubaccount(index)
        }
    }

    // subaccounts section: popup on subaccounts
    func presentAccountMenu(frame: CGRect, index: Int) {
        let storyboard = UIStoryboard(name: "PopoverMenu", bundle: nil)
        if let popover  = storyboard.instantiateViewController(withIdentifier: "PopoverMenuAccountViewController") as? PopoverMenuAccountViewController {
            popover.delegate = self
            popover.index = index
            popover.canArchive = (subaccounts.filter { $0.hidden == false }).count > 1
            popover.modalPresentationStyle = .popover
            let popoverPresentationController = popover.popoverPresentationController
            popoverPresentationController?.backgroundColor = UIColor.customModalDark()
            popoverPresentationController?.delegate = self
            popoverPresentationController?.sourceView = self.tableView
            popoverPresentationController?.sourceRect = CGRect(x: self.tableView.frame.width - 80.0, y: frame.origin.y, width: 60.0, height: 60.0)
            popoverPresentationController?.permittedArrowDirections = .up
            self.present(popover, animated: true)
        }
    }

    // subaccounts section: archive a subaccount
    func archiveSubaccount(_ index: Int) {
        let bgq = DispatchQueue.global(qos: .background)
        guard let session = WalletManager.current?.currentSession else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            session.updateSubaccount(subaccount: self.subaccounts[index].pointer, hidden: true)
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            let present = (index == 0 ? self.subaccounts[1] : self.subaccounts[0])
            self.wm?.currentSubaccount = present
            self.reloadData()
        }.catch { e in
            DropAlert().error(message: e.localizedDescription)
            print(e.localizedDescription)
        }
    }
}
