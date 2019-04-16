import Foundation
import UIKit
import PromiseKit

class TransactionDetailViewController: KeyboardViewController {

    var transaction: Transaction!
    var wallet: WalletItem!

    @IBOutlet weak var viewInExplorerButton: UIButton!

    var viewInExplorerPreference: Bool {
        get {
            return UserDefaults.standard.bool(forKey: getNetwork() + "_view_in_explorer")
        }
        set {
            UserDefaults.standard.set(newValue, forKey: getNetwork() + "_view_in_explorer")
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        if transaction.type == "redeposit" {
            title = NSLocalizedString("id_redeposited", comment: "")
        } else if transaction.type == "incoming" {
            title = NSLocalizedString("id_received_on", comment: "")
        } else {
            title = NSLocalizedString("id_sent_to", comment: "")
        }
        viewInExplorerButton.setTitle(NSLocalizedString("id_view_in_explorer", comment: ""), for: .normal)
        viewInExplorerButton.setGradient(true)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        viewInExplorerButton.updateGradientLayerFrame()
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let tableViewController = segue.destination as? TransactionTableViewController {
            tableViewController.transaction = transaction
            tableViewController.wallet = wallet
        } else if let nextController = segue.destination as? SendBtcDetailsViewController {
            nextController.transaction = sender as? Transaction
            nextController.wallet = wallet
        }
    }

    @IBAction func viewInExplorerClicked(_ sender: Any) {
        let currentNetwork: String = getNetwork().lowercased()
        let configNetwork = try? getGdkNetwork(currentNetwork)
        guard let config = configNetwork as? [String: Any] else { return }
        guard let baseUrl = config["tx_explorer_url"] as? String else { return }
        guard let url: URL = URL(string: baseUrl + self.transaction.hash) else { return }
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
        self.present(alert, animated: true, completion: nil)
    }
}

class TransactionTableViewController: UITableViewController, UITextViewDelegate {

    var transaction: Transaction!
    var wallet: WalletItem!

    @IBOutlet weak var hashTitle: UILabel!
    @IBOutlet weak var recipientTitle: UILabel!
    @IBOutlet weak var feeTitle: UILabel!
    @IBOutlet weak var amountTitle: UILabel!
    @IBOutlet weak var memoTitle: UILabel!
    @IBOutlet weak var walletTitle: UILabel!

    @IBOutlet weak var hashLabel: UILabel!
    @IBOutlet weak var dateLabel: UILabel!
    @IBOutlet weak var amountLabel: UILabel!
    @IBOutlet weak var feeLabel: UILabel!
    @IBOutlet weak var walletLabel: UILabel!
    @IBOutlet weak var recipientLabel: UILabel!
    @IBOutlet weak var increasefeeLabel: UILabel!
    @IBOutlet weak var statusLabel: UILabel!
    @IBOutlet weak var memoTextView: UITextView!

    @IBOutlet weak var statusImage: UIImageView!
    @IBOutlet weak var statusCell: UITableViewCell!
    @IBOutlet weak var recipientCell: UITableViewCell!
    @IBOutlet weak var walletCell: UITableViewCell!
    @IBOutlet weak var walletGradientView: UIView!
    @IBOutlet weak var saveButton: UIButton!
    var gradientLayer = CAGradientLayer()

    override func viewDidLoad() {
        super.viewDidLoad()
        hashTitle.text = NSLocalizedString("id_txid", comment: "").uppercased()
        feeTitle.text = NSLocalizedString("id_fee", comment: "").uppercased()
        amountTitle.text = NSLocalizedString("id_amount", comment: "").uppercased()
        memoTitle.text = NSLocalizedString("id_my_notes", comment: "").uppercased()
        recipientTitle.text = NSLocalizedString("id_recipient", comment: "").uppercased()
        walletTitle.text = NSLocalizedString("id_received_on", comment: "").uppercased()
        increasefeeLabel.text = NSLocalizedString("id_increase_fee", comment: "")
        saveButton.setTitle(NSLocalizedString("id_save", comment: "").uppercased(), for: .normal)
        saveButton.isHidden = true
        gradientLayer = walletGradientView.makeGradientCard()
        walletGradientView.layer.insertSublayer(gradientLayer, at: 0)
        self.tableView.separatorStyle = .none
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        NotificationCenter.default.addObserver(self, selector: #selector(self.refreshTransaction(_:)), name: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil)
         NotificationCenter.default.addObserver(self, selector: #selector(self.refreshTransaction(_:)), name: NSNotification.Name(rawValue: EventType.Block.rawValue), object: nil)

        let tap = UITapGestureRecognizer(target: self, action: #selector(increaseFeeClicked))
        statusCell.addGestureRecognizer(tap)
        updateUI()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.Transaction.rawValue), object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.Block.rawValue), object: nil)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        gradientLayer.frame = walletGradientView.bounds
        gradientLayer.setNeedsDisplay()
    }

