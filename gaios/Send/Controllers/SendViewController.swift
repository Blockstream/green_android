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

    var viewModel: SendViewModel!
    var accounts: [WalletItem]?
    var fixedWallet: Bool = false
    var fixedAsset: Bool = false
    var addressInputType: AnalyticsManager.AddressInputType = .paste

    private let activityIndicator = UIActivityIndicatorView(style: .white)
    private var remoteAlert: RemoteAlert?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        updateBtnNext()

        viewModel.loadRecipient()
        viewModel.updateRecipientFromTx(tx: viewModel.transaction)
        reloadSections([.recipient, .fee], animated: true)

        activityIndicator.hidesWhenStopped = true
        view.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.view
        btnNext.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.nextBtn

        tableView.register(UINib(nibName: "AlertCardCell", bundle: nil), forCellReuseIdentifier: "AlertCardCell")
        remoteAlert = RemoteAlertManager.shared.getAlert(screen: .send, network: AccountsManager.shared.current?.networkName)

        AnalyticsManager.shared.recordView(.send, sgmt: AnalyticsManager.shared.subAccSeg(AccountsManager.shared.current, walletType: viewModel.account.type))
    }

    func setContent() {
        self.title = viewModel.inputType == .sweep ? String(format: NSLocalizedString("id_sweep_into_s", comment: ""), viewModel.account.localizedName()) : NSLocalizedString("id_send_to", comment: "")
    }

    func setStyle() {
        btnNext.setStyle(.primary)
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

    func updateBtnNext() {
        btnNext.setStyle(.primaryDisabled)
        if let tx = viewModel.transaction {
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
        viewModel.createTx().done { tx in
            if let error = tx?.error, !error.isEmpty, !self.viewModel.inlineErrors.contains(error) {
                self.dropError(NSLocalizedString(error, comment: ""))
            }
        }.catch { error in
            switch error {
            case TransactionError.invalid(let localizedDescription):
                self.dropError(localizedDescription)
            case GaError.ReconnectError, GaError.SessionLost, GaError.TimeoutError:
                self.dropError("id_you_are_not_connected".localized)
            default:
                self.dropError(error.localizedDescription)
            }
        }.finally {
            self.hideIndicator()
            let vc = self.tableView.visibleCells
            vc.forEach({ item in
                if let cell = item as? RecipientCell {
                    cell.model = self.viewModel.recipient
                    cell.onTransactionValidate()
                }})
            self.updateBtnNext()
            self.reloadSections([.fee], animated: false)
        }
    }

    func dropError(_ msg: String) {
        DropAlert().error(message: msg)
    }

    override func keyboardWillShow(notification: Notification) {
    }

    override func keyboardWillHide(notification: Notification) {
        tableView.setContentOffset(CGPoint(x: 0.0, y: 0.0), animated: true)
    }

    func onTransactionReady() {
        if viewModel.isBipAddress() {
            addressInputType = .bip21 //analytics only
        }
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendConfirmViewController") as? SendConfirmViewController {
            vc.wallet = viewModel.account
            vc.transaction = viewModel.transaction
            vc.inputType = viewModel.inputType
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
            return viewModel.recipientCellModels.count
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
                cell.configure(cellModel: viewModel.recipientCellModels[indexPath.row],
                               index: indexPath.row,
                               isMultiple: viewModel.recipientCellModels.count > 1)
                cell.updateModel = { if let model = $0 { self.viewModel.recipientCellModels[indexPath.row] = model } }
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
                cell.configure(fee: self.viewModel.transaction?.fee,
                               feeRate: self.viewModel.transaction?.feeRate,
                               txError: self.viewModel.transaction?.error,
                               transactionPriority: self.viewModel.transactionPriority)
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
        viewModel.recipientCellModels.remove(at: index)
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
            viewModel.updateRecipientFromTx(tx: nil)
            viewModel.recipientCellModels[index].address = value
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
        viewModel.customFee = fee ?? 1000
        viewModel.transactionPriority = .Custom
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
        if let vc = storyboard.instantiateViewController(withIdentifier: "AccountAssetViewController") as? AccountAssetViewController {
            let accounts = fixedWallet ? [viewModel.account] :  accounts ?? []
            vc.viewModel = AccountAssetViewModel(accounts: accounts)
            vc.delegate = self
            navigationController?.pushViewController(vc, animated: true)
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
        tableView.scrollToRow(at: IndexPath(row: viewModel.recipientCellModels.count - 1, section: SendSection.allCases.count - 1), at: .bottom, animated: true)
    }
}

extension SendViewController: AddRecipientCellDelegate {

    func action() {
        //addRecipient()
    }
}

extension SendViewController: FeeEditCellDelegate {
    func setCustomFee() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogCustomFeeViewController") as? DialogCustomFeeViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            vc.account = viewModel.account
            vc.storedFeeRate = viewModel.feeEstimates()[3]
            present(vc, animated: false, completion: nil)
        }
    }

    func updatePriority(_ priority: TransactionPriority) {
        viewModel.transactionPriority = priority
        validateTransaction()
    }
}

extension SendViewController: AssetSelectViewControllerDelegate {
    func didSelectAnyAsset() {
        let feeAsset = viewModel.account.gdkNetwork.getFeeAsset()
        selectAsset(assetId: feeAsset, index: nil)
    }

    func didSelectAsset(_ assetId: String) {
        selectAsset(assetId: assetId, index: nil)
    }

    func selectAsset(assetId: String, index: Int?) {
        let index = index ?? 0
        viewModel.updateRecipientFromTx(tx: nil)
        viewModel.updateRecipient(assetId: assetId)
        reloadSections([SendSection.recipient], animated: false)
        validateTransaction()
    }
}
extension SendViewController: AssetExpandableSelectViewControllerDelegate {
    func didSelectReceiver(assetId: String, account: WalletItem) {
        viewModel.account = account
        selectAsset(assetId: assetId, index: nil)
    }
}
extension SendViewController: AccountAssetViewControllerDelegate {
    func didSelectAccountAsset(account: WalletItem, asset: AssetInfo) {
        viewModel.account = account
        selectAsset(assetId: asset.assetId, index: nil)
    }
}
