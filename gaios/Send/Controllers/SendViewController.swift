import Foundation
import UIKit
import PromiseKit
import gdk
import greenaddress

class SendViewController: KeyboardViewController {

    enum SendSection: Int, CaseIterable {
        case alert
        case accountAsset
        case address
        case amount
        case fee
    }

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnNext: UIButton!
    private var headerH: CGFloat = 36.0

    var viewModel: SendViewModel!
    var fixedWallet: Bool = false
    var fixedAsset: Bool = false
    var addressInputType: AnalyticsManager.AddressInputType = .paste

    override func viewDidLoad() {
        super.viewDidLoad()
        register()
        setContent()
        setStyle()
        updateBtnNext()
        reloadSections([.accountAsset, .address, .amount, .fee], animated: false)
        view.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.view
        btnNext.accessibilityIdentifier = AccessibilityIdentifiers.SendScreen.nextBtn
        AnalyticsManager.shared.recordView(.send, sgmt: AnalyticsManager.shared.subAccSeg(AccountsRepository.shared.current, walletType: viewModel.account.type))

        if viewModel.transaction != nil {
            viewModel.reload()
            refreshAmountCell()
            reloadSections([.accountAsset, .address, .fee], animated: false)
            validateTransaction()
        }
    }

    func register() {
        ["ReceiveAssetCell", "AlertCardCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
    }
    func setContent() {
        self.title = viewModel.inputType == .sweep ? String(format: NSLocalizedString("id_sweep_into_s", comment: ""), viewModel.account.localizedName) : NSLocalizedString("id_send_to", comment: "")
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

    func validateTransaction() {
        Guarantee()
        .then { self.viewModel.validateTransaction() }
        .done { tx in
            if let error = tx?.error, !error.isEmpty, !self.viewModel.inlineErrors.contains(error) {
                DropAlert().error(message: error.localized)
            }
        }.catch { error in
            switch error {
            case TransactionError.invalid(let localizedDescription):
                DropAlert().error(message: localizedDescription)
            case GaError.ReconnectError, GaError.SessionLost, GaError.TimeoutError:
                DropAlert().error(message: "id_you_are_not_connected".localized)
            default:
                DropAlert().error(message: error.localizedDescription)
            }
        }.finally {
            self.refreshAmountCell()
            self.updateBtnNext()
            self.reloadSections([.accountAsset, .address, .fee], animated: false)
        }
    }

    func cleanAmountCell() {
        let vc = self.tableView.visibleCells
        vc.forEach({ item in
            if let cell = item as? AmountEditCell {
                cell.amountTextField.text = nil
                cell.errorLabel.text = nil
            }})
    }

    func refreshAmountCell() {
        let vc = self.tableView.visibleCells
        vc.forEach({ item in
            if let cell = item as? AmountEditCell {
                if let text = cell.amountTextField.text,
                    text.isEmpty {
                    cell.amountTextField.isEnabled = self.viewModel.editable
                    cell.amountTextField.isUserInteractionEnabled = self.viewModel.editable
                    cell.amountTextField.text = self.viewModel.amount
                }
                cell.errorLabel.text = self.viewModel.amountError?.localized
            }})
    }
    
    override func keyboardWillHide(notification: Notification) {
        if keyboardDismissGesture != nil {
            view.removeGestureRecognizer(keyboardDismissGesture!)
            keyboardDismissGesture = nil
        }
        tableView.setContentOffset(CGPoint(x: 0.0, y: 0.0), animated: true)
    }

    func onTransactionReady() {
       // if viewModel.isBipAddress() {
        //    addressInputType = .bip21 //analytics only
        //}
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendConfirmViewController") as? SendConfirmViewController, let tx = viewModel.transaction {
            vc.viewModel = SendConfirmViewModel(account: viewModel.account,
                                                tx: tx)
            vc.inputType = viewModel.inputType
            vc.addressInputType = addressInputType
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
        switch SendSection(rawValue: section) {
        case .alert:
            return viewModel.alertCellModel != nil ? 1 : 0
        case .accountAsset:
            return 1
        case .address:
            return 1
        case .amount:
            return 1
        case .fee:
            return !viewModel.account.gdkNetwork.lightning ? 1 : 0
        case .none:
            return 0
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        switch SendSection(rawValue: indexPath.section) {
        case .alert:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AlertCardCell", for: indexPath) as? AlertCardCell,
               let remoteAlert = viewModel.remoteAlert,
                let cellModel = viewModel.alertCellModel {
                cell.configure(cellModel,
                                   onLeft: nil,
                                   onRight: (remoteAlert.link ?? "" ).isEmpty ? nil : { () in
                    SafeNavigationManager.shared.navigate(remoteAlert.link)
                },
                               onDismiss: {[weak self] in
                    self?.viewModel.remoteAlert = nil
                    self?.reloadSections([.alert], animated: true)
                })
                cell.selectionStyle = .none
                return cell
            }
        case .accountAsset:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "ReceiveAssetCell") as? ReceiveAssetCell {
                let cellModel = viewModel.accountAssetCellModel
                cell.configure(model: cellModel, onTap: { [weak self] in self?.willSelectAccountAsset() })
                cell.selectionStyle = .none
                return cell
            }
        case .address:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AddressEditCell") as? AddressEditCell {
                let cellModel = viewModel.addressEditCellModel
                cell.configure(cellModel: cellModel, delegate: self)
                cell.selectionStyle = .none
                return cell
            }
        case .amount:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "AmountEditCell") as? AmountEditCell {
                let cellModel = viewModel.amountCellModel
                cell.configure(cellModel: cellModel, delegate: self)
                cell.selectionStyle = .none
                return cell
            }
        case .fee:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "FeeEditCell") as? FeeEditCell {
                let cellModel = viewModel.feeCellModel
                cell.configure(tx: viewModel.transaction, cellModel: cellModel, delegate: self)
                cell.selectionStyle = .none
                return cell
            }
        default:
            break
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch SendSection(rawValue: section) {
        case .alert:
            return 0.1
        case .accountAsset:
            return headerH
        case .address:
            return headerH
        case .amount:
            return headerH
        case .fee:
            return viewModel.account.gdkNetwork.lightning ? 0.1 : headerH
        default:
            return 0.1
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0.1
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        switch SendSection(rawValue: section) {
        case .alert:
            return nil
        case .address:
            if viewModel.account.gdkNetwork.lightning {
                return headerView("Lightning Invoice / LNURL".localized)
            } else if viewModel.inputType == .sweep {
                return headerView("id_enter_a_private_key_to_sweep".localized)
            } else {
                return headerView("id_enter_an_address".localized)
            }
        case .amount:
            return headerView("id_amount".localized)
        case .fee:
            return viewModel.account.gdkNetwork.lightning ? nil : headerView("id_fee".localized)
        case .accountAsset:
            return headerView("id_account__asset".localized)
        case .none:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
    }

    func headerView(_ txt: String) -> UIView {
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.gBlackBg()
        let title = UILabel(frame: .zero)
        title.setStyle(.sectionTitle)
        title.text = txt
        title.numberOfLines = 0
        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)
        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 25),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -25)
        ])
        return section
    }
}

