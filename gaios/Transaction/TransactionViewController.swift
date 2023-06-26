import UIKit

import gdk

enum TransactionSection: Int, CaseIterable {
    case amount = 0
    case fee = 1
    case status = 2
    case detail = 3
    case note = 4
}

protocol TransactionViewControllerDelegate: AnyObject {
    func onMemoEdit()
}

class TransactionViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!

    weak var delegate: TransactionViewControllerDelegate?

    var wallet: WalletItem!
    var transaction: Transaction! { didSet { assetAmountList = AssetAmountList(transaction.amountsWithoutFees) }}
    var assetAmountList: AssetAmountList!

    var isWatchonly: Bool {
        WalletManager.current?.account.isWatchonly ?? false
    }
    var isSinglesig: Bool {
        transaction.subaccountItem?.isSinglesig ?? true
    }

    var viewInExplorerPreference: Bool {
        get {
            return UserDefaults.standard.bool(forKey: wallet.gdkNetwork.chain + "_view_in_explorer")
        }
        set {
            UserDefaults.standard.set(newValue, forKey: wallet.gdkNetwork.chain + "_view_in_explorer")
        }
    }

    private var transactionToken: NSObjectProtocol?
    private var blockToken: NSObjectProtocol?
    private var cantBumpFees: Bool {
        return wallet.session?.isResetActive ?? false ||
            !transaction.canRBF || isWatchonly
    }

    var headerH: CGFloat = 44.0

    private var hideBalance: Bool {
        return UserDefaults.standard.bool(forKey: AppStorage.hideBalance)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        tableView.refreshControl = UIRefreshControl()
        tableView.refreshControl!.tintColor = UIColor.white
        tableView.refreshControl!.addTarget(self, action: #selector(handleRefresh(_:)), for: .valueChanged)
        navBarSetup()

        AnalyticsManager.shared.recordView(.transactionDetails, sgmt: AnalyticsManager.shared.sessSgmt(AccountsRepository.shared.current))
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

    func navBarSetup() {
        title = "id_details".localized
//        var status = NSLocalizedString("id_sent", comment: "")
//        if transaction.type == .redeposit {
//            status = NSLocalizedString("id_redeposited", comment: "")
//        } else if transaction.type == .incoming {
//            status = NSLocalizedString("id_received_on", comment: "")
//        }
//        let leftBarItem = ((Bundle.main.loadNibNamed("TransactionBarItem", owner: self, options: nil)![0] as? TransactionBarItem)!)
//        leftBarItem.configure(status: status, account: "") {
//            [weak self] in
//            self?.navigationController?.popViewController(animated: true)
//        }
//        navigationItem.leftBarButtonItem = UIBarButtonItem(customView: leftBarItem)

        let shareBtn = UIButton(type: .system)
        shareBtn.setImage(UIImage(named: "ic_export"), for: .normal)
        shareBtn.addTarget(self, action: #selector(shareButtonTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: shareBtn)
    }

    @objc func handleRefresh(_ sender: UIRefreshControl? = nil) {
        tableView.reloadData { [weak self] in
            self?.tableView.refreshControl?.endRefreshing()
        }
    }

    func editNote() {
        if isWatchonly && !isSinglesig { return }
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogEditViewController") as? DialogEditViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.prefill = transaction.memo
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    @objc func shareButtonTapped(_ sender: UIButton) {

        AnalyticsManager.shared.shareTransaction(account: AccountsRepository.shared.current, isShare: true)
        // We have more options in liquid for confidential txs
        if transaction.isLiquid {
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
        return URL(string: (wallet.gdkNetwork.txExplorerUrl ?? "") + self.transaction.hash)
    }

    func urlForTxUnblinded() -> URL? {
        return URL(string: (wallet.gdkNetwork.txExplorerUrl ?? "") + self.transaction.hash + self.transaction.blindingUrlString())
    }

    func blidingDataString() -> String? {
        let blinding = self.transaction.blindingData()
        let jsonData = try? JSONEncoder().encode(blinding)
        let jsonString = String(data: jsonData ?? Data(), encoding: .utf8)
        return jsonString
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
        if transaction.isLiquid {
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
        DispatchQueue.main.async {
            self.tableView.reloadData()
        }
    }

    func copyToClipboard(_ value: String) {

        AnalyticsManager.shared.shareTransaction(account: AccountsRepository.shared.current, isShare: false)

        UIPasteboard.general.string = value
        DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 1.0)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }

    func getFeeRate() async -> UInt64 {
        var fee: UInt64 = self.transaction.feeRate
        if let estimates = try? await wallet.session?.getFeeEstimates(), estimates.count > 2 {
            fee = estimates[3]
        }
        return fee
    }

    func increaseFeeTapped() {
        if self.cantBumpFees { return }
        guard let session = wallet.session else { return }
        Task {
            let unspentOutputs = try? await session.getUnspentOutputs(subaccount: self.wallet?.pointer ?? 0, numConfs: 1)
            let details = ["previous_transaction": self.transaction.details,
                           "fee_rate": await self.getFeeRate() + 10,
                           "subaccount": self.wallet.pointer,
                           "utxos": unspentOutputs]
            let tx = try? await session.createTransaction(tx: Transaction(details))
            let storyboard = UIStoryboard(name: "Send", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
                vc.viewModel = SendViewModel(account: self.wallet,
                                             inputType: .bumpFee,
                                             transaction: tx,
                                             input: nil)
                vc.fixedWallet = true
                vc.fixedAsset = true
                self.navigationController?.pushViewController(vc, animated: true)
            }
        }
    }

    func didSelectAmountAt(_ index: Int) {
        if !transaction.isLiquid { return }
        Task {
            let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogDetailViewController") as? DialogDetailViewController {
                let assetAmount = assetAmountList.amounts[index]
                vc.tag = assetAmount.0
                vc.asset = (assetAmountList?.assets[vc.tag])!
                vc.satoshi = assetAmount.1
                vc.modalPresentationStyle = .overFullScreen
                present(vc, animated: false, completion: nil)
            }
        }
    }
}

extension TransactionViewController: UITableViewDelegate, UITableViewDataSource {

    var sections: [TransactionSection] {
        if wallet.gdkNetwork.lightning {
            return [.amount, .status, .note]
        } else {
            return TransactionSection.allCases
        }
    }

    func numberOfSections(in tableView: UITableView) -> Int {
        return sections.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch sections[section] {
        case TransactionSection.amount:
            return assetAmountList.amounts.count
        case TransactionSection.fee:
            return 1
        case TransactionSection.status:
            return 1
        case TransactionSection.detail:
            return 1
        case TransactionSection.note:
            return 1
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        switch sections[indexPath.section] {
        case .amount:
            let copyAmount: ((String) -> Void)? = { [weak self] value in
                self?.copyToClipboard(value)
            }
            let copyRecipient: ((String) -> Void)? = { [weak self] value in
                self?.copyToClipboard(value)
            }
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionAmountCell") as? TransactionAmountCell {
                let assetAmount = assetAmountList.amounts[indexPath.row]
                cell.configure(tx: transaction,
                               isLightning: wallet.type.lightning,
                               id: assetAmount.0,
                               value: assetAmount.1,
                               hideBalance: hideBalance,
                               copyAmount: copyAmount, copyRecipient: copyRecipient)
                cell.selectionStyle = .none
                return cell
            }
        case .fee:
            let feeAction: VoidToVoid? = { [weak self] in
                self?.increaseFeeTapped()
            }
            let copyFee: ((String) -> Void)? = { [weak self] value in
                self?.copyToClipboard(value)
            }
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionFeeCell") as? TransactionFeeCell {
                cell.configure(transaction: transaction,
                               isLiquid: transaction.isLiquid,
                               hideBalance: hideBalance,
                               feeAction: feeAction,
                               copyFee: copyFee)
                cell.selectionStyle = .none
                return cell
            }
        case .status:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionStatusCell") as? TransactionStatusCell {
                let blockHeight = wallet.session?.blockHeight
                cell.configure(transaction: transaction, blockHeight: blockHeight ?? 0)
                cell.selectionStyle = .none
                return cell
            }
        case .detail:
            let explorerAction: VoidToVoid? = { [weak self] in
                self?.explorerAction()
            }
            let copyHash: ((String) -> Void)? = { [weak self] value in
                self?.copyToClipboard(value)
            }
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionDetailCell") as? TransactionDetailCell {
                cell.configure(
                    transaction: transaction,
                    explorerAction: explorerAction,
                    copyHash: copyHash
                )
                cell.selectionStyle = .none
                return cell
            }
        case .note:
            let noteAction: VoidToVoid? = { [weak self] in
                self?.editNote()
            }

            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionDetailNoteCell") as? TransactionDetailNoteCell {
                cell.configure(
                    transaction: transaction,
                    noteAction: noteAction
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
        return 0.1
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0.1
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {

        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        return nil
//        switch section {
//        case TransactionSection.detail.rawValue:
//            return headerView(NSLocalizedString("id_transaction_details", comment: ""))
//        default:
//            return headerView("")
//        }
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
//    func headerView(_ txt: String) -> UIView {
//        if txt == "" {
//            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: 1.0))
//            section.backgroundColor = .clear
//            return section
//        }
//        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
//        section.backgroundColor = UIColor.customTitaniumDark()
//        let title = UILabel(frame: .zero)
//        title.text = txt
//        title.numberOfLines = 0
//
//        title.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
//        title.textColor = .yellow.withAlphaComponent(0.4)
//
//        title.translatesAutoresizingMaskIntoConstraints = false
//        section.addSubview(title)
//
//        NSLayoutConstraint.activate([
//            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
//            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 20),
//            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -20)
//        ])
//
//        return section
//    }
}

extension TransactionViewController: DialogEditViewControllerDelegate {

    func didSave(_ note: String) {
        self.startAnimating()
        Task {
            try? await self.wallet.session?.session?.setTransactionMemo(txhash_hex: self.transaction.hash, memo: note, memo_type: 0)
            self.transaction.memo = note
            self.delegate?.onMemoEdit()
            self.stopAnimating()
            self.tableView.reloadData()
        }
    }

    func didClose() { }
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
