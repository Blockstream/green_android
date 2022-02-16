import UIKit
import PromiseKit

enum TransactionSection: Int, CaseIterable {
    case amount = 0
    case fee = 1
    case status = 2
    case detail = 3
}

class TransactionViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!

    var wallet: WalletItem!
    var transaction: Transaction!

    private var account = AccountsManager.shared.current
    private var amounts: [(key: String, value: UInt64)] {
        get {
            return Transaction.sort(transaction.amounts)
        }
    }

    var viewInExplorerPreference: Bool {
        get {
            return UserDefaults.standard.bool(forKey: getNetwork() + "_view_in_explorer")
        }
        set {
            UserDefaults.standard.set(newValue, forKey: getNetwork() + "_view_in_explorer")
        }
    }

    private var transactionToken: NSObjectProtocol?
    private var blockToken: NSObjectProtocol?
    private var cantBumpFees: Bool {
        get {
            return SessionsManager.current.isResetActive ?? false ||
            !transaction.canRBF || account?.isWatchonly ?? false
        }
    }

    var isIncoming: Bool {
        get {
            transaction.type == "incoming"
        }
    }
    var isRedeposit: Bool {
        get {
            transaction.type == "redeposit"
        }
    }
    var isLiquid: Bool {
        get {
            account?.gdkNetwork?.liquid ?? false
        }
    }
    var headerH: CGFloat = 44.0

    override func viewDidLoad() {
        super.viewDidLoad()

        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)
        navBarSetup()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        transactionToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil, queue: .main, using: refreshTransaction)
        blockToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Block.rawValue), object: nil, queue: .main, using: refreshTransaction)
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
    }

    deinit {
        print("DEINIT")
    }

    func navBarSetup() {
        var status = NSLocalizedString("id_sent", comment: "")
        if isRedeposit {
            status = NSLocalizedString("id_redeposited", comment: "")
        } else if isIncoming {
            status = NSLocalizedString("id_received_on", comment: "")
        }
        let leftBarItem = ((Bundle.main.loadNibNamed("TransactionBarItem", owner: self, options: nil)![0] as? TransactionBarItem)!)
        leftBarItem.configure(status: status, account: wallet.localizedName()) {
            [weak self] in
            self?.navigationController?.popViewController(animated: true)
        }
        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: leftBarItem)

        let shareBtn = UIButton(type: .system)
        shareBtn.setImage(UIImage(named: "ic_share"), for: .normal)
        shareBtn.addTarget(self, action: #selector(shareButtonTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: shareBtn)
    }

    @objc func handleRefresh(_ sender: UIRefreshControl? = nil) {
        tableView.reloadData { [weak self] in
            self?.tableView.refreshControl?.endRefreshing()
        }
    }

    func editNote() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogNoteViewController") as? DialogNoteViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.prefill = transaction.memo
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    @objc func shareButtonTapped(_ sender: UIButton) {
        // We have more options in liquid for confidential txs
        if isLiquid {
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogShareTxOptionViewController") as? DialogShareTxOptionViewController {
                vc.modalPresentationStyle = .overFullScreen
                vc.delegate = self
                present(vc, animated: false, completion: nil)
            }
        } else {
            if let url = urlForTx() {
                let tx: [Any] = [url]
                let shareVC = UIActivityViewController(activityItems: tx, applicationActivities: nil)
                shareVC.popoverPresentationController?.sourceView = sender
                self.present(shareVC, animated: true, completion: nil)
            }
        }
    }

    func urlForTx() -> URL? {
        return URL(string: (account?.gdkNetwork?.txExplorerUrl ?? "") + self.transaction.hash)
    }

    func urlForTxUnblinded() -> URL? {
        return URL(string: (self.account?.gdkNetwork?.txExplorerUrl ?? "") + self.transaction.hash + self.transaction.blindingUrlString())
    }

    func blidingDataString() -> String? {
        let blindingData: Data? = try? JSONSerialization.data(withJSONObject: self.transaction.blindingData() ?? "", options: [])
        guard let data = blindingData else { return nil }
        guard let dataString = String(data: data, encoding: .utf8) else { return nil }
        return dataString
    }

    func openShare(_ option: TxShareOption) {
        switch option {
        case .confidential:
            if let url = urlForTx() {
                let shareVC = UIActivityViewController(activityItems: [url], applicationActivities: nil)
                present(shareVC, animated: true, completion: nil)
            }
        case .nonConfidential:
            if let url = urlForTxUnblinded() {
                let shareVC = UIActivityViewController(activityItems: [url], applicationActivities: nil)
                present(shareVC, animated: true, completion: nil)
            }
        case .unblindingData:
            if let str = blidingDataString() {
                let shareVC = UIActivityViewController(activityItems: [str], applicationActivities: nil)
                present(shareVC, animated: true, completion: nil)
            }
        }
    }

    func openExplorer(_ option: ExplorerOption) {
        var exUrl: URL?
        switch option {
        case .confidential:
            exUrl = urlForTx()
        case .nonConfidential:
            exUrl = urlForTxUnblinded()
        }
        guard let url = exUrl else { return }
        let host = url.host!.starts(with: "www.") ? String(url.host!.prefix(5)) : url.host!
        if viewInExplorerPreference {
            UIApplication.shared.open(url, options: [:])
            return
        }
        let message = String(format: NSLocalizedString("id_are_you_sure_you_want_to_view", comment: ""), host)
        let alert = UIAlertController(title: "", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (_: UIAlertAction) in
        })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_only_this_time", comment: ""), style: .default) { (_: UIAlertAction) in
            UIApplication.shared.open(url, options: [:])
        })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_always", comment: ""), style: .default) { (_: UIAlertAction) in
            self.viewInExplorerPreference = true
            UIApplication.shared.open(url, options: [:])
        })
        present(alert, animated: true, completion: nil)
    }

    func explorerAction() {
        if isLiquid {
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogExplorerOptionsViewController") as? DialogExplorerOptionsViewController {
                vc.modalPresentationStyle = .overFullScreen
                vc.delegate = self
                present(vc, animated: false, completion: nil)
            }
        } else {
            openExplorer(.confidential)
        }
    }

    func refreshTransaction(_ notification: Notification) {
        Guarantee().done { [weak self] _ in
            DispatchQueue.main.async {
                self?.tableView.reloadData {}
            }
        }
    }

    func copyToClipboard() {
        UIPasteboard.general.string = self.transaction.hash
        DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 1.0)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }

    func getFeeRate() -> UInt64 {
        var fee: UInt64 = self.transaction.feeRate
        if let estimates = getFeeEstimates(), estimates.count > 2 {
            fee = estimates[3]
        }
        return fee
    }

    func increaseFeeTapped() {
        if self.cantBumpFees { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then {
            try SessionsManager.current.getUnspentOutputs(details: ["subaccount": self.wallet?.pointer ?? 0, "num_confs": 1]).resolve()
        }.compactMap { data in
            let result = data["result"] as? [String: Any]
            let unspent = result?["unspent_outputs"] as? [String: Any]
            return ["previous_transaction": self.transaction.details,
                    "fee_rate": self.getFeeRate(),
                    "subaccount": self.wallet.pointer,
                    "utxos": unspent ?? [:]]
        }.then { details in
            gaios.createTransaction(details: details)
        }.ensure {
            self.stopAnimating()
        }.done { [weak self] tx in
            let storyboard = UIStoryboard(name: "Send", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
                vc.transaction = tx
                vc.inputType = .bumpFee
                vc.wallet = self?.wallet
                self?.navigationController?.pushViewController(vc, animated: true)
            }
        }.catch { err in
            print(err.localizedDescription)
        }
    }

    func didSelectAmountAt(_ index: Int) {
        let storyboard = UIStoryboard(name: "Assets", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AssetDetailTableViewController") as? AssetDetailTableViewController {
            if isLiquid {
                if let amount = isIncoming ? amounts[index] : amounts.filter({ $0.key == transaction.defaultAsset}).first {
                    vc.tag = amount.key
                    if let asset = Registry.shared.infos[amount.key] {
                        vc.asset = asset
                    } else {
                        vc.asset = AssetInfo(assetId: amount.key,
                                             name: NSLocalizedString("id_no_registered_name_for_this", comment: ""),
                                             precision: 0,
                                             ticker: NSLocalizedString("id_no_registered_ticker_for_this", comment: ""))
                    }
                    vc.satoshi = wallet?.satoshi?[amount.key] ?? 0
                    present(vc, animated: true) {}
                }
            }
        }
    }
}

