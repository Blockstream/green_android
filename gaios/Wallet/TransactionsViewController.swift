import Foundation
import UIKit
import PromiseKit

class TransactionsController: UITableViewController {

    var presentingWallet: WalletItem?
    private var txs: [Transactions] = []
    private var fetchTxs: Promise<Void>?
    private var isSweep: Bool = false
    private let pointerKey = String(format: "%@_wallet_pointer", AccountsManager.shared.current?.id ?? "")
    private var pointerWallet: UInt32 { UInt32(UserDefaults.standard.integer(forKey: pointerKey)) }

    private var blockToken: NSObjectProtocol?
    private var transactionToken: NSObjectProtocol?
    private var assetsUpdatedToken: NSObjectProtocol?
    private var settingsUpdatedToken: NSObjectProtocol?

    var isResetActive: Bool {
        get {
            guard let twoFactorConfig = getGAService().getTwoFactorReset() else { return false }
            return twoFactorConfig.isResetActive
        }
    }
    var isDisputeActive: Bool {
        get {
            guard let twoFactorConfig = getGAService().getTwoFactorReset() else { return false }
            return twoFactorConfig.isDisputeActive
        }
    }
    var resetDaysRemaining: Int? {
        get {
            guard let twoFactorConfig = getGAService().getTwoFactorReset() else { return nil }
            return twoFactorConfig.daysRemaining
        }
    }

    var alertCards: [AlertCardType] = []

    override func viewDidLoad() {
        super.viewDidLoad()
        let nib = UINib(nibName: "TransactionTableCell", bundle: nil)
        let nib2 = UINib(nibName: "AlertCardCell", bundle: nil)
        tableView.delegate = self
        tableView.dataSource = self
        tableView.prefetchDataSource = self
        tableView.tableFooterView = getFooterView()
        tableView.register(nib, forCellReuseIdentifier: "TransactionTableCell")
        tableView.register(nib2, forCellReuseIdentifier: "AlertCardCell")
        tableView.allowsSelection = true
        tableView.isUserInteractionEnabled = true
        tableView.tableHeaderView = getAccountCardView()
        tableView.bounces = true
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 60
        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)

        let networkSelector = ((Bundle.main.loadNibNamed("NetworkSelectorBarItem", owner: self, options: nil)![0] as? NetworkSelectorBarItem)!)
        networkSelector.configure({[weak self] () in
            self?.switchNetwork()
        })
        let leftItem: UIBarButtonItem = UIBarButtonItem(customView: networkSelector)
        navigationItem.leftBarButtonItem = leftItem

