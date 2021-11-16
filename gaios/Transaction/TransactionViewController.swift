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
            return SessionManager.shared.isResetActive ?? false ||
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

        //        cellTypes.remove(at: cellTypes.firstIndex(of: .asset)!)
        //        if isLiquid {
        //            if isIncoming {
        //                for _ in amounts {
        //                    cellTypes.insert(.asset, at: 1)
        //                }
        //            } else {
        //                cellTypes.insert(.asset, at: 1)
        //            }
        //            _ = isRedeposit || amounts.count > 0 ?
        //                cellTypes.remove(at: cellTypes.firstIndex(of: .amount)!) :
        //                cellTypes.remove(at: cellTypes.firstIndex(of: .fee)!)
        //        }
        //        _ = isIncoming || isRedeposit ? cellTypes.remove(at: cellTypes.firstIndex(of: .recipient)!) : cellTypes.remove(at: cellTypes.firstIndex(of: .wallet)!)

        navBarSetup()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        transactionToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil, queue: .main, using: refreshTransaction)
        blockToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Block.rawValue), object: nil, queue: .main, using: refreshTransaction)

//        tableView.reloadData {}
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
            let alert = shareTransactionSheet()
            self.present(alert, animated: true, completion: nil)
        } else {
            if let url = urlForTx() {
                let tx: [Any] = [url]
                let shareVC = UIActivityViewController(activityItems: tx, applicationActivities: nil)
                shareVC.popoverPresentationController?.sourceView = sender
                self.present(shareVC, animated: true, completion: nil)
            }
        }
    }

    func shareTransactionSheet() -> UIAlertController {
        let alert = UIAlertController(title: NSLocalizedString("Share Transaction", comment: ""), message: "", preferredStyle: .actionSheet)
        // View the transaction in blockstream.info
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_view_in_explorer", comment: ""), style: .default) { _ in
            guard let alert: UIAlertController = self.explorerUrlOrAlert() else { return }
            self.present(alert, animated: true, completion: nil)
        })
        // Share the unblinded transaction explorer url
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_share_nonconfidential", comment: ""), style: .default) { _ in
            let unblindedUrl = (self.account?.gdkNetwork?.txExplorerUrl ?? "") + self.transaction.hash + self.transaction.blindingUrlString()
            let shareVC = UIActivityViewController(activityItems: [unblindedUrl], applicationActivities: nil)
            self.present(shareVC, animated: true, completion: nil)
        })
        // Share data needed to unblind the transaction
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_share_unblinding_data", comment: ""), style: .default) { _ in
            let blindingData = try? JSONSerialization.data(withJSONObject: self.transaction.blindingData() ?? "", options: [])
            let shareVC = UIActivityViewController(activityItems: [String(data: blindingData!, encoding: .utf8)!], applicationActivities: nil)
            self.present(shareVC, animated: true, completion: nil)
        })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        return alert
    }

    func urlForTx() -> URL? {
        return URL(string: (account?.gdkNetwork?.txExplorerUrl ?? "") + self.transaction.hash)
    }

    func explorerUrlOrAlert() -> UIAlertController? {
        guard let url: URL = urlForTx() else { return nil }
        let host = url.host!.starts(with: "www.") ? String(url.host!.prefix(5)) : url.host!
        if viewInExplorerPreference {
            UIApplication.shared.open(url, options: [:])
            return nil
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

        return alert
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
}

extension TransactionViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return TransactionSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case TransactionSection.amount.rawValue:
            return 2
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

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        switch indexPath.section {
        case TransactionSection.amount.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionAmountCell") as? TransactionAmountCell {
                cell.configure()
                cell.selectionStyle = .none
                return cell
            }
        case TransactionSection.fee.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionFeeCell") as? TransactionFeeCell {
                cell.configure()
                cell.selectionStyle = .none
                return cell
            }
        case TransactionSection.status.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionStatusCell") as? TransactionStatusCell {
                cell.configure(num: 1)
                cell.selectionStyle = .none
                return cell
            }
        case TransactionSection.detail.rawValue:
            let noteAction: VoidToVoid? = { [weak self] in
                self?.editNote()
            }
            let explorerAction: VoidToVoid? = { [weak self] in
                guard let alert: UIAlertController = self?.explorerUrlOrAlert() else { return }
                self?.present(alert, animated: true, completion: nil)
            }
            let copyAction: VoidToVoid? = { [weak self] in
                self?.copyToClipboard()
            }
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TransactionDetailCell") as? TransactionDetailCell {
                cell.configure(
                    transaction: self.transaction,
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
            try gaios.SessionManager.shared.setTransactionMemo(txhash_hex: self.transaction.hash, memo: note, memo_type: 0)
            self.transaction.memo = note
            }.ensure {
                self.stopAnimating()
            }.done {
                self.tableView.reloadData {}
            }.catch { _ in}
    }

    func didCancel() {
        print("cancel")
    }
}