extension TransactionViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return TransactionSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case TransactionSection.amount.rawValue:
            var items = 0
            if isLiquid {
                if isIncoming {
                    items = amounts.count
                } else {
                    items = 1
                }
                if isRedeposit {
                    items = 0
                }
            } else {
                if !isRedeposit {
                    items = 1
                }
            }
            return items
        case TransactionSection.fee.rawValue:
            return 1
        case TransactionSection.status.rawValue:
            return 1
        case TransactionSection.detail.rawValue:
            return 1
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        //
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case TransactionSection.amount.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionAmountCell") as? TransactionAmountCell {
                cell.configure(transaction: transaction, network: account?.network, index: indexPath.row)
                cell.selectionStyle = .none
                return cell
            }
        case TransactionSection.fee.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionFeeCell") as? TransactionFeeCell {
                cell.configure(transaction: transaction,
                               isLiquid: isLiquid,
                               feeAction: { [weak self] in
                    self?.increaseFeeTapped()
                })
                cell.selectionStyle = .none
                return cell
            }
        case TransactionSection.status.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionStatusCell") as? TransactionStatusCell {
                cell.configure(transaction: transaction, isLiquid: isLiquid)
                cell.selectionStyle = .none
                return cell
            }
        case TransactionSection.detail.rawValue:
            let noteAction: VoidToVoid? = { [weak self] in
                self?.editNote()
            }
            let explorerAction: VoidToVoid? = { [weak self] in
                self?.explorerAction()
            }
            let copyAction: VoidToVoid? = { [weak self] in
                self?.copyToClipboard()
            }
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionDetailCell") as? TransactionDetailCell {
                cell.configure(
                    transaction: transaction,
                    noteAction: noteAction,
                    explorerAction: explorerAction,
                    copyAction: copyAction
                )
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch section {
        case TransactionSection.detail.rawValue:
            return headerH
        default:
            return 1
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 1
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {

        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        switch section {
        case TransactionSection.detail.rawValue:
            return headerView("Transaction details")
        default:
            return headerView("")
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if indexPath.section == TransactionSection.amount.rawValue {
            didSelectAmountAt(indexPath.row)
        }
    }
}

extension TransactionViewController {
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
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 20),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -20)
        ])

        return section
    }
}

extension TransactionViewController: DialogNoteViewControllerDelegate {

    func didSave(_ note: String) {
        self.startAnimating()
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map(on: bgq) { _ in
            try gaios.SessionsManager.current.setTransactionMemo(txhash_hex: self.transaction.hash, memo: note, memo_type: 0)
            self.transaction.memo = note
            }.ensure {
                self.stopAnimating()
            }.done {
                self.tableView.reloadData {}
            }.catch { _ in}
    }

    func didCancel() { }
}

extension TransactionViewController: DialogExplorerOptionsViewControllerDelegate {
    func didSelect(_ option: ExplorerOption) {
        openExplorer(option)
    }
}

extension TransactionViewController: DialogShareTxOptionViewControllerDelegate {
    func didSelect(_ option: TxShareOption) {
        openShare(option)
    }
}
