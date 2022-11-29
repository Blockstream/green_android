import Foundation
import UIKit
import PromiseKit

class SendViewController: KeyboardViewController {

    enum SendSection: Int, CaseIterable {
        case remoteAlerts = 0
        case recipient = 1
        case addRecipient = 2
        case fee = 3
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnNext: UIButton!

    var wallet: WalletItem!
    var fixedWallet: Bool = false
    var fixedAsset: Bool = false
    var transaction: Transaction?
    var recipients: [Recipient] = []
    var inputType: InputType = .transaction
    var addressInputType: AnalyticsManager.AddressInputType = .paste

    private var validateTask: ValidateTask?
    private var transactionPriority: TransactionPriority = .High
    private var customFee: UInt64 = 1000
    private let activityIndicator = UIActivityIndicatorView(style: .white)

    private var session: SessionManager { wallet.session! }
    private var isLiquid: Bool { session.gdkNetwork.liquid }
    private var isBtc: Bool { !session.gdkNetwork.liquid }
    private var btc: String { session.gdkNetwork.getFeeAsset() }

    func feeEstimates() -> [UInt64?] {
        var feeEstimates = [UInt64?](repeating: 0, count: 4)
        guard let estimates = session.getFeeEstimates() else {
            // We use the default minimum fee rates when estimates are not available
            let defaultMinFee = isLiquid ? 100 : 1000
            return [UInt64(defaultMinFee), UInt64(defaultMinFee), UInt64(defaultMinFee), UInt64(defaultMinFee)]
        }
        for (index, value) in [3, 12, 24, 0].enumerated() {
            feeEstimates[index] = estimates[value]
        }
        feeEstimates[3] = nil
        return feeEstimates
    }

    func defaultTransactionPriority() -> TransactionPriority {
        guard let settings = WalletManager.current?.prominentSession?.settings else {
            return .High
        }
        if let pref = TransactionPriority.getPreference() {
            settings.transactionPriority = pref
        }
        return settings.transactionPriority
    }

    private var remoteAlert: RemoteAlert?

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

    var inlineErrors = ["id_invalid_address",
                      "id_invalid_private_key",
                      "id_invalid_amount",
                      "id_insufficient_funds",
                      "id_invalid_payment_request_assetid",
                      "id_invalid_asset_id"
    ]

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        addRecipient()
        updateBtnNext()
        transactionPriority = inputType == .bumpFee ? .High : defaultTransactionPriority()
        activityIndicator.hidesWhenStopped = true

        view.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.view
        btnNext.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.nextBtn

        tableView.register(UINib(nibName: "AlertCardCell", bundle: nil), forCellReuseIdentifier: "AlertCardCell")

        self.remoteAlert = RemoteAlertManager.shared.getAlert(screen: .send, network: AccountsManager.shared.current?.networkName)

        AnalyticsManager.shared.recordView(.send, sgmt: AnalyticsManager.shared.subAccSeg(AccountsManager.shared.current, walletType: wallet?.type))
        AnalyticsManager.shared.startSendTransaction()
    }

    func setContent() {
        if let wallet = wallet {
            self.title = inputType == .sweep ? String(format: NSLocalizedString("id_sweep_into_s", comment: ""), wallet.localizedName()) : NSLocalizedString("id_send_to", comment: "")
        }
    }

    func setStyle() {
        btnNext.setStyle(.primary)
    }

