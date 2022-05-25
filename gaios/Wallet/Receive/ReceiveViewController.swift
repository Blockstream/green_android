import Foundation
import UIKit
import PromiseKit
import LinkPresentation

public enum TransactionType: UInt32 {
    case BTC = 0
    case FIAT = 1
}

class ReceiveViewController: UIViewController {

    @IBOutlet weak var cardQRCode: UIView!
    @IBOutlet weak var btnQRCode: UIButton!
    @IBOutlet weak var btnAddress: UIButton!
    @IBOutlet weak var btnCopy: UIButton!
    @IBOutlet weak var btnShare: UIButton!
    @IBOutlet weak var btnEdit: UIButton!
    @IBOutlet weak var btnOptions: UIButton!
    @IBOutlet weak var qrFrame: UIView!
    @IBOutlet weak var btnVerify: UIButton!

    var wallet: WalletItem?
    var selectedType = TransactionType.BTC

    private var newAddressToken: NSObjectProtocol?
    private var account = AccountsManager.shared.current
    var satoshi: UInt64?
    var address: Address?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        btnVerify.isHidden = !(self.account?.isHW == true && self.account?.isLedger == false)
        btnEdit.isHidden = true
        let helpBtn = UIButton(type: .system)
        helpBtn.setImage(UIImage(named: "ic_help"), for: .normal)
        helpBtn.addTarget(self, action: #selector(helpBtnTap), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: helpBtn)

        view.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveScreen.view
        btnQRCode.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveScreen.qrCodeBtn
        btnOptions.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveScreen.moreOptionsBtn
        btnAddress.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveScreen.addressBtn

        AnalyticsManager.shared.recordView(.receive, sgmt: AnalyticsManager.shared.subAccSeg(AccountsManager.shared.current, walletType: wallet?.type))
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        newAddressToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil, queue: .main, using: newAddress)
        refreshClick(nil)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = newAddressToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    func setContent() {
        title = NSLocalizedString("id_receive", comment: "")
        btnShare.setTitle(NSLocalizedString("id_share_address", comment: ""), for: .normal)
        btnEdit.setTitle(NSLocalizedString("id_edit", comment: ""), for: .normal)
        btnOptions.setTitle(NSLocalizedString("id_more_options", comment: ""), for: .normal)
        btnVerify.setTitle(NSLocalizedString("id_verify_on_device", comment: ""), for: .normal)
    }

    func setStyle() {
        cardQRCode.layer.cornerRadius = 5.0
        btnShare.setStyle(.primary)
        btnEdit.setStyle(.outlined)
        btnOptions.setStyle(.outlinedGray)
        btnVerify.layer.cornerRadius = 4.0
    }

    func newAddress(_ notification: Notification?) {
        let dict = notification?.userInfo as NSDictionary?
        let pointer = dict?["pointer"] as? UInt32
        guard let session = SessionsManager.current,
              let wallet = wallet,
              wallet.pointer == pointer else {
            return
        }
        Address.generate(with: session, wallet: wallet)
            .done { [weak self] addr in
                self?.address = addr
                self?.wallet?.receiveAddress = addr.address
                self?.reload()
            }.catch { _ in
                DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
            }
    }

