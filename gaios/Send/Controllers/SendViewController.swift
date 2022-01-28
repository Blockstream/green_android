import Foundation
import UIKit
import PromiseKit

class SendViewController: KeyboardViewController {

    enum SendSection: Int, CaseIterable {
        case recipient = 0
        case addRecipient = 1
        case fee = 2
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnNext: UIButton!

    var wallet: WalletItem?
    var recipients: [Recipient] = []
    var isSendAll: Bool = false
    var isSweep: Bool = false
    var isBumpFee: Bool = false
    var inputType: InputType = .transaction

    var transaction: Transaction?
    private var validateTask: ValidateTask?
    var prevTxDetails: [String: Any] = [:]

    var transactionPriority: TransactionPriority = .High
    var customFee: UInt64 = 1000
    let activityIndicator = UIActivityIndicatorView(style: .white)

    var isLiquid: Bool {
        get {
            return AccountsManager.shared.current?.gdkNetwork?.liquid ?? false
        }
    }

    var isBtc: Bool {
        get {
            let ntw = AccountsManager.shared.current?.network
            return ntw == "mainnet" || ntw == "testnet"
        }
    }

    private var feeEstimates: [UInt64?] = {
        var feeEstimates = [UInt64?](repeating: 0, count: 4)
        guard let estimates = getFeeEstimates() else {
            // We use the default minimum fee rates when estimates are not available
            let defaultMinFee = AccountsManager.shared.current?.gdkNetwork?.liquid ?? false ? 100 : 1000
            return [UInt64(defaultMinFee), UInt64(defaultMinFee), UInt64(defaultMinFee), UInt64(defaultMinFee)]
        }
        for (index, value) in [3, 12, 24, 0].enumerated() {
            feeEstimates[index] = estimates[value]
        }
        feeEstimates[3] = nil
        return feeEstimates
    }()

    private var defaultTransactionPriority: TransactionPriority = {
        guard let settings = SessionManager.shared.settings else { return .High }
        if let pref = TransactionPriority.getPreference() {
            settings.transactionPriority = pref
        }
        return settings.transactionPriority
    }()

