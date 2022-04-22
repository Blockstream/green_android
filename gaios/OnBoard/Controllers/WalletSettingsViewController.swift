import UIKit

protocol WalletSettingsViewControllerDelegate: AnyObject {
    func didSet(tor: Bool)
    func didSet(testnet: Bool)
}

class WalletSettingsViewController: KeyboardViewController {

    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var cardTor: UIView!
    @IBOutlet weak var lblTorTitle: UILabel!
    @IBOutlet weak var lblTorHint: UILabel!
    @IBOutlet weak var switchTor: UISwitch!

    @IBOutlet weak var cardAnalytics: UIView!
    @IBOutlet weak var lblAnalyticsTitle: UILabel!
    @IBOutlet weak var lblAnalyticsHint: UILabel!
    @IBOutlet weak var btnAnalytics: UIButton!
    @IBOutlet weak var switchAnalytics: UISwitch!

    @IBOutlet weak var cardProxy: UIView!
    @IBOutlet weak var lblProxyTitle: UILabel!
    @IBOutlet weak var lblProxyHint: UILabel!
    @IBOutlet weak var cardProxyDetail: UIView!
    @IBOutlet weak var switchProxy: UISwitch!
    @IBOutlet weak var fieldProxyIp: UITextField!

    @IBOutlet weak var cardTestnet: UIView!
    @IBOutlet weak var lblTestnetTitle: UILabel!
    @IBOutlet weak var lblTestnetHint: UILabel!
    @IBOutlet weak var switchTestnet: UISwitch!
    @IBOutlet weak var cardSPV: UIView!
    @IBOutlet weak var lblSPVTitle: UILabel!
    @IBOutlet weak var lblSPVHint: UILabel!

    @IBOutlet weak var cardSPVPersonalNode: UIView!
    @IBOutlet weak var lblSPVPersonalNodeTitle: UILabel!
    @IBOutlet weak var lblSPVPersonalNodeHint: UILabel!
    @IBOutlet weak var switchPSPVPersonalNode: UISwitch!
    @IBOutlet weak var cardSPVPersonalNodeDetails: UIView!

    @IBOutlet weak var cardSPVbtcServer: UIView!
    @IBOutlet weak var lblSPVbtcServer: UILabel!
    @IBOutlet weak var fieldSPVbtcServer: UITextField!

    @IBOutlet weak var cardSPVliquidServer: UIView!
    @IBOutlet weak var lblSPVliquidServer: UILabel!
    @IBOutlet weak var fieldSPVliquidServer: UITextField!

    @IBOutlet weak var cardSPVtestnetServer: UIView!
    @IBOutlet weak var lblSPVtestnetServer: UILabel!
    @IBOutlet weak var fieldSPVtestnetServer: UITextField!

    @IBOutlet weak var cardSPVliquidTestnetServer: UIView!
    @IBOutlet weak var lblSPVliquidTestnetServer: UILabel!
    @IBOutlet weak var fieldSPVliquidTestnetServer: UITextField!

    @IBOutlet weak var cardTxCheck: UIView!
    @IBOutlet weak var lblTxCheckTitle: UILabel!
    @IBOutlet weak var lblTxCheckHint: UILabel!
    @IBOutlet weak var switchTxCheck: UISwitch!

    @IBOutlet weak var cardMulti: UIView!
    @IBOutlet weak var lblMultiTitle: UILabel!
    @IBOutlet weak var lblMultiHint: UILabel!

    @IBOutlet weak var toolBar: UIToolbar!

    @IBOutlet weak var btnCancel: UIButton!
    @IBOutlet weak var btnSave: UIButton!

    weak var delegate: WalletSettingsViewControllerDelegate?
    var account: Account?