    func validate() {
        guard let addr = self.address else { return }
        let hw: HWProtocol = account?.isLedger ?? false ? Ledger.shared : Jade.shared
        firstly {
            return Guarantee()
        }.then {
            Address.validate(with: self.wallet!, hw: hw, addr: addr, network: AccountsManager.shared.current!.network)
        }.ensure {
            self.presentedViewController?.dismiss(animated: true, completion: nil)
        }.done { addr in
            if self.wallet?.receiveAddress == addr {
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

    func reload() {
        updateQRCode()
    }

    func isBipAddress(_ addr: String) -> Bool {
        return addr.starts(with: "bitcoin:") || addr.starts(with: "liquidnetwork:")
    }

    @objc func copyToClipboard(_ sender: Any) {

        guard let wallet = self.wallet else { return }
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().then(on: bgq) {
            return wallet.getAddress()
        }.done { address in

            let data = AnalyticsManager.ReceiveAddressData(type: self.isBipAddress(self.uriBitcoin(address: address)) ? AnalyticsManager.ReceiveAddressType.uri : AnalyticsManager.ReceiveAddressType.address, media: AnalyticsManager.ReceiveAddressMedia.text, method: AnalyticsManager.ReceiveAddressMethod.copy)
            AnalyticsManager.shared.receiveAddress(account: AccountsManager.shared.current, walletType: wallet.type, data: data)

            UIPasteboard.general.string = self.uriBitcoin(address: address)
            DropAlert().info(message: NSLocalizedString("id_address_copied_to_clipboard", comment: ""), delay: 1.0)
            UINotificationFeedbackGenerator().notificationOccurred(.success)
        }.catch { _ in }
    }

    func updateQRCode() {
        guard let wallet = self.wallet else {
            btnAddress.isHidden = true
            btnQRCode.isHidden = true
            return
        }
        guard let address = wallet.receiveAddress, !address.isEmpty else {
            return
        }
        let uri = uriBitcoin(address: address)
        btnAddress.setTitle(uri, for: .normal)
        let dim = min(qrFrame.frame.size.width, qrFrame.frame.size.height)
        let frame = CGRect(x: 0.0, y: 0.0, width: dim, height: dim)
        btnQRCode.setImage(QRImageGenerator.imageForTextWhite(text: uri, frame: frame), for: .normal)
        btnQRCode.imageView?.contentMode = .scaleAspectFit
        btnQRCode.layer.cornerRadius = 5.0
    }

    func uriBitcoin(address: String) -> String {
        let ntwPrefix = (account?.gdkNetwork?.liquid ?? false) ? "liquidnetwork" : "bitcoin"
        if satoshi == nil || satoshi == 0 {
            btnEdit.isHidden = true
            return address
        }
        if !(account?.gdkNetwork?.liquid ?? false) {
            btnEdit.isHidden = false
        }
        return String(format: "%@:%@?amount=%.8f", ntwPrefix, address, toBTC(satoshi!))
    }

    func toBTC(_ satoshi: UInt64) -> Double {
        return Double(satoshi) / 100000000
    }

    @objc func helpBtnTap() {
        UIApplication.shared.open(ExternalUrls.receiveTransactionHelp, options: [:], completionHandler: nil)
    }

    @IBAction func btnShare(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogReceiveShareTypeViewController") as? DialogReceiveShareTypeViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnEdit(_ sender: Any) {
        didSelect(.requestAmount)
    }

    @IBAction func btnOptions(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogReceiveMoreOptionsViewController") as? DialogReceiveMoreOptionsViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.isLiquid = account?.gdkNetwork?.liquid ?? false
            vc.isSingleSig = account?.isSingleSig ?? false
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnVerify(_ sender: Any) {
        validate()
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogReceiveVerifyAddressViewController") as? DialogReceiveVerifyAddressViewController {
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func refreshClick(_ sender: Any?) {
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil, userInfo: ["pointer": self.wallet?.pointer ?? 0])
    }

    @IBAction func copyAction(_ sender: Any) {
        copyToClipboard(sender)
    }
}

extension ReceiveViewController: DialogReceiveMoreOptionsViewControllerDelegate {
    func didSelect(_ action: ReceiveOptionAction) {
        switch action {
        case .requestAmount:
            let storyboard = UIStoryboard(name: "Shared", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "DialogReceiveRequestAmountViewController") as? DialogReceiveRequestAmountViewController {
                vc.modalPresentationStyle = .overFullScreen
                vc.delegate = self
                vc.wallet = wallet
                vc.prefill = self.satoshi
                present(vc, animated: false, completion: nil)
            }
        case .sweep:
            let storyboard = UIStoryboard(name: "Send", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "SendViewController") as? SendViewController {
                vc.inputType = .sweep
                vc.wallet = wallet
                navigationController?.pushViewController(vc, animated: true)
            }
        case .cancel:
            break
        }
    }
}

extension ReceiveViewController: DialogReceiveRequestAmountViewControllerDelegate {
    func didConfirm(satoshi: UInt64?) {
        self.satoshi = satoshi
        updateQRCode()
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

extension ReceiveViewController: DialogReceiveShareTypeViewControllerDelegate {
    func didSelect(_ option: ReceiveShareOption) {

        if option == .cancel { return }
        guard let wallet = self.wallet else { return }

        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().then(on: bgq) {
            return wallet.getAddress()
        }.done { address in
            if address.isEmpty {
                throw GaError.GenericError
            }
            if option == .address {
                let uri = self.uriBitcoin(address: address)
            let activityViewController = UIActivityViewController(activityItems: [uri], applicationActivities: nil)
            activityViewController.popoverPresentationController?.sourceView = self.view
            self.present(activityViewController, animated: true, completion: nil)
            } else if option == .qr {
                let image = (self.btnQRCode.imageView?.image)!
                let share = UIActivityViewController(activityItems: [image, self], applicationActivities: nil)
                self.present(share, animated: true, completion: nil)
            }
            // analytics
            switch option {
            case .address:
                let data = AnalyticsManager.ReceiveAddressData(type: self.isBipAddress(self.uriBitcoin(address: address)) ? AnalyticsManager.ReceiveAddressType.uri : AnalyticsManager.ReceiveAddressType.address, media: AnalyticsManager.ReceiveAddressMedia.text, method: AnalyticsManager.ReceiveAddressMethod.share)
                AnalyticsManager.shared.receiveAddress(account: AccountsManager.shared.current, walletType: wallet.type, data: data)
            case .qr:
                let data = AnalyticsManager.ReceiveAddressData(type: self.isBipAddress(self.uriBitcoin(address: address)) ? AnalyticsManager.ReceiveAddressType.uri : AnalyticsManager.ReceiveAddressType.address, media: AnalyticsManager.ReceiveAddressMedia.image, method: AnalyticsManager.ReceiveAddressMethod.share)
                AnalyticsManager.shared.receiveAddress(account: AccountsManager.shared.current, walletType: wallet.type, data: data)
            case .cancel:
                break
            }
        }.catch { _ in }
    }
}