        switch Registry.shared.failStatus() {
        case .all, .assets:
            if AccountsManager.shared.current!.network == "liquid" {
                presentDialogRegistryFail()
            }
        default:
            break
        }
        view.accessibilityIdentifier = AccessibilityIdentifiers.TransactionsScreen.view
    }

    func presentDialogRegistryFail() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogRegistryFailViewController") as? DialogRegistryFailViewController {
            vc.modalPresentationStyle = .overFullScreen
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                self.present(vc, animated: false, completion: nil)
            }
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        transactionToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil, queue: .main, using: onNewTransaction)
        blockToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Block.rawValue), object: nil, queue: .main, using: onNewBlock)
        assetsUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue), object: nil, queue: .main, using: onAssetsUpdated)
        settingsUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Settings.rawValue), object: nil, queue: .main, using: onSettingsTransaction)
        if presentingWallet != nil {
            showWallet()
        }
        handleRefresh()
        checkFiatRate()
        loadAlertCards()
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
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        guard let headerView = tableView.tableHeaderView else { return }
        let height = headerView.systemLayoutSizeFitting(UIView.layoutFittingCompressedSize).height
        if height != headerView.frame.size.height {
            headerView.frame.size.height = height
            tableView.tableHeaderView = headerView
        }
        guard let footerView = tableView.tableFooterView else { return }
        let fHeight = footerView.systemLayoutSizeFitting(UIView.layoutFittingCompressedSize).height
        if fHeight != footerView.frame.size.height {
            footerView.frame.size.height = fHeight
            tableView.tableFooterView = footerView
        }
    }

    @IBAction func notifications(_ sender: Any) {
        self.performSegue(withIdentifier: "notifications", sender: nil)
    }

    @IBAction func settings(_ sender: Any) {
        self.performSegue(withIdentifier: "settings", sender: nil)
    }

    func checkFiatRate() {
        // diplay errore if fiat_rate is missing
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            return Guarantee()
        }.map(on: bgq) { () -> (String?, String) in
            return Balance.convert(details: ["satoshi": 0])?.get(tag: "fiat") ?? (nil, "")
        }.done { (amount, _) in
            if amount == nil {
                self.showError(NSLocalizedString("id_your_favourite_exchange_rate_is", comment: ""))
            }
        }.catch { err in
            print(err.localizedDescription)
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
        // We will use Card2faType.reactivate for expired coins
    }

    func onNewBlock(_ notification: Notification) {
        // update txs only if pending txs > 0
        if txs.first?.list.filter({ $0.blockHeight == 0 }).first != nil {
            self.loadTransactions().done {
                self.showTransactions()
            }.catch { _ in }
        }
    }

    func onAssetsUpdated(_ notification: Notification) {
        Guarantee()
            .compactMap { Registry.shared.cache() }
            .done { self.showTransactions() }
            .catch { err in
                print(err.localizedDescription)
        }
    }

    func onNewTransaction(_ notification: Notification) {
        guard let dict = notification.userInfo as NSDictionary? else { return }
        guard let subaccounts = dict["subaccounts"] as? [UInt32] else { return }
        if subaccounts.contains(pointerWallet) {
            handleRefresh()
        }
    }

    func onSettingsTransaction(_ notification: Notification) {
        self.showWallet()
        self.showTransactions()
    }

    func showTransactions() {

        let count = txs.map { $0.list.count }.reduce(0, +)
        if count == 0 {
            tableView.tableFooterView = getFooterView()
        } else {
            tableView.tableFooterView = UIView()
        }
        if tableView.refreshControl?.isRefreshing ?? false {
            tableView.refreshControl?.endRefreshing()
        }
        tableView.reloadData()
    }

    func reloadRegistry() {

        self.startAnimating()
        Registry.shared.load().done { () in
            self.stopAnimating()
            self.loadAlertCards()
            self.tableView.reloadData()
        }.catch { _ in }
    }

    override func numberOfSections(in tableView: UITableView) -> Int {
        // section 0 is dadicated to cards
        return txs.count + 1
    }

    public override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if section == 0 {
            return alertCards.count
        } else {
            return txs[section - 1].list.count
        }
    }

    override func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        guard let cell = cell as? TransactionTableCell else { return }
        let transactions = txs[indexPath.section - 1]
        let tx = transactions.list[indexPath.row]
        cell.checkBlockHeight(transaction: tx, blockHeight: getGAService().getBlockheight())
        cell.checkTransactionType(transaction: tx)
    }

    public override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        if indexPath.section == 0 {
            guard let cell = tableView.dequeueReusableCell(withIdentifier: "AlertCardCell", for: indexPath) as? AlertCardCell else { fatalError("Fail to dequeue reusable cell") }
            let alertCard = alertCards[indexPath.row]
            switch alertCard {
            case .reset, .dispute, .reactivate:
                cell.configure(alertCards[indexPath.row],
                               onLeft: {[weak self] in
                                self?.performSegue(withIdentifier: "reactivate2fa", sender: self)
                               },
                               onRight: {[weak self] in
                                self?.performSegue(withIdentifier: "leaarnMore2fa", sender: self)
                               })
            case .assetsRegistryFail, .iconsRegistryFail:
                cell.configure(alertCards[indexPath.row],
                               onLeft: nil,
                               onRight: {[weak self] in
                                self?.reloadRegistry()
                               })
            }

            cell.selectionStyle = .none
            return cell
        }

        guard let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionTableCell", for: indexPath) as? TransactionTableCell else { fatalError("Fail to dequeue reusable cell") }
        let transactions = txs[indexPath.section - 1]
        let tx = transactions.list[indexPath.row]
        cell.setup(with: tx)
        return cell
    }

    public override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if indexPath.section == 0 {
            return
        }
        let transactions = txs[indexPath.section - 1]
        let tx = transactions.list[indexPath.row]
        showTransaction(tx: tx)
    }

    override func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 0
    }

    @objc func handleRefresh(_ sender: UIRefreshControl? = nil) {
        self.loadWallet()
        .compactMap {
            self.showWallet()
        }.then {
            self.loadTransactions()
        }.done { _ in
            self.showTransactions()
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    func loadTransactions(_ pageId: Int = 0) -> Promise<Void> {
        return getTransactions(self.pointerWallet, first: 0)
        .map { txs in
            self.txs.removeAll()
            self.txs.append(txs)
        }
    }

    func getFooterView() -> FooterView {
        let view: FooterView = ((Bundle.main.loadNibNamed("FooterView", owner: self, options: nil)![0] as? FooterView)!)
        return view
    }

    func getAccountCardView() -> WalletFullCardView? {
        let view: WalletFullCardView = ((Bundle.main.loadNibNamed("WalletFullCardView", owner: self, options: nil)![0] as? WalletFullCardView)!)
        view.receiveView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.receiveToWallet)))
        view.sendView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.sendfromWallet)))
        view.sweepView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.sweepFromWallet)))
        view.stackButton.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.wallets)))
        view.assetsView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.showAssets)))
        return view
    }

    func loadWallet() -> Promise<Void> {
        return getSubaccount(self.pointerWallet)
        .recover { _ in
            getSubaccount(0)
        }.map { wallet in
            UserDefaults.standard.set(Int(wallet.pointer), forKey: self.pointerKey)
            UserDefaults.standard.synchronize()
            self.presentingWallet = wallet
        }
    }

    func showWallet() {
        let view = self.tableView.tableHeaderView as? WalletFullCardView
        if let wallet = presentingWallet {
            view?.setup(with: wallet)
        }
    }

    @objc func showAssets(_ sender: UIButton) {
        guard presentingWallet?.satoshi != nil else { return }
        self.performSegue(withIdentifier: "assets", sender: self)
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

    @objc func wallets(_ sender: UIButton) {
        self.performSegue(withIdentifier: "wallets", sender: self)
    }

    @objc func sendfromWallet(_ sender: UIButton) {
        let account = AccountsManager.shared.current
        if (account?.gdkNetwork?.liquid ?? false) && presentingWallet?.btc == 0 {
            let message = NSLocalizedString("id_insufficient_lbtc_to_send_a", comment: "")
            let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_receive", comment: ""), style: .default) { _ in
                self.performSegue(withIdentifier: "receive", sender: self)
            })
            self.present(alert, animated: true, completion: nil)
        }
        return self.performSegue(withIdentifier: "send", sender: self)
    }

    @objc func sweepFromWallet(_ sender: UIButton) {
        isSweep = true
        self.performSegue(withIdentifier: "send", sender: self)
    }

    @objc func receiveToWallet(_ sender: UIButton) {
        self.performSegue(withIdentifier: "receive", sender: self)
    }

    func showTransaction(tx: Transaction) {
        self.performSegue(withIdentifier: "detail", sender: tx)
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let nextController = segue.destination as? SendBtcViewController {
            nextController.isSweep = isSweep
            nextController.wallet = presentingWallet
        } else if let nextController = segue.destination as? ReceiveBtcViewController {
            nextController.wallet = presentingWallet
        } else if let nextController = segue.destination as? TransactionDetailViewController {
            nextController.transaction = sender as? Transaction
            nextController.wallet = presentingWallet
        } else if let nextController = segue.destination as? AccountsViewController {
            nextController.subaccountDelegate = self
        } else if let nextController = segue.destination as? AssetsListTableViewController {
            nextController.wallet = presentingWallet
            nextController.title = presentingWallet!.localizedName()
        }
    }
}

