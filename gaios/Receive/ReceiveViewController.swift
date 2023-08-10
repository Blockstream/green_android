import Foundation
import BreezSDK
import UIKit
import LinkPresentation
import gdk
import hw
import Combine

public enum TransactionBaseType: UInt32 {
    case BTC = 0
    case FIAT = 1
}

enum ReceiveSection: Int, CaseIterable {
    case asset = 0
    case amount
    case address
    case infoReceiveAmount
    case infoExpiredIn
    case note
}

class ReceiveViewController: KeyboardViewController {
    
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnShare: UIButton!
    @IBOutlet weak var btnEdit: UIButton!
    @IBOutlet weak var btnOptions: UIButton!
    @IBOutlet weak var btnVerify: UIButton!
    @IBOutlet weak var btnOnChain: UIButton!
    @IBOutlet weak var btnConfirm: UIButton!
    @IBOutlet weak var stackBottom: NSLayoutConstraint!
    
    private var selectedType = TransactionBaseType.BTC
    private var lightningAmountEditing = true
    private var newAddressToken, invoicePaidToken: NSObjectProtocol?
    private var headerH: CGFloat = 36.0
    private var loading = true
    private var keyboardVisible = false
    var viewModel: ReceiveViewModel!
    var dialogReceiveVerifyAddressViewController: DialogReceiveVerifyAddressViewController?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        register()
        setContent()
        setStyle()
        
        view.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveScreen.view
        btnOptions.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveScreen.moreOptionsBtn
    
