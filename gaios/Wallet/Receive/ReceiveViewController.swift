import Foundation
import UIKit
import PromiseKit
import LinkPresentation

public enum TransactionBaseType: UInt32 {
    case BTC = 0
    case FIAT = 1
}

class ReceiveViewController: UIViewController {

    @IBOutlet weak var cardQRCode: UIView!
    @IBOutlet weak var bgCardQR: UIView!
    @IBOutlet weak var btnQRCode: UIButton!
    @IBOutlet weak var lblAddress: UILabel!
    @IBOutlet weak var btnCopy: UIButton!
    @IBOutlet weak var btnShare: UIButton!
    @IBOutlet weak var btnEdit: UIButton!
    @IBOutlet weak var btnOptions: UIButton!
    @IBOutlet weak var qrFrame: UIView!
    @IBOutlet weak var btnVerify: UIButton!
    @IBOutlet weak var lblAssetTitle: UILabel!
    @IBOutlet weak var bgAssetCard: UIView!
    @IBOutlet weak var lblAddressTitle: UILabel!

    @IBOutlet weak var iconAsset: UIImageView!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblAccount: UILabel!

    private var selectedType = TransactionBaseType.BTC
    private var newAddressToken: NSObjectProtocol?
    private var satoshi: Int64?

    var viewModel: ReceiveViewModel!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        let userAccount = AccountsManager.shared.current
        btnVerify.isHidden = !(userAccount?.isHW == true && userAccount?.isLedger == false)
        btnEdit.isHidden = true
        let helpBtn = UIButton(type: .system)
        helpBtn.setImage(UIImage(named: "ic_help"), for: .normal)
        helpBtn.addTarget(self, action: #selector(helpBtnTap), for: .touchUpInside)
        // navigationItem.rightBarButtonItem = UIBarButtonItem(customView: helpBtn)

        view.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveScreen.view
        btnQRCode.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveScreen.qrCodeBtn
        btnOptions.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveScreen.moreOptionsBtn

        AnalyticsManager.shared.recordView(.receive, sgmt: AnalyticsManager.shared.subAccSeg(AccountsManager.shared.current, walletType: viewModel.account.type))

        viewModel.reload = reload
        viewModel.error = showError
        reload()
        viewModel.newAddress()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        newAddressToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil, queue: .main, using: newAddress)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = newAddressToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    func setContent() {
        title = "id_receive".localized
        btnShare.setTitle("id_share_address".localized, for: .normal)
        btnEdit.setTitle("id_edit".localized, for: .normal)
        btnOptions.setTitle("id_more_options".localized, for: .normal)
        btnVerify.setTitle("id_verify_on_device".localized, for: .normal)
        lblAssetTitle.text = "id_account__asset".localized
        lblAddressTitle.text = "id_account_address".localized
    }

    func setStyle() {
        btnShare.setStyle(.primary)
        btnEdit.setStyle(.outlinedWhite)
        btnOptions.setStyle(.outlinedWhite)
        btnVerify.setStyle(.outlinedWhite)
        btnCopy.cornerRadius = 5.0
        bgCardQR.layer.cornerRadius = 5.0
        bgAssetCard.layer.cornerRadius = 5.0
    }

    func reload() {
        iconAsset.image = viewModel.assetIcon()
        lblAsset.text = viewModel.assetName()
        lblAccount.text = viewModel.accountType().uppercased()
        lblAddress.text = viewModel.address?.address
        btnEdit.isHidden = true //self.viewModel.account.gdkNetwork.liquid
        if let address = viewModel.address?.address, !address.isEmpty {
            let uri = viewModel.addressToUri(address: address, satoshi: satoshi ?? 0)
            let dim = min(qrFrame.frame.size.width, qrFrame.frame.size.height)
            let frame = CGRect(x: 0.0, y: 0.0, width: dim, height: dim)
            btnQRCode.setImage(QRImageGenerator.imageForTextWhite(text: uri, frame: frame), for: .normal)
            btnQRCode.imageView?.contentMode = .scaleAspectFit
            btnQRCode.layer.cornerRadius = 5.0
            if let satoshi = self.satoshi, satoshi != 0 {
                lblAddress.text = uri
            }
        }
    }