extension TransactionsController: UITableViewDataSourcePrefetching {
    func tableView(_ tableView: UITableView, prefetchRowsAt indexPaths: [IndexPath]) {
        let filteredIndexPaths = indexPaths.filter { $0.section > 0 }
        if !filteredIndexPaths.contains(where: { $0.row >= self.txs[$0.section - 1].list.count - 1 }) { return }
        if self.txs.count == 0 { return }
        if self.txs.last?.list.count == 0 { return }
        if fetchTxs != nil && fetchTxs!.isPending { return }
        let count = txs.map { $0.list.count }.reduce(0, +)
        fetchTxs = getTransactions(self.pointerWallet, first: UInt32(count)).map { txs in
            self.txs.append(txs)
            self.showTransactions()
        }
    }
}

extension TransactionsController: SubaccountDelegate {
    func onChange(_ wallet: WalletItem) {
        // Replace wallet and balance
        UserDefaults.standard.set(Int(wallet.pointer), forKey: pointerKey)
        UserDefaults.standard.synchronize()
        presentingWallet = wallet
        showWallet()
        // Empty and reload transactions list
        txs.removeAll()
        tableView.reloadData()
        loadTransactions().done {
            self.showTransactions()
        }.catch { _ in }
    }
}

extension TransactionsController: UIViewControllerTransitioningDelegate {
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

extension TransactionsController: DrawerNetworkSelectionDelegate {

    func logout() -> Promise<Void> {
        let appDelegate = UIApplication.shared.delegate as? AppDelegate
        let bgq = DispatchQueue.global(qos: .background)
        return firstly {
            self.startLoader(message: NSLocalizedString("id_logout", comment: ""))
            return Guarantee()
        }.map(on: bgq) {
            appDelegate?.disconnect()
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
