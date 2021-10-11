import Foundation
import UIKit
import PromiseKit

class ReceiveBtcViewController: KeyboardViewController {

    @IBOutlet var content: ReceiveBtcView!
    var wallet: WalletItem?
    var selectedType = TransactionType.BTC
    var gestureTap: UITapGestureRecognizer?
    var gestureTapQRCode: UITapGestureRecognizer?
    var gestureTapAccountId: UITapGestureRecognizer?

    private var newAddressToken: NSObjectProtocol?
    private var account = AccountsManager.shared.current

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_receive", comment: "")
        gestureTap = UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard))
        gestureTapQRCode = UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard))
        gestureTapAccountId = UITapGestureRecognizer(target: self, action: #selector(self.copyAccountIdToClipboard))
        content.amountTextfield.attributedPlaceholder = NSAttributedString(string: "0.00".localeFormattedString(2),
                                                             attributes: [NSAttributedString.Key.foregroundColor: UIColor.white])

        content.walletQRCode.isUserInteractionEnabled = true
        content.walletAddressLabel.isUserInteractionEnabled = true
        content.walletQRCode.addGestureRecognizer(gestureTapQRCode!)
        content.walletAddressLabel.addGestureRecognizer(gestureTap!)
        content.amountLabel.text = NSLocalizedString("id_amount", comment: "")
        content.shareButton.setTitle(NSLocalizedString("id_share_address", comment: ""), for: .normal)
        content.shareButton.setGradient(true)

        let isLiquid = account?.gdkNetwork?.liquid ?? false
        content.amountView.isHidden = isLiquid

        content.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveBtcScreen.view
        content.walletQRCode.accessibilityIdentifier = AccessibilityIdentifiers.ReceiveBtcScreen.qrCodeView
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        newAddressToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil, queue: .main, using: newAddress)
        content.amountTextfield.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.fiatSwitchButton.addTarget(self, action: #selector(fiatSwitchButtonClick(_:)), for: .touchUpInside)
        content.shareButton.addTarget(self, action: #selector(shareButtonClicked(_:)), for: .touchUpInside)
        refreshClick(nil)
        updateEstimate()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = newAddressToken {
            NotificationCenter.default.removeObserver(token)
        }
        guard gestureTap != nil else { return }
        content.walletQRCode.removeGestureRecognizer(gestureTapQRCode!)
        content.walletAddressLabel.removeGestureRecognizer(gestureTap!)
        content.walletAddressLabel.removeGestureRecognizer(gestureTapAccountId!)
        content.amountTextfield.removeTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.fiatSwitchButton.removeTarget(self, action: #selector(fiatSwitchButtonClick(_:)), for: .touchUpInside)
        content.shareButton.removeTarget(self, action: #selector(shareButtonClicked(_:)), for: .touchUpInside)
    }

    @IBAction func refreshClick(_ sender: Any?) {
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil, userInfo: ["pointer": self.wallet?.pointer ?? 0])
    }

    func newAddress(_ notification: Notification?) {
        let dict = notification?.userInfo as NSDictionary?
        let pointer = dict?["pointer"] as? UInt32
        guard wallet?.pointer == pointer else {
            return
        }
        Address.generate(with: wallet!)
            .done { addr in
                self.wallet?.receiveAddress = addr.address
                self.reload()
                if self.account?.isHW ?? false {
                    if self.account?.isLedger ?? false {
                        //Ledger does not suport address validation
                    } else {
                        self.validate(addr: addr)
                    }
                }
            }.catch { _ in
                DropAlert().error(message: NSLocalizedString("id_connection_failed", comment: ""))
            }
    }

    func validate(addr: Address) {
        let hw: HWProtocol = account?.isLedger ?? false ? Ledger.shared : Jade.shared
        firstly {
            DropAlert().info(message: NSLocalizedString("id_please_verify_that_the_address", comment: ""))
            return Guarantee()
        }.then {
            Address.validate(with: self.wallet!, hw: hw, addr: addr, network: AccountsManager.shared.current!.network)
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
        setButton()
        updateEstimate()
    }

    @objc func copyAccountIdToClipboard(_ sender: Any) {
        UIPasteboard.general.string = wallet?.receivingId ?? ""
        DropAlert().info(message: NSLocalizedString("id_copied_to_clipboard", comment: ""), delay: 1.0)
    }

    @objc func copyToClipboard(_ sender: Any) {
        guard let wallet = self.wallet else { return }
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().then(on: bgq) {
            return wallet.getAddress()
        }.done { address in
            UIPasteboard.general.string = self.uriBitcoin(address: address)
            DropAlert().info(message: NSLocalizedString("id_address_copied_to_clipboard", comment: ""), delay: 1.0)
        }.catch { _ in }
    }

    @objc func fiatSwitchButtonClick(_ sender: Any) {
        let satoshi = getSatoshi() ?? 0
        let balance = Balance.convert(details: ["satoshi": satoshi])
        if let (amount, _) = balance?.get(tag: selectedType == TransactionType.BTC ? "fiat": "btc") {
            if amount == nil {
                showError(NSLocalizedString("id_your_favourite_exchange_rate_is", comment: ""))
                return
            }
        }
        if selectedType == TransactionType.BTC {
            selectedType = TransactionType.FIAT
        } else {
            selectedType = TransactionType.BTC
        }
        let tag = selectedType == TransactionType.BTC ? "btc" : "fiat"
        content.amountTextfield.text = String(format: "%@", balance?.get(tag: tag).0 ?? "")
        reload()
    }

    func setButton() {
        guard let settings = SessionManager.shared.settings else {
            return
        }
        if selectedType == TransactionType.BTC {
            content.fiatSwitchButton.setTitle(settings.denomination.string, for: UIControl.State.normal)
            content.fiatSwitchButton.backgroundColor = UIColor.customMatrixGreen()
            content.fiatSwitchButton.setTitleColor(UIColor.white, for: UIControl.State.normal)
        } else {
            let isMainnet = AccountsManager.shared.current?.gdkNetwork?.mainnet ?? true
            content.fiatSwitchButton.setTitle(isMainnet ? settings.getCurrency() : "FIAT", for: UIControl.State.normal)
            content.fiatSwitchButton.backgroundColor = UIColor.clear
            content.fiatSwitchButton.setTitleColor(UIColor.white, for: UIControl.State.normal)
        }
    }

    func updateEstimate() {
        let satoshi = getSatoshi() ?? 0
        let tag = selectedType == TransactionType.BTC ? "fiat": "btc"
        if let (amount, denom) = Balance.convert(details: ["satoshi": satoshi])?.get(tag: tag) {
            content.estimateLabel.text = "≈ \(amount ?? "N.A.") \(denom)"
        }
    }

    func updateQRCode() {
        guard let wallet = self.wallet else {
            content.walletAddressLabel.isHidden = true
            content.walletQRCode.isHidden = true
            return
        }
        guard let address = wallet.receiveAddress, !address.isEmpty else {
            return
        }
        let uri = uriBitcoin(address: address)
        content.walletAddressLabel.text = uri
        content.walletQRCode.image = QRImageGenerator.imageForTextWhite(text: uri, frame: content.walletQRCode.frame)
    }

    func uriBitcoin(address: String) -> String {
        let satoshi = self.getSatoshi() ?? 0
        let isLiquid = AccountsManager.shared.current?.gdkNetwork?.liquid ?? false
        if isLiquid || satoshi == 0 {
            return address
        }
        return String(format: "bitcoin:%@?amount=%.8f", address, getBTC() ?? 0)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        content.shareButton.updateGradientLayerFrame()
    }

    @objc func textFieldDidChange(_ textField: UITextField) {
        reload()
    }

    @objc func shareButtonClicked(_ sender: UIButton?) {
        guard let wallet = self.wallet else { return }
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().then(on: bgq) {
            return wallet.getAddress()
        }.done { address in
            if address.isEmpty {
                throw GaError.GenericError
            }
            let uri = self.uriBitcoin(address: address)
            let activityViewController = UIActivityViewController(activityItems: [uri], applicationActivities: nil)
            activityViewController.popoverPresentationController?.sourceView = self.view
            self.present(activityViewController, animated: true, completion: nil)
        }.catch { _ in }
    }

    func getSatoshi() -> UInt64? {
        var amountText = content.amountTextfield.text!
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else { return nil }
        let denomination = SessionManager.shared.settings!.denomination
        let key = selectedType == TransactionType.BTC ? denomination.rawValue : "fiat"
        return Balance.convert(details: [key: amountText])?.satoshi
    }

    func getBTC() -> Double? {
        guard let satoshi = getSatoshi() else { return nil }
        return Double(satoshi) / 100000000
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let next = segue.destination as? AccountInfoViewController {
            next.transitioningDelegate = self
            next.modalPresentationStyle = .custom
            next.accountInfoType = .accountID
        }
    }
}

extension ReceiveBtcViewController: UIViewControllerTransitioningDelegate {
    func presentationController(forPresented presented: UIViewController, presenting: UIViewController?, source: UIViewController) -> UIPresentationController? {
        return ModalPresentationController(presentedViewController: presented, presenting: presenting)
    }

    func animationController(forPresented presented: UIViewController, presenting: UIViewController, source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        ModalAnimator(isPresenting: true)
    }
}

public enum TransactionType: UInt32 {
    case BTC = 0
    case FIAT = 1
}

@IBDesignable
class ReceiveBtcView: UIView {
    @IBOutlet weak var walletAddressLabel: UILabel!
    @IBOutlet weak var walletQRCode: UIImageView!
    @IBOutlet weak var amountTextfield: UITextField!
    @IBOutlet weak var estimateLabel: UILabel!
    @IBOutlet weak var shareButton: UIButton!
    @IBOutlet weak var fiatSwitchButton: UIButton!
    @IBOutlet weak var amountLabel: UILabel!
    @IBOutlet weak var amountView: UIView!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        shareButton.updateGradientLayerFrame()
    }
}
