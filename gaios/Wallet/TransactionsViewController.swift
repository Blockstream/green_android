import Foundation
import UIKit
import PromiseKit

protocol SubaccountDelegate: class {
    func onChange(_ pointer: UInt32)
}

class TransactionsController: UITableViewController {

    private var presentingWallet: WalletItem?
    private var txs: [Transactions] = []
    private var fetchTxs: Promise<Void>?
    private var isSweep: Bool = false
    private let pointerKey = String(format: "%@_wallet_pointer", getNetwork())
    private var pointerWallet: UInt32 { UInt32(UserDefaults.standard.integer(forKey: pointerKey)) }

    private var blockToken: NSObjectProtocol?
    private var transactionToken: NSObjectProtocol?
    private var assetsUpdatedToken: NSObjectProtocol?

    lazy var noTransactionsLabel: UILabel = {
        let noTransactionsLabel = UILabel(frame: CGRect(x: self.view.frame.size.width/2 - 100, y: self.tableView.tableHeaderView!.frame.height, width: 200, height: self.view.frame.size.height - self.tableView.tableHeaderView!.frame.height))
        noTransactionsLabel.font = UIFont.systemFont(ofSize: 16, weight: .regular)
        noTransactionsLabel.textColor = UIColor.customTitaniumLight()
        noTransactionsLabel.numberOfLines = 0
        noTransactionsLabel.textAlignment = .center
        noTransactionsLabel.text = NSLocalizedString("id_your_transactions_will_be_shown", comment: "")
        return noTransactionsLabel
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        let nib = UINib(nibName: "TransactionTableCell", bundle: nil)
        tableView.delegate = self
        tableView.dataSource = self
        tableView.prefetchDataSource = self
        tableView.tableFooterView = UIView()
        tableView.register(nib, forCellReuseIdentifier: "TransactionTableCell")
        tableView.allowsSelection = true
        tableView.isUserInteractionEnabled = true
        tableView.tableHeaderView = getAccountCardView()
        tableView.bounces = true
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 60
        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: false)
        transactionToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil, queue: .main, using: onNewTransaction)
        blockToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Block.rawValue), object: nil, queue: .main, using: onNewBlock)
        assetsUpdatedToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AssetsUpdated.rawValue), object: nil, queue: .main, using: onAssetsUpdated)
        handleRefresh()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        guard let controller = self.tabBarController as? TabViewController else { return }
        controller.snackbar.isHidden = false
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationController?.setNavigationBarHidden(false, animated: false)
        if let token = transactionToken {
            NotificationCenter.default.removeObserver(token)
        }
        if let token = blockToken {
            NotificationCenter.default.removeObserver(token)
        }
        if let token = assetsUpdatedToken {
            NotificationCenter.default.removeObserver(token)
        }
        guard let controller = self.tabBarController as? TabViewController else { return }
        controller.snackbar.isHidden = true
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        guard let headerView = tableView.tableHeaderView else { return }
        let height = headerView.systemLayoutSizeFitting(UIView.layoutFittingCompressedSize).height
        if height != headerView.frame.size.height {
            headerView.frame.size.height = height
            tableView.tableHeaderView = headerView
        }
    }

    func onNewBlock(_ notification: Notification) {
        self.loadTransactions().done {
            self.reload()
        }.catch { _ in }
    }

    func onAssetsUpdated(_ notification: Notification) {
        Registry.shared.cache().done { _ in
            self.reload()
        }.catch { err in
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

    func reload() {
        if view.subviews.contains(noTransactionsLabel) {
            noTransactionsLabel.removeFromSuperview()
        }
        let count = txs.map { $0.list.count }.reduce(0, +)
        if count == 0 {
            view.addSubview(noTransactionsLabel)
        }
        tableView.reloadData()
    }

    override func numberOfSections(in tableView: UITableView) -> Int {
        return txs.count
    }

    public override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return txs[section].list.count
    }

    override func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        guard let cell = cell as? TransactionTableCell else { return }
        let transactions = txs[indexPath.section]
        let tx = transactions.list[indexPath.row]
        cell.checkBlockHeight(transaction: tx, blockHeight: getGAService().getBlockheight())
        cell.checkTransactionType(transaction: tx)
    }

    public override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionTableCell", for: indexPath) as? TransactionTableCell else { fatalError("Fail to dequeue reusable cell") }
        let transactions = txs[indexPath.section]
        let tx = transactions.list[indexPath.row]
        cell.setup(with: tx)
        return cell
    }

    public override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let transactions = txs[indexPath.section]
        let tx = transactions.list[indexPath.row]
        showTransaction(tx: tx)
    }

    override func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 0
    }

    @objc func handleRefresh(_ sender: UIRefreshControl? = nil) {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            Registry.shared.cache()
        }.then(on: bgq) { _ -> Promise<Void> in
            when(fulfilled: [self.loadWallet(), self.loadTransactions()])
        }.ensure {
            self.stopAnimating()
            if self.tableView.refreshControl!.isRefreshing {
                self.tableView.refreshControl!.endRefreshing()
            }
        }.done { _ in
            self.reload()
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

    func getAccountCardView() -> WalletFullCardView? {
        let view: WalletFullCardView = ((Bundle.main.loadNibNamed("WalletFullCardView", owner: self, options: nil)![0] as? WalletFullCardView)!)
        view.receiveView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.receiveToWallet)))
        view.sendView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.sendfromWallet)))
        view.sweepView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.sweepFromWallet)))
        view.stackButton.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.wallets)))
        view.assetsView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.showAssets)))
        view.networkSelectorStackView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.switchNetwork)))
        return view
    }

    func loadWallet() -> Promise<Void> {
        return getSubaccount(self.pointerWallet)
        .recover { _ in
            getSubaccount(0)
        }.map { wallet in
            self.onChange(wallet.pointer)
            self.presentingWallet = wallet
            let view = self.tableView.tableHeaderView as? WalletFullCardView
            view?.setup(with: wallet)
        }
    }

    @objc func showAssets(_ sender: UIButton) {
        guard presentingWallet?.satoshi != nil else { return }
        self.performSegue(withIdentifier: "assets", sender: self)
    }

    @objc func switchNetwork() {
        self.performSegue(withIdentifier: "switch_network", sender: self)
    }

    @objc func wallets(_ sender: UIButton) {
        self.performSegue(withIdentifier: "wallets", sender: self)
    }

    @objc func sendfromWallet(_ sender: UIButton) {
        if getGdkNetwork(getNetwork()).liquid && presentingWallet?.btc == 0 {
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
            nextController.presentationController?.delegate = self
        } else if let nextController = segue.destination as? AssetsListTableViewController {
            nextController.wallet = presentingWallet
            nextController.title = presentingWallet!.localizedName()
        } else if let networkSelector = segue.destination as? NetworkSelectionSettings {
            networkSelector.transitioningDelegate = self
            networkSelector.modalPresentationStyle = .custom
            networkSelector.onSelection = networkDidChange
            networkSelector.isLanding = false
        }
    }

    func networkDidChange() {
        onFirstInitialization(network: getNetwork())
        DispatchQueue.main.async {
            let appDelegate = UIApplication.shared.delegate as? AppDelegate
            appDelegate?.logout(with: true)
        }
    }
}

