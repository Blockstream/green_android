import Foundation
import UIKit

class NetworkSelectionSettings: KeyboardViewController, UITextFieldDelegate, UIScrollViewDelegate {
    @IBOutlet var content: NetworkSelectionSettingsView!
    var currentNetworkSelection: String? = nil
    var onSave: (() -> Void)? = nil

    override func viewDidLoad() {
        super.viewDidLoad()

        content.scrollView.delegate = self
        content.socks5Hostname.delegate = self
        content.socks5Port.delegate = self

        content.bitcoin.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector (handleBitcoinSelection(_:))))
        content.testnet.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector (handleTestnetSelection(_:))))
        #if DEBUG
        content.localtest.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector (handleLocaltestSelection(_:))))
        content.regtest.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector (handleRegtestSelection(_:))))
        #else
        content.localtest.isHidden = true
        content.regtest.isHidden = true
        #endif

        content.socks5Hostname.attributedPlaceholder = NSAttributedString(string: "Socks5 Hostname",
                                                               attributes: [NSAttributedStringKey.foregroundColor: UIColor.customTitaniumLight()])
        content.socks5Port.attributedPlaceholder = NSAttributedString(string: "Socks5 Port",
                                                                 attributes: [NSAttributedStringKey.foregroundColor: UIColor.customTitaniumLight()])
        content.titleLabel.text = NSLocalizedString("id_choose_your_network", comment: "")
        content.proxyLabel.text = NSLocalizedString("id_advanced_network_settings", comment: "")
        content.proxySettingsLabel.text = NSLocalizedString("id_proxy_settings", comment: "")
        content.socks5Hostname.text = NSLocalizedString("id_socks5_hostname", comment: "")
        content.socks5Port.text = NSLocalizedString("id_socks5_port", comment: "")
        content.torLabel.text = NSLocalizedString("id_connect_with_tor", comment: "")
        content.saveButton.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)
        content.saveButton.addTarget(self, action: #selector(click), for: .touchUpInside)
        content.saveButton.setGradient(true)
        content.proxySwitch.addTarget(self, action: #selector(click), for: .valueChanged)
        content.cancelButton.addTarget(self, action: #selector(back), for: .touchUpInside)
        content.socks5Hostname.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: content.socks5Hostname.frame.height))
        content.socks5Port.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: content.socks5Port.frame.height))
        content.socks5Hostname.leftViewMode = .always
        content.socks5Port.leftViewMode = .always
        content.saveButton.setGradient(true)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        setupView()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        content.saveButton.removeTarget(self, action: #selector(click), for: .touchUpInside)
        content.proxySwitch.removeTarget(self, action: #selector(click), for: .valueChanged)
        content.cancelButton.removeTarget(self, action: #selector(back), for: .touchUpInside)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        content.saveButton.updateGradientLayerFrame()
    }

    private func setBorderColor(_ view: UIView, _ color: UIColor = UIColor.customMatrixGreen()) {
        view.borderColor = color
    }

    private func resetBorderColor() {
        let color = UIColor.customTitaniumLight()
        setBorderColor(content.bitcoin, color)
        setBorderColor(content.testnet, color)
        setBorderColor(content.localtest, color)
        setBorderColor(content.regtest, color)
    }

    private func handleSelection(_ view: UIView, _ title: String) {
        resetBorderColor()
        setBorderColor(view)
        currentNetworkSelection = title
    }

    @objc func handleBitcoinSelection(_ sender: UITapGestureRecognizer) {
        handleSelection(content.bitcoin, "Mainnet")
    }

    @objc func handleTestnetSelection(_ sender: UITapGestureRecognizer) {
        handleSelection(content.testnet, "Testnet")
    }

    @objc func handleLocaltestSelection(_ sender: UITapGestureRecognizer) {
        handleSelection(content.localtest, "Localtest")
    }

    @objc func handleRegtestSelection(_ sender: UITapGestureRecognizer) {
        handleSelection(content.regtest, "Regtest")
    }

    @objc override func keyboardWillShow(notification: NSNotification) {
        super.keyboardWillShow(notification: notification)

        let userInfo = notification.userInfo
        let keyboardFrame = userInfo?[UIKeyboardFrameEndUserInfoKey] as! CGRect
        let contentInset = UIEdgeInsetsMake(0.0, 0.0, keyboardFrame.height + content.socks5Port.frame.height, 0.0)
        content.scrollView.contentInset = contentInset
        content.scrollView.scrollIndicatorInsets = contentInset
    }

    @objc override func keyboardWillHide(notification: NSNotification) {
        let contentInset = UIEdgeInsets.zero
        content.scrollView.contentInset = contentInset
        content.scrollView.scrollIndicatorInsets = contentInset

        super.keyboardWillHide(notification: notification)
    }

    @objc func back(_ sender: UIButton?) {
        dismiss(animated: true, completion: nil)
    }

    @objc func click(_ sender: Any?) {
        if let _ = sender as? UIButton {
            UserDefaults.standard.set(["network": currentNetworkSelection!, "proxy": content.proxySwitch.isOn, "tor": content.torSwitch.isOn, "socks5_hostname": content.socks5Hostname.text ?? "", "socks5_port": content.socks5Port.text ?? ""], forKey: "network_settings")
            onSave!()
            dismiss(animated: true, completion: nil)
        } else if let switcher = sender as? UISwitch {
            content.proxySettings.isHidden = !switcher.isOn
            content.torLabel.isHidden = !switcher.isOn
            content.torSwitch.isHidden = !switcher.isOn
        }
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        view.endEditing(true)
        return true
    }

    func setupView() {
        view.backgroundColor = UIColor.black.withAlphaComponent(0.4)

        let defaults = getUserNetworkSettings()
        if defaults == nil {
            handleSelection(content.bitcoin, "Mainnet")
        } else {
            let networkName = defaults!["network"] as! String
            switch networkName {
            case "Mainnet":
                handleSelection(content.bitcoin, networkName)
            case "Testnet":
                handleSelection(content.testnet, networkName)
            case "Localtest":
                handleSelection(content.localtest, networkName)
            case "Regtest":
                handleSelection(content.regtest, networkName)
            default:
                precondition(false)
            }
        }

        content.proxySettings.isHidden = !(defaults?["proxy"] as? Bool ?? false)
        content.proxySwitch.isOn = defaults?["proxy"] as? Bool ?? false
        content.socks5Hostname.text = defaults?["socks5_hostname"] as? String ?? ""
        content.socks5Port.text = defaults?["socks5_port"] as? String ?? ""
        content.torSwitch.isOn = defaults?["tor"] as? Bool ?? false
        content.torLabel.isHidden = !content.proxySwitch.isOn
        content.torSwitch.isHidden = !content.proxySwitch.isOn
        content.saveButton.setGradient(true)
    }
}

@IBDesignable
class NetworkSelectionSettingsView: UIView {
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var bitcoin: UIView!
    @IBOutlet weak var testnet: UIView!
    @IBOutlet weak var localtest: UIView!
    @IBOutlet weak var regtest: UIView!
    @IBOutlet weak var proxySettings: UIView!
    @IBOutlet weak var proxyLabel: UILabel!
    @IBOutlet weak var proxySwitch: UISwitch!
    @IBOutlet weak var saveButton: UIButton!
    @IBOutlet weak var torSwitch: UISwitch!
    @IBOutlet weak var torLabel: UILabel!
    @IBOutlet weak var socks5Hostname: UITextField!
    @IBOutlet weak var socks5Port: UITextField!
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var proxySettingsLabel: UILabel!
    @IBOutlet weak var cancelButton: UIButton!

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
        saveButton.updateGradientLayerFrame()
    }
}