    func selectedFee() -> Int {
        switch transactionPriority {
        case .High:
            return 0
        case .Medium:
            return 1
        case .Low:
            return 2
        case .Custom:
            return 3
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        addRecipient()
        updateBtnNext()
        transactionPriority = defaultTransactionPriority
        activityIndicator.hidesWhenStopped = true
        if isBumpFee {
            prevTxDetails = transaction?.details ?? [:]
        }
        if isSweep { inputType = .sweep }
        if isBumpFee { inputType = .bumpFee }
    }

    func setContent() {
        if let wallet = wallet {
            self.title = isSweep ? String(format: NSLocalizedString("id_sweep_into_s", comment: ""), wallet.localizedName()) : NSLocalizedString("id_send_to", comment: "")
        }
    }

    func setStyle() {
        btnNext.setStyle(.primary)
    }

    func addRecipient() {
        let recipient = Recipient()
        if isBumpFee {
            let addressee = transaction?.addressees.first
            recipient.address = addressee?.address

            if let satoshi = addressee?.satoshi {
                let details = ["satoshi": satoshi]
                let (amount, _) = satoshi == 0 ? ("", "") : Balance.convert(details: details)?.get(tag: "btc") ?? ("", "")
                recipient.amount = amount
            }
        }

        recipient.assetId = isBtc ? "btc" : nil
        recipients.append(recipient)
        reloadSections([SendSection.recipient], animated: true)
    }

    func reloadSections(_ sections: [SendSection], animated: Bool) {
        DispatchQueue.main.async {
            if animated {
                self.tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
            } else {
                UIView.performWithoutAnimation {
                    self.tableView.reloadSections(IndexSet(sections.map { $0.rawValue }), with: .none)
                }
            }
        }
    }

    func getAddressee() -> [String: Any] {
        //handling only 1 recipient for the moment
        let recipient = recipients.first
        let addressInput: String = recipient?.address ?? ""

        let isBip21 = addressInput.starts(with: "bitcoin:") || addressInput.starts(with: "liquidnetwork:")
        let network = AccountsManager.shared.current?.gdkNetwork
        let policyAsset = recipients[0].assetId

        let satoshi = recipient?.getSatoshi() ?? 0

        var addressee: [String: Any] = [:]
        addressee["address"] = addressInput
        addressee["satoshi"] = satoshi

        if network?.liquid ?? false && !isBip21 {
            addressee["asset_id"] = policyAsset
        }

        return addressee
    }

    func getPrivateKey() -> String {
        //handling only 1 recipient for the moment
        let recipient = recipients.first
        return recipient?.address ?? ""
    }

    func updateBtnNext() {
        btnNext.setStyle(.primaryDisabled)
        if let tx = transaction {
            if tx.error.isEmpty {
                btnNext.setStyle(.primary)
            }
        }
    }

    func showIndicator() {
        self.activityIndicator.isHidden = false
        self.activityIndicator.startAnimating()
        self.navigationItem.titleView = self.activityIndicator
    }

    func hideIndicator() {
        self.activityIndicator.stopAnimating()
        self.navigationItem.titleView = nil
    }

    func validateTransaction() {
        transaction = nil
        updateBtnNext()

        if (recipients[0].amount ?? "").isEmpty && (recipients[0].address ?? "").isEmpty {
            return
        }
//        if recipients[0].assetId == nil {
//            return
//        }
        let subaccount = self.wallet!.pointer

        feeEstimates[3] = customFee
        guard let feeEstimate = feeEstimates[selectedFee()] else { return }
        let feeRate = feeEstimate

        var details: [String: Any] = [:]
        switch inputType {
        case .transaction:
            details["addressees"] = [self.getAddressee()]
            details["fee_rate"] = feeRate
            details["subaccount"] = subaccount
            if self.isSendAll == true {
                details["send_all"] = true
            }
        case .sweep:
            details["private_key"] = self.getPrivateKey()
            details["fee_rate"] = feeRate
            details["subaccount"] = subaccount
        case .bumpFee:
            details = self.prevTxDetails
            details["fee_rate"] = feeRate
        }

        showIndicator()
        validateTask?.cancel()
        validateTask = ValidateTask(details: details, inputType: inputType)
        validateTask?.execute().get { tx in
            self.transaction = tx
        }.done { tx in
            self.transaction = tx
            if tx!.error == "id_invalid_replacement_fee_rate" {
                DropAlert().error(message: NSLocalizedString("id_invalid_replacement_fee_rate", comment: ""))
            }
        }.catch { error in
            switch error {
            case TransactionError.invalid(let localizedDescription):
                DropAlert().error(message: localizedDescription)
            case GaError.ReconnectError, GaError.SessionLost, GaError.TimeoutError:
                DropAlert().error(message: NSLocalizedString("id_you_are_not_connected", comment: ""))
            default:
                DropAlert().error(message: error.localizedDescription)
            }
        }.finally {
            self.hideIndicator()
            self.updateBtnNext()
            let vc = self.tableView.visibleCells
            vc.forEach({ item in
                if let cell = item as? RecipientCell {
                    cell.onTransactionValidate(self.transaction)
                }})
            self.reloadSections([SendSection.fee], animated: false)
        }
    }

    override func keyboardWillShow(notification: Notification) {
    }

    override func keyboardWillHide(notification: Notification) {
        tableView.setContentOffset(CGPoint(x: 0.0, y: 0.0), animated: true)
    }

    func onTransactionReady() {
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendConfirmViewController") as? SendConfirmViewController, let tx = self.transaction {
            vc.wallet = wallet
            vc.transaction = tx
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        onTransactionReady()
    }
}

extension SendViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return SendSection.allCases.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        switch section {
        case SendSection.recipient.rawValue:
            return recipients.count
        case SendSection.addRecipient.rawValue:
            return 0 // 1
        case SendSection.fee.rawValue:
            return 1
        default:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        let removeRecipientAction: VoidToVoid = {[weak self] in
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogRecipientDeleteViewController") as? DialogRecipientDeleteViewController {
                vc.index = indexPath.row
                vc.modalPresentationStyle = .overFullScreen
                vc.delegate = self
                self?.present(vc, animated: false, completion: nil)
            }
        }
        let needRefresh: VoidToVoid = {[weak self] in
            self?.tableView.beginUpdates()
            self?.tableView.endUpdates()
        }
        let validateTransaction: VoidToVoid = {[weak self] in
            self?.validateTransaction()
        }
        let selectAsset: VoidToVoid = {[weak self] in
            let storyboard = UIStoryboard(name: "Assets", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "AssetsListViewController") as? AssetsListViewController {
                vc.wallet = self?.wallet
                vc.index = indexPath.row
                vc.delegate = self
                self?.present(vc, animated: true, completion: nil)
            }
        }
        let qrAction: VoidToVoid = {[weak self] in
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogQRCodeScanViewController") as? DialogQRCodeScanViewController {
                vc.modalPresentationStyle = .overFullScreen
                vc.index = indexPath.row
                vc.delegate = self
                self?.present(vc, animated: false, completion: nil)
            }
        }
        let tapSendAll: VoidToVoid = {[weak self] in
            self?.isSendAll.toggle()
            self?.reloadSections([SendSection.recipient], animated: true)
        }
        let onFocus: VoidToVoid = {[weak self] in
            self?.tableView.scrollToRow(at: IndexPath(row: (self?.recipients.count ?? 1) - 1, section: SendSection.allCases.count - 1), at: .bottom, animated: true)
        }
        switch indexPath.section {
        case SendSection.recipient.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "RecipientCell") as? RecipientCell {
                cell.configure(recipient: recipients[indexPath.row],
                               index: indexPath.row,
                               isMultiple: recipients.count > 1,
                               removeRecipient: removeRecipientAction,
                               needRefresh: needRefresh,
                               chooseAsset: selectAsset,
                               qrScan: qrAction,
                               walletItem: self.wallet,
                               tapSendAll: tapSendAll,
                               isSendAll: self.isSendAll,
                               isSweep: self.isSweep,
                               isBumpFee: self.isBumpFee,
                               validateTransaction: validateTransaction,
                               onFocus: onFocus)
                cell.selectionStyle = .none
                return cell
            }
        case SendSection.addRecipient.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AddRecipientCell") as? AddRecipientCell {
                cell.configure {
                    [weak self] in
                    self?.addRecipient()
                }
                cell.selectionStyle = .none
                return cell
            }
        case SendSection.fee.rawValue:
            let setCustomFee: VoidToVoid = {[weak self] in
                let storyboard = UIStoryboard(name: "Shared", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "DialogCustomFeeViewController") as? DialogCustomFeeViewController {
                    vc.modalPresentationStyle = .overFullScreen
                    vc.delegate = self
                    vc.storedFeeRate = self?.feeEstimates[3]
                    self?.present(vc, animated: false, completion: nil)
                }
            }
            let updatePriority: ((TransactionPriority) -> Void) = {[weak self] value in
                self?.transactionPriority = value
                self?.validateTransaction()
            }
            if let cell = tableView.dequeueReusableCell(withIdentifier: "FeeEditCell") as? FeeEditCell {
                cell.configure(transaction: self.transaction,
                               setCustomFee: setCustomFee,
                               updatePriority: updatePriority,
                               transactionPriority: self.transactionPriority)
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 1.0
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 1.0
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

    }
}

extension SendViewController: DialogRecipientDeleteViewControllerDelegate {
    func didCancel() {
        //
    }
    func didDelete(_ index: Int) {
        recipients.remove(at: index)
        reloadSections([SendSection.recipient], animated: true)
    }
}

extension SendViewController: AssetsListViewControllerDelegate {
    func didSelect(assetId: String, index: Int?) {
        if let index = index {
            isSendAll = false
            recipients[index].assetId = assetId
            recipients[index].amount = nil
            recipients[index].isFiat = false
            reloadSections([SendSection.recipient], animated: false)
            validateTransaction()
        }
    }
}

extension SendViewController: DialogQRCodeScanViewControllerDelegate {
    func didScan(value: String, index: Int?) {
        if let index = index {
            recipients[index].address = value
            reloadSections([SendSection.recipient], animated: false)
            validateTransaction()
        }
    }
    func didStop() {
        //
    }
}

extension SendViewController: DialogCustomFeeViewControllerDelegate {
    func didSave(fee: UInt64?) {
        feeEstimates[3] = fee ?? 1000
        customFee = feeEstimates[3] ?? 1000
        transactionPriority = .Custom
        validateTransaction()
    }
}
