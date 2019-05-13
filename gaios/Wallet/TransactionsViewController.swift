import Foundation
import UIKit
import PromiseKit

protocol SubaccountDelegate: class {
    func onChange(_ pointer: UInt32)
}

class TransactionsController: UITableViewController {

    var pointerWallet: UInt32 = 0
    var presentingWallet: WalletItem?
    var txs: [Transactions] = []
    var fetchTxs: Promise<Void>?

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
        tableView.tableHeaderView = getWalletCardView()
        tableView.bounces = true
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 60
        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: true)
        NotificationCenter.default.addObserver(self, selector: #selector(self.onNewTransaction(_:)), name: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(self.onNewBlock(_:)), name: NSNotification.Name(rawValue: EventType.Block.rawValue), object: nil)
        loadWallet()
        loadTransactions()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        guard let controller = self.tabBarController as? TabViewController else { return }
        controller.snackbar.isHidden = false
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationController?.setNavigationBarHidden(false, animated: true)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.Block.rawValue), object: nil)
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

    @objc func onNewBlock(_ notification: NSNotification) {
        self.loadTransactions()
    }

    @objc func onNewTransaction(_ notification: NSNotification) {
        guard let dict = notification.userInfo as NSDictionary? else { return }
        guard let subaccounts = dict["subaccounts"] as? [UInt32] else { return }
        if subaccounts.contains(pointerWallet) {
            self.loadWallet()
            self.loadTransactions()
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

    @objc func handleRefresh(_ sender: UIRefreshControl?) {
        self.loadWallet()
        self.loadTransactions()
    }

    func loadTransactions(_ pageId: Int = 0) {
        getTransactions(self.pointerWallet, pageId: pageId)
        .done { txs in
            self.txs.removeAll()
            self.txs.append(txs)
            self.reload()
        }.ensure {
            if self.tableView.refreshControl!.isRefreshing {
                self.tableView.refreshControl!.endRefreshing()
            }
        }.catch { _ in }
    }

    func getWalletCardView() -> WalletFullCardView? {
        let view: WalletFullCardView = ((Bundle.main.loadNibNamed("WalletFullCardView", owner: self, options: nil)![0] as? WalletFullCardView)!)
        view.receiveView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.receiveToWallet)))
        view.sendView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.sendfromWallet)))
        view.stackButton.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.wallets)))
        view.assetsView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(self.showAssets)))
        return view
    }

    func loadWallet() {
        guard let twoFactorReset = getGAService().getTwoFactorReset() else { return }
        guard let settings = getGAService().getSettings() else { return }
        let network = getGdkNetwork(getNetwork())
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then {
            getSubaccount(self.pointerWallet)
        }.ensure {
            self.stopAnimating()
        }.done { wallet in
            self.presentingWallet = wallet
            guard let view = self.tableView.tableHeaderView as? WalletFullCardView else { return }
            let satoshi = wallet.btc.satoshi
            view.balance.text = String.toBtc(satoshi: satoshi, showDenomination: false)
            view.unit.text = settings.denomination.toString()
            view.balanceFiat.text = "≈ " + String.toFiat(satoshi: satoshi)
            view.walletName.text = self.presentingWallet!.localizedName()
            view.networkImage.image = UIImage(named: network.icon!)
            view.assetsLabel.text = String(format: NSLocalizedString(wallet.balance.count == 1 ? "id_d_asset_in_this_account" : "id_d_assets_in_this_account", comment: ""), wallet.balance.count)
            if twoFactorReset.isResetActive {
                view.actionsView.isHidden = true
            } else if getGAService().isWatchOnly {
                view.sendImage.image = UIImage(named: "qr_sweep")
                view.sendLabel.text = NSLocalizedString("id_sweep", comment: "")
            }
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    @objc func showAssets(_ sender: UIButton) {
        guard presentingWallet?.balance != nil else { return }
        self.performSegue(withIdentifier: "assets", sender: self)
    }

    @objc func wallets(_ sender: UIButton) {
        self.performSegue(withIdentifier: "wallets", sender: self)
    }

    @objc func sendfromWallet(_ sender: UIButton) {
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
            nextController.wallet = presentingWallet
        } else if let nextController = segue.destination as? ReceiveBtcViewController {
            nextController.wallet = presentingWallet
        } else if let nextController = segue.destination as? TransactionDetailViewController {
            nextController.transaction = sender as? Transaction
            nextController.wallet = presentingWallet
        } else if let nextController = segue.destination as? WalletsViewController {
            nextController.subaccountDelegate = self
        } else if let nextController = segue.destination as? AssetsTableViewController {
            nextController.wallet = presentingWallet
            nextController.title = presentingWallet!.localizedName()
        }
    }

}

extension TransactionsController: UITableViewDataSourcePrefetching {
    func tableView(_ tableView: UITableView, prefetchRowsAt indexPaths: [IndexPath]) {
        let fetch = indexPaths.contains { $0.row >= self.txs[$0.section].list.count - 1 }
        let pageId = Int(self.txs.last?.nextPageId ?? 0)
        if !fetch { return }
        if pageId == 0 { return }
        if fetchTxs != nil && fetchTxs!.isPending { return }
        fetchTxs = getTransactions(self.pointerWallet, pageId: pageId).map { txs in
            self.txs.append(txs)
            self.reload()
        }
    }
}

extension TransactionsController: SubaccountDelegate {
    func onChange(_ pointer: UInt32) {
        self.pointerWallet = pointer
    }
}