extension SendViewController: DialogRecipientDeleteViewControllerDelegate {
    func didCancel() {
        //
    }
    func didDelete(_ index: Int) {
        viewModel.recipientCellModels.remove(at: index)
        reloadSections([.address, .amount], animated: true)
    }
}
extension SendViewController: DialogScanViewControllerDelegate {
    func didScan(value: String, index: Int?) {
        addressDidChange(text: value)
    }
    func didStop() {
    }
}

extension SendViewController: DialogCustomFeeViewControllerDelegate {
    func didSave(fee: UInt64?) {
        viewModel.feeRate = fee ?? 1000
        viewModel.transactionPriority = .Custom
        validateTransaction()
    }
}

extension SendViewController: FeeEditCellDelegate {
    func setCustomFee() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogCustomFeeViewController") as? DialogCustomFeeViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            vc.account = viewModel.account
            vc.storedFeeRate = viewModel.feeRate
            present(vc, animated: false, completion: nil)
        }
    }

    func updatePriority(_ priority: TransactionPriority) {
        viewModel.transactionPriority = priority
        validateTransaction()
    }
}

extension SendViewController: AccountAssetViewControllerDelegate {
    func willSelectAccountAsset() {
        if fixedAsset {
            return
        }
        if let vc = UIStoryboard(name: "Utility", bundle: nil).instantiateViewController(withIdentifier: "AccountAssetViewController") as? AccountAssetViewController {
            let accounts = fixedWallet ? [viewModel.account] : viewModel.wm?.subaccounts ?? []
            vc.viewModel = AccountAssetViewModel(accounts: accounts)
            vc.delegate = self
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func didSelectAccountAsset(account: WalletItem, asset: AssetInfo) {
        viewModel.account = account
        viewModel.assetId = asset.assetId
        tableView.reloadData()
        viewModel.validateInput()
            .done {
                self.refreshAmountCell()
                self.reloadSections([.address], animated: false)
                if self.viewModel.satoshi != nil {
                    self.validateTransaction()
                }
            }
            .catch { print($0) }
    }
}

extension SendViewController: AddressEditCellDelegate {
    func qrcodeScanner() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogScanViewController") as? DialogScanViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.index = nil
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }
    
    func paste() {
        if let text = UIPasteboard.general.string {
            addressDidChange(text: text)
        }
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }
    
    func addressDidChange(text: String) {
        viewModel.input = text
        viewModel.satoshi = nil
        cleanAmountCell()
        viewModel.validateInput()
            .done {
                self.refreshAmountCell()
                self.reloadSections([.address], animated: false)
                if self.viewModel.satoshi != nil {
                    self.validateTransaction()
                }
            }
            .catch { print($0) }
    }
}

extension SendViewController: AmountEditCellDelegate {
    func sendAll(enabled: Bool) {
        viewModel.sendAll = enabled
        viewModel.amount = nil
        reloadSections([.amount], animated: false)
        validateTransaction()
    }

    func amountDidChange(text: String, isFiat: Bool) {
        viewModel.isFiat = isFiat
        viewModel.amount = text
        validateTransaction()
    }

    func onFocus() {
        if !viewModel.account.type.lightning {
            tableView.scrollToRow(at: IndexPath(row: 0, section: SendSection.fee.rawValue), at: .bottom, animated: true)
        }
    }
}