    func updateUI() {
        hashLabel.text = transaction.hash
        amountLabel.text = String(format: "%@ / %@", String.toBtc(satoshi: transaction.satoshi), String.toFiat(satoshi: transaction.satoshi))
        feeLabel.text = String(format: "%d satoshi, %.2f sat/vbyte", transaction.fee, Double(transaction.feeRate) / 1000)
        memoTextView.text = transaction.memo
        memoTextView.delegate = self
        dateLabel.text = transaction.date()
        statusImage.isHidden = true
        statusLabel.textColor = UIColor.customTitaniumLight()

        if transaction.type == "redeposit" {
            walletCell.isHidden = true
            recipientCell.isHidden = true
        } else if transaction.type == "incoming" {
            walletLabel.text = wallet.localizedName() // + " " + (transaction.address() ?? "")
            recipientCell.isHidden = true
        } else {
            recipientLabel.text = transaction.address()
            walletCell.isHidden = true
        }
        if transaction.blockHeight == 0 {
            statusLabel.textColor = UIColor.errorRed()
            statusLabel.text = NSLocalizedString("id_unconfirmed", comment: "")
        } else if getGAService().getBlockheight() - (transaction.blockHeight) < 5 {
            let blocks = getGAService().getBlockheight() - transaction.blockHeight + 1
            statusLabel.text = String(format: NSLocalizedString("id_d6_confirmations", comment: ""), blocks)
        } else {
            statusLabel.text = NSLocalizedString("id_completed", comment: "")
            statusImage.image = UIImage(named: "check")
            statusImage.isHidden = false
        }
        if transaction.canRBF && !getGAService().isWatchOnly && !getGAService().getTwoFactorReset()!.isResetActive {
            statusImage.image = UIImage(named: "bump_fee")
            statusImage.isHidden = false
            increasefeeLabel.isHidden = false
        } else {
            increasefeeLabel.isHidden = true
        }
        memoTextView.isEditable = !getGAService().isWatchOnly && !getGAService().getTwoFactorReset()!.isResetActive
        self.tableView.reloadData()
    }

    @objc func refreshTransaction(_ notification: NSNotification) {
        Guarantee().done {
            self.updateUI()
        }
    }

    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        // Hide cells
        if walletCell.isHidden && indexPath.row == 5 {
            return 0
        } else if recipientCell.isHidden && indexPath.row == 4 {
            return 0
        }
        return super.tableView(tableView, heightForRowAt: indexPath)
    }

    @objc func increaseFeeClicked(sender: UITapGestureRecognizer?) {
        if !transaction.canRBF || getGAService().isWatchOnly || getGAService().getTwoFactorReset()!.isResetActive { return }
        let details: [String: Any] = ["previous_transaction": transaction.details, "fee_rate": transaction.feeRate, "subaccount": wallet.pointer]
        gaios.createTransaction(details: details).done { tx in
            self.parent?.performSegue(withIdentifier: "rbf", sender: tx)
        }.catch { _ in
        }
    }

    @IBAction func saveClick(_ sender: Any) {
        guard let memo = memoTextView.text else { return }
        saveButton.isEnabled = false
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().map(on: bgq) {
            try gaios.getSession().setTransactionMemo(txhash_hex: self.transaction.hash, memo: memo, memo_type: 0)
        }.ensure {
            self.saveButton.isEnabled = true
        }.done {
            self.saveButton.isHidden = true
            self.view.endEditing(true)
        }.catch { _ in}
    }

    func textViewDidChangeSelection(_ textView: UITextView) {
        saveButton.isHidden = false
    }
}