        AnalyticsManager.shared.recordView(.receive, sgmt: AnalyticsManager.shared.subAccSeg(AccountsRepository.shared.current, walletItem: viewModel.account))
        reload()
        newAddress()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        invoicePaidToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.InvoicePaid.rawValue), object: nil, queue: .main, using: invoicePaid)
        newAddressToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil, queue: .main, using: newAddress)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = newAddressToken {
            NotificationCenter.default.removeObserver(token)
        }
        if let token = invoicePaidToken {
            NotificationCenter.default.removeObserver(token)
        }
    }
    
    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect ?? .zero
        let inset = (UIApplication.shared.windows.first?.safeAreaInsets.bottom ?? 0) - 5
        keyboardVisible = true
        stackBottom.constant = keyboardFrame.height - inset
        UIView.animate(withDuration: 0.5, animations: { [weak self] in
            self?.view.layoutIfNeeded()
            let network = self?.viewModel.account.gdkNetwork
            self?.btnOnChain.isHidden = !(network?.lightning ?? false) || self?.keyboardVisible ?? false
        })
    }
    
    override func keyboardWillHide(notification: Notification) {
        super.keyboardWillShow(notification: notification)
        keyboardVisible = false
        stackBottom.constant = 0.0
        UIView.animate(withDuration: 0.5, animations: { [weak self] in
            self?.view.layoutIfNeeded()
            let network = self?.viewModel.account.gdkNetwork
            self?.btnOnChain.isHidden = !(network?.lightning ?? false) || self?.keyboardVisible ?? false
        })
    }
    
    func register() {
        ["ReceiveAssetCell", "ReceiveAddressCell", "LTAmountCell", "LTInfoCell", "LTNoteCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
    }
    
    func setContent() {
        title = "id_receive".localized
        btnShare.setTitle("id_share".localized, for: .normal)
        btnEdit.setTitle("id_edit".localized, for: .normal)
        btnOptions.setTitle("id_more_options".localized, for: .normal)
        btnVerify.setTitle("id_verify_on_device".localized, for: .normal)
        btnConfirm.setTitle("id_confirm".localized, for: .normal)
    }
    
    func setStyle() {
        btnShare.setStyle(.primary)
        [btnEdit, btnOptions, btnVerify].forEach{ $0.setStyle(.outlinedWhite) }
        btnOnChain.semanticContentAttribute = UIApplication.shared.userInterfaceLayoutDirection == .rightToLeft ? .forceLeftToRight : .forceRightToLeft
        stateDidChange(.disabled)
    }
    
    var sections: [ReceiveSection] {
        switch viewModel.type {
        case .bolt11:
            if lightningAmountEditing {
                return [.asset, .amount]
            } else if viewModel.description == nil {
                return [.asset, .amount, .address, .infoReceiveAmount, .infoExpiredIn]
            } else {
                return ReceiveSection.allCases
            }
        case .swap:
            return [.asset, .address]
        case .address:
            return [.asset, .address]
        }
    }
    
    func reload() {
        let network = viewModel.account.gdkNetwork
        btnOnChain.isHidden = !network.lightning || keyboardVisible
        btnEdit.isHidden = network.liquid || network.lightning || viewModel.satoshi == nil
        btnOptions.isHidden = network.lightning
        btnConfirm.isHidden = !(network.lightning && lightningAmountEditing)
        btnShare.isHidden = !(!network.lightning || !lightningAmountEditing)
        if viewModel.type == .swap {
            btnConfirm.isHidden = true
            btnShare.isHidden = false
        }
        let userAccount = viewModel.wm.account
        btnVerify.isHidden = !(userAccount.isHW == true && userAccount.isLedger == false)
        btnOnChain.setTitle(viewModel.type == .bolt11 ? "id_show_onchain_address".localized : "id_show_lightning_invoice".localized, for: .normal)
        reloadNavigationBtns()
        tableView.reloadData()
    }
    
    func reloadNavigationBtns() {
        let network = viewModel.account.gdkNetwork
        if network.lightning {
            let settingsBtn = UIButton(type: .system)
            settingsBtn.setStyle(.inline)
            settingsBtn.setTitle("id_add_note".localized, for: .normal)
            settingsBtn.addTarget(self, action: #selector(editNoteBtnTapped), for: .touchUpInside)
            navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
        } else {
            let helpBtn = UIButton(type: .system)
            helpBtn.setImage(UIImage(named: "ic_help"), for: .normal)
            helpBtn.addTarget(self, action: #selector(helpBtnTap), for: .touchUpInside)
            navigationItem.rightBarButtonItem = UIBarButtonItem(customView: helpBtn)
        }
    }
    
    func invoicePaid(_ notification: Notification? = nil) {
        Task {
            do {
                let invoice = notification?.object as? InvoicePaidDetails
                let account = WalletManager.current?.lightningSubaccount
                let parser = Parser(selectedAccount: account!, input: invoice?.bolt11 ?? "", discoverable: true)
                try await parser.parse()
                switch parser.lightningType {
                case .some(.bolt11(let invoice)):
                    if let balance = Balance.fromSatoshi(invoice.amountSatoshi ?? 0, assetId: AssetInfo.btcId) {
                        let (amount, denom) = balance.toDenom(viewModel.inputDenomination)
                        let model = LTSuccessViewModel(account: account?.name ?? "", amount: amount, denom: denom)
                        ltSuccessViewController(model: model)
                    }
                default:
                    break
                }
            } catch { print(error) }
        }
    }

    func newAddress(_ notification: Notification? = nil) {
        loading = true
        reload()
        Task {
            do {
                try await viewModel?.newAddress()
            } catch { failure(error) }
            self.loading = false
            self.reload()
        }
    }
    
    func failure(_ err: Error) {
        switch err {
        case BreezSDK.SdkError.Generic(let msg),
            BreezSDK.SdkError.LspConnectFailed(let msg),
            BreezSDK.SdkError.PersistenceFailure(let msg),
            BreezSDK.SdkError.ReceivePaymentFailed(let msg):
            self.showError(msg)
        default:
            super.showError(err)
        }
    }

    func validate() {
        Task {
            do {
                let res = try await viewModel?.validateHw()
                await MainActor.run {
                    if res ?? false {
                        dialogReceiveVerifyAddressViewController?.dismiss()
                        DropAlert().success(message: NSLocalizedString("id_the_address_is_valid", comment: ""))
                    } else {
                        DropAlert().error(message: NSLocalizedString("id_the_addresses_dont_match", comment: ""))
                    }
                }
            } catch {
                await MainActor.run {
                    dialogReceiveVerifyAddressViewController?.dismiss()
                    switch error {
                    case HWError.Abort(let desc),
                        HWError.URLError(let desc),
                        HWError.Declined(let desc):
                        DropAlert().error(message: desc)
                    default:
                        DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
                    }
                }
            }
        }
    }

    func isBipAddress(_ addr: String) -> Bool {
        return viewModel?.isBipAddress(addr) ?? false
    }

    @objc func copyToClipboard(_ sender: Any? = nil) {
        guard let text = viewModel.text else { return }
        let data = AnalyticsManager.ReceiveAddressData(type: self.isBipAddress(text) ? AnalyticsManager.ReceiveAddressType.uri : AnalyticsManager.ReceiveAddressType.address,
                                                       media: AnalyticsManager.ReceiveAddressMedia.text,
                                                       method: AnalyticsManager.ReceiveAddressMethod.copy)
        AnalyticsManager.shared.receiveAddress(account: AccountsRepository.shared.current,
                                               walletItem: viewModel.account,
                                               data: data)
        UIPasteboard.general.string = text
        DropAlert().info(message: "id_address_copied_to_clipboard".localized, delay: 1.0)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }

    func imgToShare() -> UIImage {
        guard let text = viewModel.text else { return UIImage() }
        let frame = CGRect(x: 0.0, y: 0.0, width: 256, height: 256)
        return QRImageGenerator.imageForTextWhite(text: text, frame: frame) ?? UIImage()
    }

    @objc func helpBtnTap() {
        UIApplication.shared.open(ExternalUrls.receiveTransactionHelp, options: [:], completionHandler: nil)
    }

    func optRequestAmount() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogAmountViewController") as? DialogAmountViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            vc.wallet = viewModel.account
            vc.prefill = self.viewModel.satoshi
            present(vc, animated: false, completion: nil)
        }
    }

    func optSweep() {
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
            let sendViewModel = SendViewModel(account: viewModel.account, inputType: .sweep, transaction: nil, input: nil)
            vc.viewModel = sendViewModel
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func optAddressAuth() {
        let storyboard = UIStoryboard(name: "AddressAuth", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "AddressAuthViewController") as? AddressAuthViewController {
            // add required model info
            vc.viewModel = AddressAuthViewModel(wallet: viewModel.account)
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func onChangeReceiver() async {
        AnalyticsManager.shared.changeAsset(account: AccountsRepository.shared.current)
        let previousViewController = navigationController?.viewControllers.last { $0 != navigationController?.topViewController }
        let storyboard = UIStoryboard(name: "Utility", bundle: nil)
        if previousViewController is WalletViewController {
            // from WalletViewController, show assets and account selection
            if let vc = storyboard.instantiateViewController(withIdentifier: "AssetExpandableSelectViewController") as? AssetExpandableSelectViewController {
                vc.viewModel = viewModel.getAssetExpandableSelectViewModel()
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        } else {
            // from AccountViewController, show only assets selection
            if let vc = storyboard.instantiateViewController(withIdentifier: "AssetSelectViewController") as? AssetSelectViewController {
                vc.viewModel = viewModel.getAssetSelectViewModel()
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        }
    }

    func onRefreshClick() {
        newAddress()
    }

    func magnifyQR() {
        let stb = UIStoryboard(name: "Utility", bundle: nil)
        if let vc = stb.instantiateViewController(withIdentifier: "MagnifyQRViewController") as? MagnifyQRViewController {
            vc.qrTxt = viewModel.text
            vc.modalPresentationStyle = .overFullScreen
            self.present(vc, animated: false, completion: nil)
            UINotificationFeedbackGenerator().notificationOccurred(.success)
        }
    }

    func ltSuccessViewController(model: LTSuccessViewModel) {
        let storyboard = UIStoryboard(name: "LTFlow", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "LTSuccessViewController") as? LTSuccessViewController {
            vc.viewModel = model
            vc.delegate = self
            vc.modalPresentationStyle = .overFullScreen
            self.present(vc, animated: false, completion: nil)
        }
    }

    @objc func editNoteBtnTapped() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogEditViewController") as? DialogEditViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.prefill = ""
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    func showLightningFeeInfo() {
        SafeNavigationManager.shared.navigate( ExternalUrls.helpReceiveFees )
    }

    func showDialogInputDenominations() {
        let model = viewModel.dialogInputDenominationViewModel()
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogInputDenominationViewController") as? DialogInputDenominationViewController, let balance = viewModel.getBalance() {
            vc.viewModel = model
            vc.delegate = self
            vc.balance = balance
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnShare(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: "id_share".localized,
                                               type: .sharePrefs,
                                               items: SharePrefs.getItems())
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnEdit(_ sender: Any) {
        optRequestAmount()
    }

    @IBAction func btnOptions(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            
            vc.viewModel = DialogListViewModel(title: "id_more_options".localized,
                                               type: .moreOptPrefs,
                                               items: MoreOptPrefs.getItems(account: viewModel.account))
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnVerify(_ sender: Any) {
        AnalyticsManager.shared.verifyAddressJade(account: AccountsRepository.shared.current, walletItem: viewModel.account)
        validate()
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogReceiveVerifyAddressViewController") as? DialogReceiveVerifyAddressViewController {
            vc.address = viewModel.address?.address ?? ""
            vc.walletItem = viewModel.account
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
            dialogReceiveVerifyAddressViewController = vc
        }
    }

    @IBAction func btnConfirm(_ sender: Any) {
        if viewModel.satoshi != nil {
            lightningAmountEditing = false
            newAddress()
        }
        
    }

    @IBAction func btnOnChain(_ sender: Any) {
        viewModel.type = viewModel.type == .bolt11 ? .swap : .bolt11
        switch viewModel.type {
        case .bolt11:
            if viewModel.invoice == nil {
                newAddress()
            }
        case .swap:
            if viewModel.swap == nil {
                newAddress()
            }
        default:
            break
        }
        reload()
    }
}

extension ReceiveViewController: AssetExpandableSelectViewControllerDelegate {
    func didSelectReceiver(assetId: String, account: WalletItem) {
        viewModel.asset = assetId
        viewModel.account = account
        viewModel.type = account.gdkNetwork.lightning ? .bolt11 : .address
        reload()
        newAddress()
    }
}

extension ReceiveViewController: AssetSelectViewControllerDelegate {
    func didSelectAnyAsset() {
        /// handle any asset case
        viewModel?.asset = AssetInfo.lbtcId
        reload()
        newAddress()
    }

    func didSelectAsset(_ assetId: String) {
        viewModel?.asset = assetId
        reload()
        newAddress()
    }
}
extension ReceiveViewController: AccountSelectViewControllerDelegate {
    func didSelectAccount(_ account: WalletItem) {
        viewModel?.account = account
        viewModel.type = account.gdkNetwork.lightning ? .bolt11 : .address
        reload()
        newAddress()
    }
}

extension ReceiveViewController: DialogAmountViewControllerDelegate {
    func didConfirm(satoshi: Int64?) {
        self.viewModel.satoshi = satoshi
        reload()
    }

    func didCancel() { }
}

extension ReceiveViewController: UIActivityItemSource {
    func activityViewControllerPlaceholderItem(_ activityViewController: UIActivityViewController) -> Any {
        return ""
    }

    func activityViewController(_ activityViewController: UIActivityViewController, itemForActivityType activityType: UIActivity.ActivityType?) -> Any? {
        return nil
    }

    @available(iOS 13.0, *)
    func activityViewControllerLinkMetadata(_ activityViewController: UIActivityViewController) -> LPLinkMetadata? {
        let image = imgToShare()
        let imageProvider = NSItemProvider(object: image)
        let metadata = LPLinkMetadata()
        metadata.imageProvider = imageProvider
        return metadata
    }
}

extension ReceiveViewController: DialogListViewControllerDelegate {
    func didSelectIndex(_ index: Int, with type: DialogType) {
        switch type {
        case .moreOptPrefs:

            if let item = MoreOptPrefs.getPrefs(account: viewModel.account)[safe: index] {
                switch item {
                case .requestAmount:
                    optRequestAmount()
                case .sweep:
                    optSweep()
                case .addressAuth:
                    optAddressAuth()
                }
            }
        case .sharePrefs:

            switch SharePrefs(rawValue: index) {
            case .none:
                return
            case .address:
                let uri = viewModel.text
                let activityViewController = UIActivityViewController(activityItems: [uri ?? ""], applicationActivities: nil)
                activityViewController.popoverPresentationController?.sourceView = self.view
                self.present(activityViewController, animated: true, completion: nil)
                let data = AnalyticsManager.ReceiveAddressData(type: self.isBipAddress(uri ?? "") ? AnalyticsManager.ReceiveAddressType.uri : AnalyticsManager.ReceiveAddressType.address,
                                                               media: AnalyticsManager.ReceiveAddressMedia.text,
                                                               method: AnalyticsManager.ReceiveAddressMethod.share)
                AnalyticsManager.shared.receiveAddress(account: AccountsRepository.shared.current,
                                                       walletItem: viewModel.account,
                                                       data: data)
            case .qr:
                let uri = viewModel.text
                let image = imgToShare()
                let share = UIActivityViewController(activityItems: [image, self], applicationActivities: nil)
                self.present(share, animated: true, completion: nil)
                let data = AnalyticsManager.ReceiveAddressData(type: self.isBipAddress(uri ?? "") ? AnalyticsManager.ReceiveAddressType.uri : AnalyticsManager.ReceiveAddressType.address,
                                                               media: AnalyticsManager.ReceiveAddressMedia.image,
                                                               method: AnalyticsManager.ReceiveAddressMethod.share)
                AnalyticsManager.shared.receiveAddress(account: AccountsRepository.shared.current,
                                                       walletItem: viewModel.account,
                                                       data: data)
            }
        default:
            break
        }
    }
}

extension ReceiveViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return sections.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch sections[section] {
        case ReceiveSection.asset:
            return 1
        case ReceiveSection.address:
            return 1
        case ReceiveSection.amount:
            return 1
        case ReceiveSection.infoReceiveAmount:
            return 1
        case ReceiveSection.infoExpiredIn:
            return 1
        case ReceiveSection.note:
            return 1
        }
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        switch sections[indexPath.section] {
        case ReceiveSection.asset:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "ReceiveAssetCell") as? ReceiveAssetCell {
                let model = viewModel.assetCellModel
                cell.configure(model: model, onTap: { [weak self] in Task { await self?.onChangeReceiver() } })
                cell.selectionStyle = .none
                return cell
            }
        case ReceiveSection.amount:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "LTAmountCell") as? LTAmountCell {
                let model = viewModel.amountCellModel
                cell.configure(model: model, delegate: self, enabled: lightningAmountEditing)
                cell.selectionStyle = .none
                return cell
            }
        case ReceiveSection.address:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "ReceiveAddressCell") as? ReceiveAddressCell {
                let model = viewModel.addressCellModel
                cell.configure(model: model, isAnimating: loading) { [weak self] in self?.copyToClipboard()
                } onRefreshClick: { [weak self] in
                    self?.onRefreshClick()
                } onLongpress: { [weak self] in
                    self?.magnifyQR()
                }
                cell.selectionStyle = .none
                return cell
            }
        case ReceiveSection.infoReceiveAmount:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "LTInfoCell") as? LTInfoCell {
                cell.configure(model: viewModel.infoReceivedAmountCellModel)
                cell.selectionStyle = .none
                return cell
            }
        case ReceiveSection.infoExpiredIn:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "LTInfoCell") as? LTInfoCell {
                cell.configure(model: viewModel.infoExpiredInCellModel)
                cell.selectionStyle = .none
                return cell
            }
        case ReceiveSection.note:
            if let cell = tableView.dequeueReusableCell(withIdentifier: "LTNoteCell") as? LTNoteCell {
                cell.configure(model: viewModel.noteCellModel) { [weak self] in
                    self?.editNoteBtnTapped()
                }
                cell.selectionStyle = .none
                return cell
            }
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch sections[section] {
        case ReceiveSection.asset:
            return headerH
        case ReceiveSection.address:
            return headerH
        case ReceiveSection.amount:
            return headerH
        case ReceiveSection.note:
            return headerH
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
        switch sections[section] {
        case ReceiveSection.asset:
            return headerView("id_account__asset".localized)
        case ReceiveSection.address:
            switch viewModel.type {
            case .address:
                return headerView("id_account_address".localized)
            case .bolt11:
                return headerView("id_lightning_invoice".localized)
            case .swap:
                return headerView("id_onchain_address".localized)
            }
        case ReceiveSection.amount:
            return headerView("id_amount".localized)
        case ReceiveSection.note:
            return headerView("id_note".localized)
        case .infoReceiveAmount:
            return nil
        case .infoExpiredIn:
            return nil
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

    }
}

extension ReceiveViewController {
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

extension ReceiveViewController: DialogEditViewControllerDelegate {

    func didSave(_ note: String) {
        viewModel.description = note
        newAddress()
    }

    func didClose() { }
}

extension ReceiveViewController: LTSuccessViewControllerDelegate {
    func onDone() {
        print("Done")
    }
}

extension ReceiveViewController: LTAmountCellDelegate {
    
    func onInputDenomination() {
        showDialogInputDenominations()
    }
    
    func onFeeInfo() {
        showLightningFeeInfo()
    }
    
    func textFieldEnabled() {
        lightningAmountEditing = true
        reload()
    }
    
    func textFieldDidChange(_ satoshi: Int64?, isFiat: Bool) {
        viewModel.satoshi = satoshi
        viewModel.isFiat = isFiat
        tableView.beginUpdates()
        tableView.endUpdates()
    }
    func stateDidChange(_ state: LTAmountCellState) {
        viewModel.state = state
        btnConfirm.isEnabled = viewModel.state == .valid || viewModel.state == .validFunding
        btnConfirm.setStyle( btnConfirm.isEnabled ? .primary : .primaryGray)
    }
}

extension ReceiveViewController: DialogInputDenominationViewControllerDelagate {

    func didSelectFiat() {
        viewModel.isFiat = true
        tableView.reloadData()
    }

    func didSelectInput(denomination: gdk.DenominationType) {
        viewModel.isFiat = false
        viewModel?.inputDenomination = denomination
        tableView.reloadData()
    }
}