    private var networkSettings: [String: Any] {
        get {
            UserDefaults.standard.value(forKey: "network_settings") as? [String: Any] ?? [:]
        }
        set {
            UserDefaults.standard.set(newValue, forKey: "network_settings")
            UserDefaults.standard.synchronize()
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        fieldProxyIp.delegate = self
        fieldSPVbtcServer.delegate = self
        fieldSPVliquidServer.delegate = self
        fieldSPVtestnetServer.delegate = self
        fieldSPVliquidTestnetServer.delegate = self

        setContent()
        setStyle()
        reload()

        view.accessibilityIdentifier = AccessibilityIdentifiers.WalletSettingsScreen.view
        switchTor.accessibilityIdentifier = AccessibilityIdentifiers.WalletSettingsScreen.torSwitch
        btnSave.accessibilityIdentifier = AccessibilityIdentifiers.WalletSettingsScreen.saveBtn
        btnCancel.accessibilityIdentifier = AccessibilityIdentifiers.WalletSettingsScreen.cancelBtn
        switchTestnet.accessibilityIdentifier = AccessibilityIdentifiers.WalletSettingsScreen.testnetSwitch
    }

    func setContent() {
        title = ""
        lblTitle.text = NSLocalizedString("id_app_settings", comment: "")
        lblHint.text = NSLocalizedString("id_these_settings_apply_for_every", comment: "")

        lblTorTitle.text = NSLocalizedString("id_connect_with_tor", comment: "")
        lblTorHint.text = NSLocalizedString("id_private_but_less_stable", comment: "")
        lblTestnetTitle.text = NSLocalizedString("id_enable_testnet", comment: "")
        lblTestnetHint.text = ""
        lblAnalyticsTitle.text = "Help Green improve"
        lblAnalyticsHint.text = "Enable anonimous data collection"
        btnAnalytics.setTitle("More info", for: .normal)
        lblProxyTitle.text = NSLocalizedString("id_connect_through_a_proxy", comment: "")
        lblProxyHint.text = ""
        fieldProxyIp.placeholder = NSLocalizedString("id_server_ip_and_port_ipport", comment: "")

        lblSPVTitle.text = NSLocalizedString("id_custom_servers_and_validation", comment: "")
        lblSPVHint.text = NSLocalizedString("id_spv_mode_is_currently_available", comment: "")
        lblSPVPersonalNodeTitle.text = NSLocalizedString("id_personal_electrum_server", comment: "")
        lblSPVPersonalNodeHint.text = NSLocalizedString("id_choose_the_electrum_servers_you", comment: "")
        lblSPVbtcServer.text = NSLocalizedString("id_bitcoin_electrum_server", comment: "")
        lblSPVliquidServer.text = NSLocalizedString("id_liquid_electrum_server", comment: "")
        lblSPVliquidTestnetServer.text = NSLocalizedString("id_liquid_testnet_electrum_server", comment: "")
        lblSPVtestnetServer.text = NSLocalizedString("id_testnet_electrum_server", comment: "")
        fieldSPVbtcServer.placeholder = NSLocalizedString("id_server_ip_and_port_ipport", comment: "")
        fieldSPVliquidServer.placeholder = NSLocalizedString("id_server_ip_and_port_ipport", comment: "")
        fieldSPVtestnetServer.placeholder = NSLocalizedString("id_server_ip_and_port_ipport", comment: "")
        lblTxCheckTitle.text = NSLocalizedString("id_spv_verification", comment: "")
        lblTxCheckHint.text = NSLocalizedString("id_verify_your_bitcoin", comment: "")
        lblMultiTitle.text = NSLocalizedString("id_multiserver_validation", comment: "")
        lblMultiHint.text = NSLocalizedString("id_double_check_spv_with_other", comment: "")

        btnCancel.setTitle(NSLocalizedString("id_cancel", comment: ""), for: .normal)
        btnSave.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)

        fieldSPVbtcServer.placeholder = Constants.btcElectrumSrvDefaultEndPoint
        fieldSPVliquidServer.placeholder = Constants.liquidElectrumSrvDefaultEndPoint
        fieldSPVtestnetServer.placeholder = Constants.testnetElectrumSrvDefaultEndPoint
        fieldSPVliquidTestnetServer.placeholder = Constants.liquidTestnetElectrumSrvDefaultEndPoint
    }