extension TransactionsController: UITableViewDataSourcePrefetching {
    func tableView(_ tableView: UITableView, prefetchRowsAt indexPaths: [IndexPath]) {
        if !indexPaths.contains { $0.row >= self.txs[$0.section].list.count - 1 } { return }
        if self.txs.count == 0 { return }
        if self.txs.last?.list.count == 0 { return }
        if fetchTxs != nil && fetchTxs!.isPending { return }
        let count = txs.map { $0.list.count }.reduce(0, +)
        fetchTxs = getTransactions(self.pointerWallet, first: UInt32(count)).map { txs in
            self.txs.append(txs)
            self.reload()
        }
    }
}

extension TransactionsController: SubaccountDelegate {
    func onChange(_ pointer: UInt32) {
        UserDefaults.standard.set(Int(pointer), forKey: pointerKey)
        UserDefaults.standard.synchronize()
    }
}

extension TransactionsController: UIViewControllerTransitioningDelegate {
    func presentationController(forPresented presented: UIViewController, presenting: UIViewController?, source: UIViewController) -> UIPresentationController? {
        return ModalPresentationController(presentedViewController: presented, presenting: presenting)
    }

    func animationController(forPresented presented: UIViewController, presenting: UIViewController, source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        ModalAnimator(isPresenting: true)
    }

    func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        ModalAnimator(isPresenting: false)
    }
}

extension TransactionsController: UIAdaptivePresentationControllerDelegate {

    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        handleRefresh()
    }
}