    func newAddress(_ notification: Notification?) {
        viewModel?.newAddress()
    }

    func validate() {
        viewModel?.validateHw()
            .ensure {
                self.presentedViewController?.dismiss(animated: true, completion: nil)
            }.done { equal in
                if equal {
                    DropAlert().success(message: NSLocalizedString("id_the_address_is_valid", comment: ""))
                } else {
                    DropAlert().error(message: NSLocalizedString("id_the_addresses_dont_match", comment: ""))
                }
            }.catch { err in
                switch err {
                case JadeError.Abort(let desc),
                     JadeError.URLError(let desc),
                     JadeError.Declined(let desc):
                    DropAlert().error(message: desc)
                default:
                    DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
                }
            }
    }

    func isBipAddress(_ addr: String) -> Bool {
        return viewModel?.isBipAddress(addr) ?? false
    }

    @objc func copyToClipboard(_ sender: Any) {
        let address = (viewModel.address?.address)!
        let uri = viewModel.addressToUri(address: address, satoshi: satoshi ?? 0)
        let data = AnalyticsManager.ReceiveAddressData(type: self.isBipAddress(uri) ? AnalyticsManager.ReceiveAddressType.uri : AnalyticsManager.ReceiveAddressType.address,
                                                       media: AnalyticsManager.ReceiveAddressMedia.text,
                                                       method: AnalyticsManager.ReceiveAddressMethod.copy)
        AnalyticsManager.shared.receiveAddress(account: AccountsManager.shared.current,
                                               walletType: viewModel.account.type,
                                               data: data)
        UIPasteboard.general.string = uri
        DropAlert().info(message: NSLocalizedString("id_address_copied_to_clipboard", comment: ""), delay: 1.0)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }

    @objc func helpBtnTap() {
        UIApplication.shared.open(ExternalUrls.receiveTransactionHelp, options: [:], completionHandler: nil)
    }

