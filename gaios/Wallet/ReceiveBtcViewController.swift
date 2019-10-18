import Foundation
import UIKit
import PromiseKit

class ReceiveBtcViewController: KeyboardViewController {

    @IBOutlet var content: ReceiveBtcView!
    var wallet: WalletItem?
    var selectedType = TransactionType.BTC
    var gestureTap: UITapGestureRecognizer?
    var gestureTapQRCode: UITapGestureRecognizer?

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_receive", comment: "")
        tabBarController?.tabBar.isHidden = true
        gestureTap = UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard))
        gestureTapQRCode = UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard))
        content.amountTextfield.attributedPlaceholder = NSAttributedString(string: "0.00",
                                                             attributes: [NSAttributedString.Key.foregroundColor: UIColor.white])

        content.walletQRCode.addGestureRecognizer(gestureTapQRCode!)
        content.walletQRCode.isUserInteractionEnabled = true
        content.walletAddressLabel.isUserInteractionEnabled = true
        content.walletAddressLabel.addGestureRecognizer(gestureTap!)
        content.amountLabel.text = NSLocalizedString("id_amount", comment: "")
        content.shareButton.setTitle(NSLocalizedString("id_share_address", comment: ""), for: .normal)
        content.shareButton.setGradient(true)

        if getGdkNetwork(getNetwork()).liquid {
            content.hideAmount()
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        NotificationCenter.default.addObserver(self, selector: #selector(self.newAddress(_:)), name: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil)
        content.amountTextfield.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.fiatSwitchButton.addTarget(self, action: #selector(fiatSwitchButtonClick(_:)), for: .touchUpInside)
        content.shareButton.addTarget(self, action: #selector(shareButtonClicked(_:)), for: .touchUpInside)
        reload()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil)
        guard gestureTap != nil else { return }
        content.walletQRCode.removeGestureRecognizer(gestureTapQRCode!)
        content.walletAddressLabel.removeGestureRecognizer(gestureTap!)
        content.amountTextfield.removeTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.fiatSwitchButton.removeTarget(self, action: #selector(fiatSwitchButtonClick(_:)), for: .touchUpInside)
        content.shareButton.removeTarget(self, action: #selector(shareButtonClicked(_:)), for: .touchUpInside)
    }

    @IBAction func refreshClick(_ sender: Any) {
        let pointers: [UInt32] = [self.wallet!.pointer]
        changeAddresses(pointers).done { (wallets: [WalletItem]) in
            wallets.forEach { wallet in
                guard let address = wallet.receiveAddress else { return }
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil, userInfo: ["pointer": wallet.pointer, "address": address])
            }
        }.catch { _ in }
    }

    @objc func newAddress(_ notification: NSNotification) {
        guard let dict = notification.userInfo as NSDictionary? else { return }
        guard let pointer = dict["pointer"] as? UInt32 else { return }
        guard let address = dict["address"] as? String else { return }
        if wallet?.pointer == pointer {
            wallet?.receiveAddress = address
            DispatchQueue.main.async {
                self.reload()
            }
        }
    }

    func reload() {
        updateQRCode()
        setButton()
        updateEstimate()
    }

    @objc func copyToClipboard(_ sender: Any) {
        guard let wallet = self.wallet else { return }
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().compactMap(on: bgq) {
            return wallet.getAddress()
        }.done { address in
            let uri = getGdkNetwork(getNetwork()).liquid || self.getSatoshi() == 0 ? address : Bip21Helper.btcURIforAmount(address: address, amount: self.getBTC() ?? 0)
            UIPasteboard.general.string = uri
            Toast.show(NSLocalizedString("id_address_copied_to_clipboard", comment: ""), timeout: Toast.SHORT)
        }.catch { _ in }
    }

    @objc func fiatSwitchButtonClick(_ sender: Any) {
        guard let satoshi = getSatoshi() else { return }
        guard let balance = Balance.convert(details: ["satoshi": satoshi]) else { return }
        if selectedType == TransactionType.BTC {
            selectedType = TransactionType.FIAT
        } else {
            selectedType = TransactionType.BTC
        }
        let tag = selectedType == TransactionType.BTC ? "btc" : "fiat"
        content.amountTextfield.text = String(format: "%@", balance.get(tag: tag).0)
        reload()
    }

    func setButton() {
        let settings = getGAService().getSettings()!
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
        guard let balance = Balance.convert(details: ["satoshi": satoshi]) else { return }
        let (amount, denom) = balance.get(tag: selectedType == TransactionType.BTC ? "fiat": "btc")
        content.estimateLabel.text = "≈ \(amount) \(denom)"
    }

    func updateQRCode() {
        guard let wallet = self.wallet else {
            content.walletAddressLabel.isHidden = true
            content.walletQRCode.isHidden = true
            return
        }
        let bgq = DispatchQueue.global(qos: .background)
        Guarantee().compactMap(on: bgq) {
            return wallet.getAddress()
        }.done { address in
            if address.isEmpty {
                throw GaError.GenericError
            }
            let uri: String
            if getGdkNetwork(getNetwork()).liquid {
                uri = address
                self.content.walletAddressLabel.text = address
            } else if self.getSatoshi() == 0 {
                uri = Bip21Helper.btcURIforAddress(address: address)
                self.content.walletAddressLabel.text = address
            } else {
                uri = Bip21Helper.btcURIforAmount(address: address, amount: self.getBTC() ?? 0)
                self.content.walletAddressLabel.text = uri
            }
            self.content.walletQRCode.image = QRImageGenerator.imageForTextWhite(text: uri, frame: self.content.walletQRCode.frame)
        }.catch { _ in
            Toast.show(NSLocalizedString("id_you_are_not_connected_to_the", comment: ""), timeout: Toast.SHORT)
        }
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
        Guarantee().compactMap(on: bgq) {
            return wallet.getAddress()
        }.done { address in
            if address.isEmpty {
                throw GaError.GenericError
            }
            let uri = self.getSatoshi() == 0 ? address : Bip21Helper.btcURIforAmount(address: address, amount: self.getBTC() ?? 0)
            let activityViewController = UIActivityViewController(activityItems: [uri], applicationActivities: nil)
            activityViewController.popoverPresentationController?.sourceView = self.view
            self.present(activityViewController, animated: true, completion: nil)
        }.catch { _ in }
    }

    func getSatoshi() -> UInt64? {
        var amountText = content.amountTextfield.text!
        amountText = amountText.replacingOccurrences(of: ",", with: ".")
        amountText = amountText.isEmpty ? "0" : amountText
        guard let number = Double(amountText) else { return nil }
        if number < 0 { return nil }
        let denomination = getGAService().getSettings()!.denomination
        let key = selectedType == TransactionType.BTC ? denomination.rawValue : "fiat"
        return Balance.convert(details: [key: amountText])?.satoshi
    }

    func getBTC() -> Double? {
        guard let satoshi = getSatoshi() else { return nil }
        return Double(satoshi) / 100000000
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

    func hideAmount() {
        amountLabel.isHidden = true
        amountTextfield.isHidden = true
        fiatSwitchButton.isHidden = true
        estimateLabel.isHidden = true
    }
}
