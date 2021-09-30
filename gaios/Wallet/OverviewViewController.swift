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

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var actionsView: UIStackView!
    @IBOutlet weak var sendView: UIView!
    @IBOutlet weak var sendLabel: UILabel!
    @IBOutlet weak var sendImage: UIImageView!
    @IBOutlet weak var receiveView: UIView!
    @IBOutlet weak var receiveLabel: UILabel!
    @IBOutlet weak var receiveImage: UIImageView!

    var presentingWallet: WalletItem?
    private var transactions: [Transaction] = []
    private var fetchTxs: Promise<Void>?
    var callPage: UInt32 = 0

    private var blockToken: NSObjectProtocol?
    private var transactionToken: NSObjectProtocol?
    private var assetsUpdatedToken: NSObjectProtocol?
    private var settingsUpdatedToken: NSObjectProtocol?
    private var tickerUpdatedToken: NSObjectProtocol?
    private var networkToken: NSObjectProtocol?

    var headerH: CGFloat = 44.0
    var footerH: CGFloat = 54.0

    private var subAccounts = [WalletItem]()

    var showAccounts = false
    var assets = [(key: String, value: UInt64)]()
    var isLoading = false
    var accounts: [WalletItem] {
        get {
            if subAccounts.count == 0 { return [] }
            if showAccounts {
                return subAccounts.filter { $0.pointer == SessionManager.shared.activeWallet} + subAccounts.filter { $0.pointer != SessionManager.shared.activeWallet}
            } else {
                return subAccounts.filter { $0.pointer == SessionManager.shared.activeWallet}
            }
        }
    }
    var account = AccountsManager.shared.current
    private var isLiquid: Bool { account?.gdkNetwork?.liquid ?? false }
    private var isAmp: Bool {
        guard let wallet = presentingWallet else { return false }
        return AccountType(rawValue: wallet.type) == AccountType.amp
    }
    private var btc: String {
        return account?.gdkNetwork?.getFeeAsset() ?? ""
    }

    var color: UIColor = .clear

    var isResetActive: Bool {
        get {
            guard let twoFactorConfig = SessionManager.shared.notificationManager.twoFactorReset else { return false }
            return twoFactorConfig.isResetActive
        }
    }
    var isDisputeActive: Bool {
        get {
            guard let twoFactorConfig = SessionManager.shared.notificationManager.twoFactorReset else { return false }
            return twoFactorConfig.isDisputeActive
        }
    }
    var resetDaysRemaining: Int? {
        get {
            guard let twoFactorConfig = SessionManager.shared.notificationManager.twoFactorReset else { return nil }
            return twoFactorConfig.daysRemaining
        }
    }

    var alertCards: [AlertCardType] = []
    var userWillLogout = false

    override func viewDidLoad() {
        super.viewDidLoad()

        tableView.prefetchDataSource = self

        setContent()
        setStyle()

        let networkSelector = ((Bundle.main.loadNibNamed("NetworkSelectorBarItem", owner: self, options: nil)![0] as? NetworkSelectorBarItem)!)
        networkSelector.configure({[weak self] () in
            self?.switchNetwork()
        })
        let leftItem: UIBarButtonItem = UIBarButtonItem(customView: networkSelector)
        navigationItem.leftBarButtonItem = leftItem

        let settingsBtn = UIButton(type: .system)
        settingsBtn.setImage(UIImage(named: "settings"), for: .normal)
        settingsBtn.addTarget(self, action: #selector(settingsBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)

        loadAccounts()

        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)

        isLoading = true
    }

    func setContent() {
        sendLabel.text = NSLocalizedString("id_send", comment: "").capitalized
        receiveLabel.text = NSLocalizedString("id_receive", comment: "").capitalized
        sendView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.sendfromWallet)))
        receiveView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.receiveToWallet)))
    }

    func setStyle() {
        actionsView.layer.cornerRadius = 6.0
        sendImage.tintColor = UIColor.customMatrixGreen()
        receiveImage.tintColor = UIColor.customMatrixGreen()

        if account?.network == AvailableNetworks.bitcoin.rawValue { color = AvailableNetworks.bitcoin.color() }
        if account?.network == AvailableNetworks.liquid.rawValue { color = AvailableNetworks.liquid.color() }
        if account?.network == AvailableNetworks.testnet.rawValue { color = AvailableNetworks.testnet.color() }
    }

    @objc func switchNetwork() {
        let storyboard = UIStoryboard(name: "DrawerNetworkSelection", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DrawerNetworkSelection") as? DrawerNetworkSelectionViewController {
            vc.transitioningDelegate = self
            vc.modalPresentationStyle = .custom
            vc.delegate = self
            present(vc, animated: true, completion: nil)
        }
    }

    @objc func settingsBtnTapped(_ sender: Any) {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        let nvc = storyboard.instantiateViewController(withIdentifier: "UserSettingsNavigationController")
        if let nvc = nvc as? UINavigationController {
            if let vc = nvc.viewControllers.first as? UserSettingsViewController {
                vc.delegate = self
                nvc.modalPresentationStyle = .fullScreen
                present(nvc, animated: true, completion: nil)
            }
        }
    }

    @objc func sendfromWallet(_ sender: UIButton) {
        if account?.isWatchonly ?? false {
            let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""),
                                          message: "Send is disabled in watch-only mode",
                                          preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .default) { _ in })
            self.present(alert, animated: true, completion: nil)
            return
        }
        if presentingWallet?.btc == 0 {
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
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendBtcViewController") as? SendBtcViewController {
            vc.isSweep = false
            vc.wallet = presentingWallet
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    @objc func receiveToWallet(_ sender: UIButton) {
        receiveScreen()
    }

    func receiveScreen() {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "ReceiveBtcViewController") as? ReceiveBtcViewController {
            vc.wallet = presentingWallet
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func systemMessageScreen(text: String) {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SystemMessageViewController") as? SystemMessageViewController {
            vc.text = text
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func loadAlertCards() {
        alertCards = []
        if isResetActive {
            let resetCard = isDisputeActive ? AlertCardType.dispute : AlertCardType.reset(resetDaysRemaining ?? 0)
            alertCards.append(resetCard)
        }
        if AccountsManager.shared.current!.network == "liquid" {
            switch Registry.shared.failStatus() {
            case .assets, .all:
                alertCards.append(AlertCardType.assetsRegistryFail)
            case .icons:
                alertCards.append(AlertCardType.iconsRegistryFail)
            case .none:
                break
            }
        }
        tableView.reloadSections([OverviewSection.card.rawValue], with: .none)
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map(on: bgq) {
            try SessionManager.shared.getSystemMessage()
        }.done { text in
            if !text.isEmpty {
                self.alertCards.append(AlertCardType.systemMessage(text))
                self.tableView.reloadSections([OverviewSection.card.rawValue], with: .none)
            }
        }.catch { err in
            print(err.localizedDescription)
        }
        //We will use Card2faType.reactivate for expired coins

        let bgqu = DispatchQueue.global(qos: .background)
        firstly {
            return Guarantee()
        }.map(on: bgqu) { () -> (String?, String) in
            return Balance.convert(details: ["satoshi": 0])?.get(tag: "fiat") ?? (nil, "")
        }.done { (amount, _) in
            if amount == nil {
                self.alertCards.append(AlertCardType.fiatMissing)
                self.tableView.reloadSections([OverviewSection.card.rawValue], with: .none)
            }
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    @objc func handleRefresh(_ sender: UIRefreshControl? = nil) {
        self.isLoading = true
        self.loadWallet()
        .compactMap {
            self.tableView.reloadSections([OverviewSection.accountId.rawValue], with: .none)
            return self.loadAssets()
        }
        .then {
            self.loadTransactions()
        }.done { _ in
            self.isLoading = false
            self.showTransactions()
        }.catch { err in
            print(err.localizedDescription)
        }
        loadAlertCards()
    }

    func loadWallet() -> Promise<Void> {
        return SessionManager.shared.subaccount().then { wallet in
            wallet.getBalance().compactMap { _ in wallet }
        }.map { wallet in
            self.presentingWallet = wallet
        }
    }

    func loadTransactions(_ pageId: Int = 0) -> Promise<Void> {
        return SessionManager.shared.transactions(first: UInt32(pageId))
        .map { page in
            self.transactions.removeAll()
            self.transactions += page.list
            self.callPage = UInt32(pageId) + 1
        }
    }

    func showTransactions() {
        if tableView.refreshControl?.isRefreshing ?? false {
            tableView.refreshControl?.endRefreshing()
        }
        UIView.setAnimationsEnabled(false)
        self.tableView.beginUpdates()
        self.tableView.reloadSections([OverviewSection.transaction.rawValue], with: .none)
        self.tableView.endUpdates()
        UIView.setAnimationsEnabled(true)
    }

    func onNewBlock(_ notification: Notification) {
        // update txs only if pending txs > 0
        if transactions.filter({ $0.blockHeight == 0 }).first != nil {
            handleRefresh()
        }
    }

    func onAssetsUpdated(_ notification: Notification) {
        Guarantee()
            .compactMap { Registry.shared.cache() }
            .done {
                self.tableView.reloadSections([OverviewSection.asset.rawValue], with: .none)
                self.showTransactions()
            }
            .catch { err in
                print(err.localizedDescription)
        }
    }

    func onNewTransaction(_ notification: Notification) {
        guard let dict = notification.userInfo as NSDictionary? else { return }
        guard let subaccounts = dict["subaccounts"] as? [UInt32] else { return }
        if subaccounts.contains(SessionManager.shared.activeWallet) {
            handleRefresh()
        }
    }

    @objc func onNetworkEvent(_ notification: Notification) {
        guard let dict = notification.userInfo as NSDictionary? else { return }
        guard let connected = dict["connected"] as? Bool else { return }
        guard let loginRequired = dict["login_required"] as? Bool else { return }
        if connected == true && loginRequired == false {
            DispatchQueue.main.async { [weak self] in
                self?.handleRefresh()
            }
        }
    }

    func refresh(_ notification: Notification) {
        tableView.reloadSections([OverviewSection.asset.rawValue], with: .none)
        tableView.reloadSections([OverviewSection.transaction.rawValue], with: .none)
    }

    func loadAccounts() {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            SessionManager.shared.subaccounts()
        }
        .ensure {
            self.stopAnimating()
        }.done { wallets in
            self.subAccounts = wallets
            self.tableView.reloadSections([OverviewSection.account.rawValue], with: .none)
            self.handleRefresh()
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    func loadAssets() {
        assets = [(key: String, value: UInt64)]()
        if let wallet = presentingWallet {
            assets = Transaction.sort(wallet.satoshi ?? [:])
            sortAssets()
        }
        tableView.reloadSections([OverviewSection.asset.rawValue], with: .none)
    }

    func sortAssets() {
        var tAssets: [SortingAsset] = []
        assets.forEach { asset in
            let tAss = SortingAsset(tag: asset.key, info: Registry.shared.infos[asset.key], hasImage: Registry.shared.hasImage(for: asset.key), value: asset.value)
            tAssets.append(tAss)
        }
        var oAssets = [(key: String, value: UInt64)]()
        tAssets.sort(by: {!$0.hasImage && !$1.hasImage ? $0.info?.ticker != nil && !($1.info?.ticker != nil) : $0.hasImage && !$1.hasImage})

        tAssets.forEach { asset in
            oAssets.append((key:asset.tag, value: asset.value))
        }
        assets = oAssets
    }

    func reloadRegistry() {
        self.startAnimating()
        Registry.shared.load().done { () in
            self.stopAnimating()
            self.loadAlertCards()
        }.catch { _ in }
    }

    @objc func addAccount() {
        let storyboard = UIStoryboard(name: "Accounts", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountCreateSelectTypeViewController") as? AccountCreateSelectTypeViewController {
            show(vc, sender: self)
        }
    }

    func onAccountChange() {
        loadAccounts()
    }

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

    func logout() -> Promise<Void> {
        let bgq = DispatchQueue.global(qos: .background)
        return firstly {
            self.startLoader(message: NSLocalizedString("id_logout", comment: ""))
            return Guarantee()
        }.map(on: bgq) {
            _ = SessionManager.newSession()
            if let account = AccountsManager.shared.current {
                if account.isJade || account.isLedger {
                    BLEManager.shared.dispose()
                }
            }
        }
    }

    func didSelectAddWallet() {
        logout()
            .recover { _ in }
            .done {
                self.navigationController?.dismiss(animated: true, completion: {})
                self.navigationController?.popToRootViewController(animated: true)
                self.stopLoader()

                let homeS = UIStoryboard(name: "Home", bundle: nil)
                let onBoardS = UIStoryboard(name: "OnBoard", bundle: nil)

                if let nav = homeS.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController,
                   let vc = onBoardS.instantiateViewController(withIdentifier: "LandingViewController") as? LandingViewController {
                    nav.pushViewController(vc, animated: false)
                    UIApplication.shared.keyWindow?.rootViewController = nav
                }
            }
    }

    func didSelectAccount(account: Account) {
        logout()
            .recover { _ in }
            .done {
                self.navigationController?.dismiss(animated: true, completion: {})
                self.navigationController?.popToRootViewController(animated: true)
                self.stopLoader()

                let homeS = UIStoryboard(name: "Home", bundle: nil)
                let onBoardS = UIStoryboard(name: "OnBoard", bundle: nil)

                if account.isWatchonly {
                    if let nav = homeS.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController,
                       let vc = onBoardS.instantiateViewController(withIdentifier: "WatchOnlyLoginViewController") as? WatchOnlyLoginViewController {
                        vc.account = account
                        nav.pushViewController(vc, animated: false)
                        UIApplication.shared.keyWindow?.rootViewController = nav
                    }
                } else {
                    if let nav = homeS.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController,
                       let vc = homeS.instantiateViewController(withIdentifier: "LoginViewController") as? LoginViewController {
                        vc.account = account
                        nav.pushViewController(vc, animated: false)
                        UIApplication.shared.keyWindow?.rootViewController = nav
                    }
                }
            }
    }

    func didSelectHW(account: Account) {

        logout()
            .recover { _ in }
            .done {
                self.navigationController?.dismiss(animated: true, completion: {})
                self.navigationController?.popToRootViewController(animated: true)
                self.stopLoader()

                let storyboard = UIStoryboard(name: "Home", bundle: nil)
                let nav = storyboard.instantiateViewController(withIdentifier: "HomeViewController") as? UINavigationController

                let storyboard2 = UIStoryboard(name: "HWW", bundle: nil)
                if let vc = storyboard2.instantiateViewController(withIdentifier: "HWWScanViewController") as? HWWScanViewController {
                    vc.account = account
                    nav?.pushViewController(vc, animated: false)
                    UIApplication.shared.keyWindow?.rootViewController = nav
                }
            }
    }
}

extension OverviewViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return OverviewSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case OverviewSection.account.rawValue:
            return accounts.count
        case OverviewSection.accountId.rawValue:
            return 1
        case OverviewSection.card.rawValue:
            return alertCards.count
        case OverviewSection.asset.rawValue:
            return assets.count
        case OverviewSection.transaction.rawValue:
            return transactions.count
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case OverviewSection.account.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "OverviewAccountCell") as? OverviewAccountCell {
                var action: VoidToVoid?
                if indexPath.row == 0 && showAccounts {
                    action = { [weak self] in

                        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
                        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogWalletNameViewController") as? DialogWalletNameViewController {
                            vc.modalPresentationStyle = .overFullScreen
                            vc.isAccountRename = true
                            vc.delegate = self
                            self?.present(vc, animated: false, completion: nil)
                        }
                    }
                }
                cell.configure(account: accounts[indexPath.row], action: action, color: color, showAccounts: showAccounts)
                cell.selectionStyle = .none
                return cell
            }
        case OverviewSection.accountId.rawValue:
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
        case OverviewSection.card.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "OverviewAlertCardCell", for: indexPath) as? OverviewAlertCardCell {
                let alertCard = alertCards[indexPath.row]
                switch alertCard {
                case .reset, .dispute, .reactivate:
                    cell.configure(alertCards[indexPath.row],
                                   onLeft: {[weak self] in
                        self?.performSegue(withIdentifier: "overviewReactivate2fa", sender: self)
                    },
                                   onRight: {[weak self] in
                        self?.performSegue(withIdentifier: "overviewLeaarnMore2fa", sender: self)
                    })
                case .assetsRegistryFail, .iconsRegistryFail:
                    cell.configure(alertCards[indexPath.row],
                                   onLeft: nil,
                                   onRight: {[weak self] in
                        self?.reloadRegistry()
                    })
                case .systemMessage(let text):
                    cell.configure(alertCards[indexPath.row],
                                   onLeft: nil,
                                   onRight: {[weak self] in
                        self?.systemMessageScreen(text: text)
                    })
                case .fiatMissing:
                    cell.configure(alertCards[indexPath.row],
                                   onLeft: nil,
                                   onRight: nil)
                }
                cell.selectionStyle = .none
                return cell
            }
        case OverviewSection.asset.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "OverviewAssetCell", for: indexPath) as? OverviewAssetCell {
                let tag = assets[indexPath.row].key
                let info = Registry.shared.infos[tag]
                var icon = Registry.shared.image(for: tag)
                if account?.network == "mainnet" {
                    icon = UIImage(named: "ntw_btc")
                } else if account?.network == "testnet" {
                    icon = UIImage(named: "ntw_testnet")
                }
                let satoshi = assets[indexPath.row].value
                cell.configure(tag: tag, info: info, icon: icon, satoshi: satoshi, isLiquid: isLiquid)
                return cell
            }
        case OverviewSection.transaction.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "OverviewTransactionCell", for: indexPath) as? OverviewTransactionCell {
                let transaction = transactions[indexPath.row]
                cell.setup(transaction: transaction, network: account?.network)
                cell.checkBlockHeight(transaction: transaction, blockHeight: SessionManager.shared.notificationManager.blockHeight)
                cell.checkTransactionType(transaction: transaction)
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch section {
        case OverviewSection.transaction.rawValue:
            return headerH
        case OverviewSection.asset.rawValue:
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
        switch section {
        case OverviewSection.account.rawValue:
            return showAccounts ? footerH : 1.0
        case OverviewSection.transaction.rawValue:
            return transactions.count == 0 ? footerH : 1.0
        default:
            return 1.0
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {

        switch indexPath.section {
        case OverviewSection.accountId.rawValue:
            return isLiquid && isAmp ? UITableView.automaticDimension : 0.0
        default:
            return UITableView.automaticDimension
        }
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {

        switch section {
        case OverviewSection.transaction.rawValue:
            return headerView("Transactions")
        case OverviewSection.asset.rawValue:
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
        switch section {
        case OverviewSection.account.rawValue:
            let isWatchonly = account?.isWatchonly ?? false
            return showAccounts && !isWatchonly ? footerView(.addAccount) : footerView(.none)
        case OverviewSection.transaction.rawValue:
            return transactions.count == 0 ? footerView(.noTransactions) : footerView(.none)
        default:
            return footerView(.none)
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        switch indexPath.section {
        case OverviewSection.account.rawValue:
            UIView.setAnimationsEnabled(true)
            if indexPath.row == 0 {
                showAccounts = !showAccounts
                tableView.reloadSections([OverviewSection.account.rawValue], with: .none)
                return
            } else {
                SessionManager.shared.activeWallet = accounts[indexPath.row].pointer
                presentingWallet = accounts[indexPath.row]
                showAccounts = !showAccounts
                self.transactions.removeAll()
                assets = [(key: String, value: UInt64)]()
                isLoading = true
                tableView.reloadData { [weak self] in
                    self?.handleRefresh()
                }
            }
        case OverviewSection.asset.rawValue:
            let storyboard = UIStoryboard(name: "Assets", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "AssetDetailTableViewController") as? AssetDetailTableViewController {
                let tag = assets[indexPath.row].key
                vc.tag = tag
                vc.asset = Registry.shared.infos[tag]
                vc.satoshi = presentingWallet?.satoshi?[tag]
                if isLiquid {
                    present(vc, animated: true, completion: nil)
                }
            }
        case OverviewSection.transaction.rawValue:
            let transaction = transactions[indexPath.row]
            let storyboard = UIStoryboard(name: "TransactionDetail", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "TransactionDetailViewController") as? TransactionDetailViewController {
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
    func tableView(_ tableView: UITableView, prefetchRowsAt indexPaths: [IndexPath]) {

        let filteredIndexPaths = indexPaths.filter { $0.section == OverviewSection.transaction.rawValue }

        if let row = filteredIndexPaths.last?.row {
            if self.callPage > 0 && row > ((self.callPage - 1 ) * Constants.trxPerPage) {
                if fetchTxs != nil && fetchTxs!.isPending {
                    print("----> null or pending")
                    return
                }
                fetchTxs = SessionManager.shared.transactions(first: UInt32(self.callPage * Constants.trxPerPage)).map { page in
                    self.transactions += page.list
                    self.callPage += 1
                    tableView.reloadSections([OverviewSection.transaction.rawValue], with: .none)
                }
            }
        }
    }
}

extension OverviewViewController {
    enum FooterType {
        case noTransactions
        case addAccount
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
        case .addAccount:
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: footerH))
            section.backgroundColor = .clear
            let addBtn = UIButton(frame: .zero)
            addBtn.setTitle("Add New Account", for: .normal)
            addBtn.setStyle(.outlinedGray)
            addBtn.translatesAutoresizingMaskIntoConstraints = false
            addBtn.addTarget(self, action: #selector(addAccount), for: .touchUpInside)
            section.addSubview(addBtn)

            NSLayoutConstraint.activate([
                addBtn.topAnchor.constraint(equalTo: section.topAnchor, constant: 0.0),
                addBtn.bottomAnchor.constraint(equalTo: section.bottomAnchor, constant: -10.0),
                addBtn.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 20.0),
                addBtn.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -20.0)
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

            if isLoading {
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
        transactionToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil, queue: .main, using: onNewTransaction)
        blockToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Block.rawValue), object: nil, queue: .main, using: onNewBlock)
        assetsUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue), object: nil, queue: .main, using: onAssetsUpdated)
        settingsUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Settings.rawValue), object: nil, queue: .main, using: refresh)
        tickerUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Settings.rawValue), object: nil, queue: .main, using: refresh)
        networkToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: onNetworkEvent)

        if subAccounts.count > 0 && !userWillLogout {
            handleRefresh()
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
    }
}

extension OverviewViewController: DialogWalletNameViewControllerDelegate {

    func didSave(_ name: String) {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try SessionManager.shared.renameSubaccount(subaccount: SessionManager.shared.activeWallet, newName: name)
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.loadAccounts()
        }.catch { e in
            DropAlert().error(message: e.localizedDescription)
            print(e.localizedDescription)
        }
    }
    func didCancel() {
    }
}

extension OverviewViewController: UserSettingsViewControllerDelegate {
    func userLogout() {
        userWillLogout = true
        self.presentedViewController?.dismiss(animated: true, completion: {
            DispatchQueue.main.async {
                let appDelegate = UIApplication.shared.delegate as? AppDelegate
                appDelegate?.logout(with: false)
            }
        })
    }
}