    func addRecipient() {
        let recipient = Recipient()
        if inputType == .bumpFee {
            let addressee = transaction?.addressees.first
            recipient.address = addressee?.address

            if let satoshi = addressee?.satoshi {
                let (amount, _) = satoshi == 0 ? ("", "") : Balance.fromSatoshi(satoshi)?.toDenom() ?? ("", "")
                recipient.amount = amount
            }
            recipient.txError = transaction?.error ?? ""
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

    func isSendAll() -> Bool {
        // handling only 1 recipient for the moment
        let recipient = recipients.first
        return recipient?.isSendAll == true
    }

    func getAddressee() -> [String: Any] {
        // handling only 1 recipient for the moment
        let recipient = recipients.first
        let addressInput: String = recipient?.address ?? ""

        let network = session.gdkNetwork
        let policyAsset = recipient?.assetId

        let satoshi = recipient?.getSatoshi() ?? 0

        var addressee: [String: Any] = [:]
        addressee["address"] = addressInput
        addressee["satoshi"] = satoshi

        if network.liquid && !isBipAddress() && policyAsset != nil {
            addressee["asset_id"] = policyAsset!
        }

        return addressee
    }

    func getPrivateKey() -> String {
        // handling only 1 recipient for the moment
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

    func isBipAddress() -> Bool {
        let addressInput: String = recipients.first?.address ?? ""
        return wallet.session?.validBip21Uri(uri: addressInput) ?? false
    }

    func validateTransaction() {

        var details: [String: Any] = [:]
        if let tx = transaction { details = tx.details }
        transaction = nil

        let subaccount = self.wallet!.pointer

        var estimates = feeEstimates()
        estimates[3] = customFee
        guard let feeEstimate = estimates[selectedFee()] else { return }
        let feeRate = feeEstimate

        switch inputType {
        case .transaction:
            if isBipAddress() { details = [:] }
            details["addressees"] = [self.getAddressee()]
            details["fee_rate"] = feeRate
            details["subaccount"] = subaccount
            details["send_all"] = self.isSendAll()
        case .sweep:
            details["private_key"] = self.getPrivateKey()
            details["fee_rate"] = feeRate
            details["subaccount"] = subaccount
        case .bumpFee:
            details["fee_rate"] = feeRate
        }

        showIndicator()
        updateBtnNext()
        validateTask?.cancel()
        validateTask = ValidateTask(details: details, inputType: inputType, session: session)
        validateTask?.execute().get { tx in
            self.transaction = tx
        }.done { tx in
            self.transaction = tx
            if let error = tx?.error, !error.isEmpty, !self.inlineErrors.contains(error) {
                self.dropError(NSLocalizedString(error, comment: ""))
            }
        }.catch { error in
            switch error {
            case TransactionError.invalid(let localizedDescription):
                self.dropError(localizedDescription)
            case GaError.ReconnectError, GaError.SessionLost, GaError.TimeoutError:
                self.dropError(NSLocalizedString("id_you_are_not_connected", comment: ""))
            default:
                self.dropError(error.localizedDescription)
            }
        }.finally {
            self.hideIndicator()
            self.updateBtnNext()
            self.bindRecipients(tx: self.transaction)
            let vc = self.tableView.visibleCells
            vc.forEach({ item in
                if let cell = item as? RecipientCell {
                    cell.onTransactionValidate()
                }})
            self.reloadSections([SendSection.fee], animated: false)
        }
    }

    func dropError(_ msg: String) {
        DropAlert().error(message: msg)
    }

    func bindRecipients(tx: Transaction?) {
        let addreessee = tx?.addressees.first
        var value = addreessee?.satoshi ?? 0
        let asset =  isLiquid ? addreessee?.assetId ?? "" : "btc"
        recipients.first?.txError = tx?.error ?? ""
        recipients.first?.assetId = asset

        if !(session.gdkNetwork.electrum) && tx?.sendAll ?? false {
            value = tx?.amounts.filter({$0.key == asset}).first?.value ?? 0
        }
        let assetInfo = WalletManager.current?.registry.info(for: asset)
        if let balance = Balance.fromSatoshi(value, asset: assetInfo) {
            let (amount, _) = value == 0 ? ("", "") : balance.toValue()
            recipients.first?.amount = amount
        }
    }

    override func keyboardWillShow(notification: Notification) {
    }

    override func keyboardWillHide(notification: Notification) {
        tableView.setContentOffset(CGPoint(x: 0.0, y: 0.0), animated: true)
    }

    func onTransactionReady() {
        if isBipAddress() {
            addressInputType = .bip21 //analytics only
        }
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendConfirmViewController") as? SendConfirmViewController, let tx = self.transaction {
            vc.wallet = wallet
            vc.transaction = tx
            vc.inputType = inputType
            vc.addressInputType = addressInputType
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func remoteAlertDismiss() {
        remoteAlert = nil
        reloadSections([SendSection.remoteAlerts], animated: true)
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
        case SendSection.remoteAlerts.rawValue:
            return self.remoteAlert != nil ? 1 : 0
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

        switch indexPath.section {
        case SendSection.remoteAlerts.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AlertCardCell", for: indexPath) as? AlertCardCell, let remoteAlert = self.remoteAlert {
                cell.configure(AlertCardCellModel(type: .remoteAlert(remoteAlert)),
                                   onLeft: nil,
                                   onRight: (remoteAlert.link ?? "" ).isEmpty ? nil : { () in
                    SafeNavigationManager.shared.navigate(remoteAlert.link)
                },
                                   onDismiss: {[weak self] in
                                 self?.remoteAlertDismiss()
                    })
                cell.selectionStyle = .none
                return cell
            }
        case SendSection.recipient.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "RecipientCell") as? RecipientCell {
                cell.configure(recipient: recipients[indexPath.row],
                               index: indexPath.row,
                               isMultiple: recipients.count > 1,
                               walletItem: self.wallet,
                               inputType: self.inputType)
                cell.delegate = self
                cell.selectionStyle = .none
                return cell
            }
        case SendSection.addRecipient.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AddRecipientCell") as? AddRecipientCell {
                cell.delegate = self
                cell.selectionStyle = .none
                return cell
            }
        case SendSection.fee.rawValue:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "FeeEditCell") as? FeeEditCell {
                cell.configure(fee: self.transaction?.fee,
                               feeRate: self.transaction?.feeRate,
                               txError: self.transaction?.error,
                               transactionPriority: self.transactionPriority)
                cell.delegate = self
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
/*
extension SendViewController: AssetsListViewControllerDelegate {
    func didSelect(assetId: String, index: Int?) {
        if let index = index {
            transaction = nil
            recipients[index].assetId = assetId
            recipients[index].amount = nil
            recipients[index].isFiat = false
            recipients[index].isSendAll = false
            reloadSections([SendSection.recipient], animated: false)
            validateTransaction()
        }
    }
}
*/
extension SendViewController: DialogQRCodeScanViewControllerDelegate {
    func didScan(value: String, index: Int?) {
        if let index = index {
            transaction = nil
            recipients[index].address = value
            reloadSections([SendSection.recipient], animated: false)
            validateTransaction()
            addressInputType = .scan
        }
    }
    func didStop() {
        //
    }
}

extension SendViewController: DialogCustomFeeViewControllerDelegate {
    func didSave(fee: UInt64?) {
        customFee = fee ?? 1000
        transactionPriority = .Custom
        validateTransaction()
    }
}

extension SendViewController: RecipientCellDelegate {

    func validateTx() {
        validateTransaction()
    }

    func removeRecipient(_ index: Int) {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogRecipientDeleteViewController") as? DialogRecipientDeleteViewController {
            vc.index = index
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    func needRefresh() {
        tableView.beginUpdates()
        tableView.endUpdates()
    }

    func chooseAsset(_ index: Int) {
        let storyboard = UIStoryboard(name: "Utility", bundle: nil)
        if fixedAsset {
            return
        }
        let registry = WalletManager.current?.registry
        if fixedWallet {
            // from AccountViewController, show only assets selection
            if let vc = storyboard.instantiateViewController(withIdentifier: "AssetSelectViewController") as? AssetSelectViewController {
                let assets = wallet.satoshi?.filter { $0.value > 0 }.keys.compactMap { registry?.info(for: $0) }
                vc.viewModel = AssetSelectViewModel(assets: assets ?? [], enableAnyAsset: false)
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        } else {
            // show assets and account selection
            if let vc = storyboard.instantiateViewController(withIdentifier: "AssetExpandableSelectViewController") as? AssetExpandableSelectViewController {
                var balance = [String: Int64]()
                WalletManager.current?.subaccounts.forEach { subaccount in
                    let satoshi = subaccount.satoshi ?? [:]
                    satoshi.forEach {
                        if let amount = balance[$0.0] {
                            balance[$0.0] = amount + $0.1
                        } else {
                            balance[$0.0] = $0.1
                        }
                    }
                }
                let assets = balance.keys.compactMap { registry?.info(for: $0) }
                vc.viewModel = AssetExpandableSelectViewModel(assets: assets, enableAnyAsset: false, onlyFunded: true)
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        }
    }

    func qrScan(_ index: Int) {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogQRCodeScanViewController") as? DialogQRCodeScanViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.index = index
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    func tapSendAll() {
        reloadSections([SendSection.recipient], animated: true)
    }

    func onFocus() {
        tableView.scrollToRow(at: IndexPath(row: recipients.count - 1, section: SendSection.allCases.count - 1), at: .bottom, animated: true)
    }
}

extension SendViewController: AddRecipientCellDelegate {

    func action() {
        addRecipient()
    }
}

extension SendViewController: FeeEditCellDelegate {
    func setCustomFee() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogCustomFeeViewController") as? DialogCustomFeeViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            vc.account = wallet
            vc.storedFeeRate = feeEstimates()[3]
            present(vc, animated: false, completion: nil)
        }
    }

    func updatePriority(_ priority: TransactionPriority) {
        transactionPriority = priority
        validateTransaction()
    }
}

extension SendViewController: AssetSelectViewControllerDelegate {
    func didSelectAnyAsset() {
        let feeAsset = wallet.gdkNetwork.getFeeAsset()
        selectAsset(assetId: feeAsset, index: nil)
    }

    func didSelectAsset(_ assetId: String) {
        selectAsset(assetId: assetId, index: nil)
    }

    func selectAsset(assetId: String, index: Int?) {
        let index = index ?? 0
        transaction = nil
        recipients[index].assetId = assetId
        recipients[index].amount = nil
        recipients[index].isFiat = false
        recipients[index].isSendAll = false
        reloadSections([SendSection.recipient], animated: false)
        validateTransaction()
    }
}
extension SendViewController: AssetExpandableSelectViewControllerDelegate {
    func didSelectReceiver(assetId: String, account: WalletItem) {
        wallet = account
        selectAsset(assetId: assetId, index: nil)
    }
}
