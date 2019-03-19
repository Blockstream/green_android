import Foundation
import UIKit
import NVActivityIndicatorView
import PromiseKit

class ReceiveBtcViewController: KeyboardViewController {

    @IBOutlet var content: ReceiveBtcView!
    var wallet: WalletItem?
    var selectedType = TransactionType.BTC
    var gestureTap: UITapGestureRecognizer?

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_receive", comment: "")
        tabBarController?.tabBar.isHidden = true
        gestureTap = UITapGestureRecognizer(target: self, action: #selector(self.copyToClipboard))
        content.amountTextfield.attributedPlaceholder = NSAttributedString(string: "0.00",
                                                             attributes: [NSAttributedStringKey.foregroundColor: UIColor.white])
        content.walletQRCode.isUserInteractionEnabled = true
        content.walletAddressLabel.isUserInteractionEnabled = true
        content.amountLabel.text = NSLocalizedString("id_amount", comment: "")
        content.shareButton.setTitle(NSLocalizedString("id_share_address", comment: ""), for: .normal)
        content.shareButton.setGradient(true)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        NotificationCenter.default.addObserver(self, selector: #selector(self.newAddress(_:)), name: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil)
        content.walletQRCode.removeGestureRecognizer(gestureTap!)
        content.walletAddressLabel.removeGestureRecognizer(gestureTap!)
        content.amountTextfield.addTarget(self, action: #selector(textFieldDidChange(_:)), for: .editingChanged)
        content.fiatSwitchButton.addTarget(self, action: #selector(fiatSwitchButtonClick(_:)), for: .touchUpInside)
        content.shareButton.addTarget(self, action: #selector(shareButtonClicked(_:)), for: .touchUpInside)
        refresh()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name(rawValue: EventType.AddressChanged.rawValue), object: nil)
        guard let _ = gestureTap else { return }
        content.walletQRCode.removeGestureRecognizer(gestureTap!)
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
                self.refresh()
            }
        }
    }

    func refresh() {
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
            let uri = self.getSatoshi() == 0 ? address : Bip21Helper.btcURIforAmount(address: address, amount: self.getBTC())
            UIPasteboard.general.string = uri
            Toast.show(NSLocalizedString("id_address_copied_to_clipboard", comment: ""), timeout: Toast.SHORT)
        }.catch { _ in }
    }

    @objc func fiatSwitchButtonClick(_ sender: Any) {
        let satoshi = getSatoshi()
        guard let settings = getGAService().getSettings() else { return }
        let res = try? getSession().convertAmount(input: ["satoshi": satoshi])
        guard let _ = res, let data = res! else { return }
        if (selectedType == TransactionType.BTC) {
            selectedType = TransactionType.FIAT
            content.amountTextfield.text = String(format: "%@", data["fiat"] as! String)
        } else {
            selectedType = TransactionType.BTC
            guard let amount = data[settings.denomination.rawValue] as? String else { return }
            content.amountTextfield.text = String(format: "%f", Double(amount) ?? 0)
        }
        setButton()
        updateEstimate()
    }

    func setButton() {
        guard let settings = getGAService().getSettings() else { return }
        if (selectedType == TransactionType.BTC) {
            content.fiatSwitchButton.setTitle(settings.denomination.toString(), for: UIControlState.normal)
            content.fiatSwitchButton.backgroundColor = UIColor.customMatrixGreen()
            content.fiatSwitchButton.setTitleColor(UIColor.white, for: UIControlState.normal)
        } else {
            content.fiatSwitchButton.setTitle(settings.getCurrency(), for: UIControlState.normal)
            content.fiatSwitchButton.backgroundColor = UIColor.clear
            content.fiatSwitchButton.setTitleColor(UIColor.white, for: UIControlState.normal)
        }
    }

    func updateEstimate() {
        let satoshi = getSatoshi()
        if (selectedType == TransactionType.BTC) {
            content.estimateLabel.text = "≈ " + String.toFiat(satoshi: satoshi)
        } else {
            content.estimateLabel.text = "≈ " + String.toBtc(satoshi: satoshi)
        }
        updateQRCode()
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
            if (self.getSatoshi() == 0) {
                uri = Bip21Helper.btcURIforAddress(address: address)
                self.content.walletAddressLabel.text = address
            } else {
                uri = Bip21Helper.btcURIforAmount(address: address, amount: self.getBTC())
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
        updateEstimate()
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
            let uri = self.getSatoshi() == 0 ? address : Bip21Helper.btcURIforAmount(address: address, amount: self.getBTC())
            let activityViewController = UIActivityViewController(activityItems: [uri], applicationActivities: nil)
            activityViewController.popoverPresentationController?.sourceView = self.view
            self.present(activityViewController, animated: true, completion: nil)
        }.catch { _ in }
    }

    func getSatoshi() -> UInt64 {
        guard let amount = content.amountTextfield.text else { return 0 }
        if (amount.isEmpty || Double(amount) == nil) {
            return 0
        }
        if (selectedType == TransactionType.BTC) {
            return String.toSatoshi(amount: amount)
        } else {
            return String.toSatoshi(fiat: amount)
        }
    }

    func getBTC() -> Double {
       return Double(getSatoshi()) / 100000000
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
}