    func optRequestAmount() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogReceiveRequestAmountViewController") as? DialogReceiveRequestAmountViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            vc.wallet = viewModel.account
            vc.prefill = self.satoshi
            present(vc, animated: false, completion: nil)
        }
    }

    func optSweep() {
        let storyboard = UIStoryboard(name: "Send", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
            let sendViewModel = SendViewModel(account: viewModel.account, inputType: .sweep, transaction: nil)
            vc.viewModel = sendViewModel
            navigationController?.pushViewController(vc, animated: true)
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
            let hideSweep = viewModel.account.gdkNetwork.liquid || viewModel.account.gdkNetwork.electrum
            vc.viewModel = DialogListViewModel(title: "id_share".localized,
                                               type: .moreOptPrefs,
                                               items: MoreOptPrefs.getItems(hideSweep: hideSweep))
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnVerify(_ sender: Any) {
        validate()
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogReceiveVerifyAddressViewController") as? DialogReceiveVerifyAddressViewController {
            vc.address = viewModel.address?.address ?? ""
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func refreshClick(_ sender: Any?) {
        viewModel.newAddress()
    }

    @IBAction func copyAction(_ sender: Any) {
        copyToClipboard(sender)
    }

    @IBAction func btnChangeReceiver(_ sender: Any) {
        AnalyticsManager.shared.changeAsset(account: AccountsManager.shared.current)
        let previousViewController = navigationController?.viewControllers.last { $0 != navigationController?.topViewController }
        let storyboard = UIStoryboard(name: "Utility", bundle: nil)
        if previousViewController is WalletViewController {
            // from WalletViewController, show assets and account selection
            if let vc = storyboard.instantiateViewController(withIdentifier: "AssetExpandableSelectViewController") as? AssetExpandableSelectViewController {
                let assets = WalletManager.current?.registry.all
                vc.viewModel = AssetExpandableSelectViewModel(assets: assets ?? [], enableAnyAsset: true, onlyFunded: false)
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        } else {
            // from AccountViewController, show only assets selection
            if let vc = storyboard.instantiateViewController(withIdentifier: "AssetSelectViewController") as? AssetSelectViewController {
                let showAmp = viewModel.accounts.filter { $0.type == .amp }.count > 0
                let showLiquid = viewModel.accounts.filter { $0.gdkNetwork.liquid }.count > 0
                let showBtc = viewModel.accounts.filter { !$0.gdkNetwork.liquid }.count > 0
                let assets: AssetAmountList? = WalletManager.current?.registry.all.filter {
                    (showAmp && $0.amp ?? false) ||
                    (showLiquid && $0.assetId != AssetInfo.btcId) ||
                    (showBtc && $0.assetId == AssetInfo.btcId)
                }.map { ($0.assetId, 0) }
                vc.viewModel = AssetSelectViewModel(assets: assets ?? AssetAmountList(), enableAnyAsset: true)
                vc.delegate = self
                navigationController?.pushViewController(vc, animated: true)
            }
        }
    }
}

extension ReceiveViewController: AssetExpandableSelectViewControllerDelegate {
    func didSelectReceiver(assetId: String, account: WalletItem) {
        viewModel?.asset = assetId
        viewModel?.account = account
        reload()
        viewModel.newAddress()
    }
}

extension ReceiveViewController: AssetSelectViewControllerDelegate {
    func didSelectAnyAsset() {
        /// handle any asset case
        viewModel?.asset = AssetInfo.lbtcId
        reload()
        viewModel.newAddress()
    }

    func didSelectAsset(_ assetId: String) {
        viewModel?.asset = assetId
        reload()
        viewModel.newAddress()
    }
}
extension ReceiveViewController: AccountSelectViewControllerDelegate {
    func didSelectAccount(_ account: WalletItem) {
        viewModel?.account = account
        reload()
        viewModel.newAddress()
    }
}

extension ReceiveViewController: DialogReceiveRequestAmountViewControllerDelegate {
    func didConfirm(satoshi: Int64?) {
        self.satoshi = satoshi
        reload()
    }

    func didCancel() {
        //
    }
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
        let image = (btnQRCode.imageView?.image)!
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

            switch MoreOptPrefs(rawValue: index) {
            case .requestAmount:
                optRequestAmount()
            case .sweep:
                optSweep()
            default:
                break
            }
        case .sharePrefs:

            guard let address = viewModel.address?.address, !address.isEmpty else { return }
            switch SharePrefs(rawValue: index) {
            case .none:
                return
            case .address:
                let uri = viewModel.addressToUri(address: address, satoshi: satoshi ?? 0)
                let activityViewController = UIActivityViewController(activityItems: [uri], applicationActivities: nil)
                activityViewController.popoverPresentationController?.sourceView = self.view
                self.present(activityViewController, animated: true, completion: nil)
                let data = AnalyticsManager.ReceiveAddressData(type: self.isBipAddress(uri) ? AnalyticsManager.ReceiveAddressType.uri : AnalyticsManager.ReceiveAddressType.address,
                                                               media: AnalyticsManager.ReceiveAddressMedia.text,
                                                               method: AnalyticsManager.ReceiveAddressMethod.share)
                AnalyticsManager.shared.receiveAddress(account: AccountsManager.shared.current,
                                                       walletType: viewModel.account.type,
                                                       data: data)
            case .qr:
                let uri = viewModel.addressToUri(address: address, satoshi: satoshi ?? 0)
                let image = (self.btnQRCode.imageView?.image)!
                let share = UIActivityViewController(activityItems: [image, self], applicationActivities: nil)
                self.present(share, animated: true, completion: nil)
                let data = AnalyticsManager.ReceiveAddressData(type: self.isBipAddress(uri) ? AnalyticsManager.ReceiveAddressType.uri : AnalyticsManager.ReceiveAddressType.address,
                                                               media: AnalyticsManager.ReceiveAddressMedia.image,
                                                               method: AnalyticsManager.ReceiveAddressMethod.share)
                AnalyticsManager.shared.receiveAddress(account: AccountsManager.shared.current,
                                                       walletType: viewModel.account.type,
                                                       data: data)
            }
        default:
            break
        }
    }
}