    func setStyle() {
        btnAnalytics.setStyle(.inline)
        btnCancel.cornerRadius = 4.0
        btnSave.cornerRadius = 4.0
        let fields = [fieldProxyIp, fieldSPVbtcServer, fieldSPVliquidServer, fieldSPVtestnetServer, fieldSPVliquidTestnetServer]
        fields.forEach {
            $0?.setLeftPaddingPoints(10.0)
            $0?.setRightPaddingPoints(10.0)
        }
        cardMulti.alpha = 0.5

        let flexButton = UIBarButtonItem(barButtonSystemItem: UIBarButtonItem.SystemItem.flexibleSpace, target: nil, action: nil)
        let doneButton = UIBarButtonItem(image: UIImage(named: "cancel"),
                                     style: .plain,
                                     target: self,
                                     action: #selector(self.donePressed))

        doneButton.tintColor = UIColor.customGrayLight()
        toolBar.setItems([flexButton, doneButton], animated: true)
    }

    @objc func donePressed() {
        dismiss(animated: true, completion: nil)
    }

    func reload() {
        switchTor.setOn(networkSettings["tor"] as? Bool ?? false, animated: true)
        switchProxy.setOn(networkSettings["proxy"] as? Bool ?? false, animated: true)
        if let socks5 = networkSettings["socks5_hostname"] as? String,
           let port = networkSettings["socks5_port"] as? String,
           !socks5.isEmpty && !port.isEmpty {
            fieldProxyIp.text = "\(socks5):\(port)"
        }

        switchTestnet.setOn(UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) == true, animated: true)
        switchTxCheck.setOn(networkSettings[Constants.spvEnabled] as? Bool ?? false, animated: true)
        switchPSPVPersonalNode.setOn(networkSettings[Constants.personalNodeEnabled] as? Bool ?? false, animated: true)

        if let uri = networkSettings[Constants.btcElectrumSrv] as? String, !uri.isEmpty {
            fieldSPVbtcServer.text = uri
        }
        if let uri = networkSettings[Constants.liquidElectrumSrv] as? String, !uri.isEmpty {
            fieldSPVliquidServer.text = uri
        }
        if let uri = networkSettings[Constants.testnetElectrumSrv] as? String, !uri.isEmpty {
            fieldSPVtestnetServer.text = uri
        }
        if let uri = networkSettings[Constants.liquidTestnetElectrumSrv] as? String, !uri.isEmpty {
            fieldSPVliquidTestnetServer.text = uri
        }

        switchPSPVPersonalNode(switchPSPVPersonalNode)
        switchProxyChange(switchProxy)
    }

    override func keyboardWillShow(notification: Notification) {
        super.keyboardWillShow(notification: notification)

        guard let userInfo = notification.userInfo else { return }
        // swiftlint:disable force_cast
        var keyboardFrame: CGRect = (userInfo[UIResponder.keyboardFrameBeginUserInfoKey] as! NSValue).cgRectValue
        keyboardFrame = self.view.convert(keyboardFrame, from: nil)

        var contentInset: UIEdgeInsets = self.scrollView.contentInset
        contentInset.bottom = keyboardFrame.size.height + 20
        scrollView.contentInset = contentInset
    }

    override func keyboardWillHide(notification: Notification) {
        let contentInset: UIEdgeInsets = UIEdgeInsets.zero
        scrollView.contentInset = contentInset
        super.keyboardWillHide(notification: notification)
    }

    @IBAction func switchProxyChange(_ sender: UISwitch) {
        cardProxyDetail.isHidden = !sender.isOn
    }

    @IBAction func switchPSPVPersonalNode(_ sender: UISwitch) {
        cardSPVPersonalNodeDetails.isHidden = !sender.isOn
        cardSPVtestnetServer.isHidden = !switchTestnet.isOn
        cardSPVliquidTestnetServer.isHidden = !switchTestnet.isOn
    }

    @IBAction func switchTestnet(_ sender: Any) {
        cardSPVtestnetServer.isHidden = !switchTestnet.isOn
        cardSPVliquidTestnetServer.isHidden = !switchTestnet.isOn
    }

    @IBAction func btnCancel(_ sender: Any) {
        dismiss(animated: true, completion: nil)
    }

    @IBAction func btnSave(_ sender: Any) {
        let socks5 = fieldProxyIp.text ?? ""
        if switchProxy.isOn && socks5.isEmpty {
            showAlert(title: NSLocalizedString("id_warning", comment: ""),
                      message: NSLocalizedString("id_socks5_proxy_and_port_must_be", comment: ""))
            return
        }
        networkSettings = [
            "proxy": switchProxy.isOn,
            "tor": switchTor.isOn,
            "socks5_hostname": String(socks5.split(separator: ":").first ?? ""),
            "socks5_port": String(socks5.split(separator: ":").last ?? ""),
            Constants.spvEnabled: switchTxCheck.isOn,
            Constants.personalNodeEnabled: switchPSPVPersonalNode.isOn,
            Constants.btcElectrumSrv: fieldSPVbtcServer.text ?? "",
            Constants.liquidElectrumSrv: fieldSPVliquidServer.text ?? "",
            Constants.testnetElectrumSrv: fieldSPVtestnetServer.text ?? "",
            Constants.liquidTestnetElectrumSrv: fieldSPVliquidTestnetServer.text ?? ""
        ]
        UserDefaults.standard.set(switchTestnet.isOn, forKey: AppStorage.testnetIsVisible)
        delegate?.didSet(tor: switchTor.isOn)
        delegate?.didSet(testnet: switchTestnet.isOn)
        dismiss(animated: true, completion: nil)
    }

}

extension WalletSettingsViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        self.view.endEditing(true)
        return false
    }
}
