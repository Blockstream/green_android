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

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_receive", comment: "")
        tabBarController?.tabBar.isHidden = true
        gestureTap = UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard))
        gestureTapQRCode = UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard))
        gestureTapAccountId = UITapGestureRecognizer(target: self, action: #selector(self.copyAccountIdToClipboard))
        content.amountTextfield.attributedPlaceholder = NSAttributedString(string: "0.00".localeFormattedString(2),
                                                             attributes: [NSAttributedString.Key.foregroundColor: UIColor.white])

        content.walletQRCode.isUserInteractionEnabled = true
        content.walletAddressLabel.isUserInteractionEnabled = true
        content.accountValue.isUserInteractionEnabled = true
        content.walletQRCode.addGestureRecognizer(gestureTapQRCode!)
        content.walletAddressLabel.addGestureRecognizer(gestureTap!)
        content.accountValue.addGestureRecognizer(gestureTapAccountId!)
        content.amountLabel.text = NSLocalizedString("id_amount", comment: "")
        content.accountTitle.text = NSLocalizedString("id_account_id", comment: "")
        content.shareButton.setTitle(NSLocalizedString("id_share_address", comment: ""), for: .normal)
        content.shareButton.setGradient(true)

        let isLiquid = getGdkNetwork(getNetwork()).liquid
        content.amountView.isHidden = isLiquid
        content.accountView.isHidden = !(isLiquid && "2of2_no_recovery" == wallet?.type)
        if isLiquid && "2of2_no_recovery" == wallet?.type {
            content.accountValue.text = wallet?.receivingId ?? ""
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        newAddressToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil, queue: .main, using: newAddress)
        content.amountTextfield.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.fiatSwitchButton.addTarget(self, action: #selector(fiatSwitchButtonClick(_:)), for: .touchUpInside)
        content.shareButton.addTarget(self, action: #selector(shareButtonClicked(_:)), for: .touchUpInside)
        content.accountButton.addTarget(self, action: #selector(authInfoTapped(_:)), for: .touchUpInside)
        reload()
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
        content.shareButton.removeTarget(self, action: #selector(authInfoTapped(_:)), for: .touchUpInside)
    }

    @IBAction func refreshClick(_ sender: Any) {
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil, userInfo: ["pointer": self.wallet?.pointer ?? 0])
    }

    func newAddress(_ notification: Notification?) {
        let dict = notification?.userInfo as NSDictionary?
        let pointer = dict?["pointer"] as? UInt32
        if wallet?.pointer == pointer {
            wallet?.generateNewAddress().done { address in
                self.wallet?.receiveAddress = address
                self.reload()
            }.catch { _ in }
        }
    }

    func reload() {
        updateQRCode()
        setButton()
        updateEstimate()
    }

    @objc func copyAccountIdToClipboard(_ sender: Any) {
        UIPasteboard.general.string = wallet?.receivingId ?? ""
        Toast.show(NSLocalizedString("id_copied_to_clipboard", comment: ""), timeout: Toast.SHORT)
    }

    @objc func copyToClipboard(_ sender: Any) {
        guard let wallet = self.wallet else { return }
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().then(on: bgq) {
            return wallet.getAddress()
        }.done { address in
            UIPasteboard.general.string = self.uriBitcoin(address: address)
            Toast.show(NSLocalizedString("id_address_copied_to_clipboard", comment: ""), timeout: Toast.SHORT)
        }.catch { _ in }
    }

    @objc func fiatSwitchButtonClick(_ sender: Any) {
        guard let satoshi = getSatoshi() else { return }
        let balance = Balance.convert(details: ["satoshi": satoshi])
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
        guard let settings = getGAService().getSettings() else {
            return
        }
        if selectedType == TransactionType.BTC {
            content.fiatSwitchButton.setTitle(settings.denomination.string, for: UIControl.State.normal)
            content.fiatSwitchButton.backgroundColor = UIColor.customMatrixGreen()
            content.fiatSwitchButton.setTitleColor(UIColor.white, for: UIControl.State.normal)
        } else {
            content.fiatSwitchButton.setTitle(settings.getCurrency(), for: UIControl.State.normal)
            content.fiatSwitchButton.backgroundColor = UIColor.clear
            content.fiatSwitchButton.setTitleColor(UIColor.white, for: UIControl.State.normal)
        }
    }

    func updateEstimate() {
        guard let satoshi = getSatoshi() else { return }
        if let (amount, denom) = Balance.convert(details: ["satoshi": satoshi])?.get(tag: selectedType == TransactionType.BTC ? "fiat": "btc") {
            content.estimateLabel.text = "≈ \(amount) \(denom)"
        }
    }

    func updateQRCode() {
        guard let wallet = self.wallet else {
            content.walletAddressLabel.isHidden = true
            content.walletQRCode.isHidden = true
            return
        }
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().then(on: bgq) {
            return wallet.getAddress()
        }.done { address in
            if address.isEmpty {
                throw GaError.GenericError
            }
            let uri = self.uriBitcoin(address: address)
            self.content.walletAddressLabel.text = uri
            self.content.walletQRCode.image = QRImageGenerator.imageForTextWhite(text: uri, frame: self.content.walletQRCode.frame)
        }.catch { _ in
            Toast.show(NSLocalizedString("id_you_are_not_connected_to_the", comment: ""), timeout: Toast.SHORT)
        }
    }

    func uriBitcoin(address: String) -> String {
        let satoshi = self.getSatoshi() ?? 0
        if getGdkNetwork(getNetwork()).liquid || satoshi == 0 {
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

    @objc func authInfoTapped(_ sender: UIButton?) {
        performSegue(withIdentifier: "auth_info", sender: nil)
    }

    func getSatoshi() -> UInt64? {
        var amountText = content.amountTextfield.text!
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else { return nil }
        let denomination = getGAService().getSettings()!.denomination
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
    @IBOutlet weak var accountView: UIView!
    @IBOutlet weak var accountButton: UIButton!
    @IBOutlet weak var accountValue: UILabel!
    @IBOutlet weak var accountTitle: UILabel!

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
