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

    @IBOutlet weak var cardTxCheck: UIView!
    @IBOutlet weak var lblTxCheckTitle: UILabel!
    @IBOutlet weak var lblTxCheckHint: UILabel!

    @IBOutlet weak var cardMulti: UIView!
    @IBOutlet weak var lblMultiTitle: UILabel!
    @IBOutlet weak var lblMultiHint: UILabel!

    @IBOutlet weak var cardElectBtc: UIView!
    @IBOutlet weak var lblElectBtcTitle: UILabel!
    @IBOutlet weak var lblElectBtcHint: UILabel!

    @IBOutlet weak var cardElectLiquid: UIView!
    @IBOutlet weak var lblElectLiquidTitle: UILabel!
    @IBOutlet weak var lblElectLiquidHint: UILabel!

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

        setContent()
        setStyle()
        setActions()
        reload()

        view.accessibilityIdentifier = AccessibilityIdentifiers.WalletSettingsScreen.view
        switchTor.accessibilityIdentifier = AccessibilityIdentifiers.WalletSettingsScreen.torSwitch
        btnSave.accessibilityIdentifier = AccessibilityIdentifiers.WalletSettingsScreen.saveBtn
        btnCancel.accessibilityIdentifier = AccessibilityIdentifiers.WalletSettingsScreen.cancelBtn

        cardSPV.isHidden = account?.isSingleSig != true
        cardSPVliquidServer.isHidden = true
    }

    func setContent() {
        title = ""
        lblTitle.text = NSLocalizedString("id_app_settings", comment: "")
        lblHint.text = NSLocalizedString("id_these_settings_apply_for_every", comment: "")

        lblTorTitle.text = NSLocalizedString("id_connect_with_tor", comment: "")
        lblTorHint.text = NSLocalizedString("id_private_but_less_stable", comment: "")
        lblTestnetTitle.text = NSLocalizedString("id_enable_testnet", comment: "")
        lblTestnetHint.text = ""
        lblProxyTitle.text = NSLocalizedString("id_connect_through_a_proxy", comment: "")
        lblProxyHint.text = ""
        fieldProxyIp.placeholder = "Host Ip"

        lblSPVTitle.text = "Backend and Validation"
        lblSPVHint.text = "SPV Validation is currently available for singlesig wallets only."
        lblSPVPersonalNodeTitle.text = "Personal Node"
        lblSPVPersonalNodeHint.text = "Choose the Electrum servers you trust for chain data and SPV"
        lblSPVbtcServer.text = "Bitcoin Electrum Server"
        lblSPVliquidServer.text = "Liquid Electrum Server"
        lblSPVtestnetServer.text = "Testnet Electrum Server"
        fieldSPVbtcServer.placeholder = "Host Ip"
        fieldSPVliquidServer.placeholder = "Host Ip"
        fieldSPVtestnetServer.placeholder = "Host Ip"
        lblTxCheckTitle.text = NSLocalizedString("id_spv_verification", comment: "")
        lblTxCheckHint.text = NSLocalizedString("id_verify_your_transactions_are", comment: "")
        lblMultiTitle.text = NSLocalizedString("id_multiserver_validation", comment: "")
        lblMultiHint.text = NSLocalizedString("id_double_check_spv_with_other", comment: "")
        lblElectBtcTitle.text = NSLocalizedString("id_bitcoin_electrum_backend", comment: "")
        lblElectBtcHint.text = NSLocalizedString("id_choose_the_electrum_servers_you", comment: "")
        lblElectLiquidTitle.text = NSLocalizedString("id_liquid_electrum_backend", comment: "")
        lblElectLiquidHint.text = NSLocalizedString("", comment: "")

        btnCancel.setTitle(NSLocalizedString("id_cancel", comment: ""), for: .normal)
        btnSave.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)

        cardProxyDetail.isHidden = true
    }

    func setStyle() {
        btnCancel.cornerRadius = 4.0
        btnSave.cornerRadius = 4.0
        let fields = [fieldProxyIp, fieldSPVbtcServer, fieldSPVliquidServer, fieldSPVtestnetServer]
        fields.forEach {
            $0?.setLeftPaddingPoints(10.0)
            $0?.setRightPaddingPoints(10.0)
        }

        cardTxCheck.alpha = 0.5
        cardMulti.alpha = 0.5
        cardElectBtc.alpha = 0.5
        cardElectLiquid.alpha = 0.5

        let flexButton = UIBarButtonItem(barButtonSystemItem: UIBarButtonItem.SystemItem.flexibleSpace, target: nil, action: nil)
        let doneButton = UIBarButtonItem(image: UIImage(named: "cancel"),
                                     style: .plain,
                                     target: self,
                                     action: #selector(self.donePressed))

        doneButton.tintColor = UIColor.customGrayLight()
        toolBar.setItems([flexButton, doneButton], animated: true)
    }

    func setActions() {

    }

    @objc func donePressed() {
        dismiss(animated: true, completion: nil)
    }

    func reload() {
        switchTor.setOn(networkSettings["tor"] as? Bool ?? false, animated: true)
        switchProxy.setOn(networkSettings["proxy"] as? Bool ?? false, animated: true)
        var socks5 = networkSettings["socks5_hostname"] as? String ?? ""
        if let port = networkSettings["socks5_port"] as? String {
            socks5 += ":\(port)"
        }
        fieldProxyIp.text = socks5
        switchTestnet.setOn(UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) == true, animated: true)

        switchPSPVPersonalNode.setOn(networkSettings[Constants.personalNodeEnabled] as? Bool ?? false, animated: true)
        cardSPVPersonalNodeDetails.isHidden = !switchPSPVPersonalNode.isOn
        fieldSPVbtcServer.text = networkSettings[Constants.btcElectrumSrv] as? String ?? ""
        fieldSPVliquidServer.text = networkSettings[Constants.liquidElectrumSrv] as? String ?? ""
        fieldSPVtestnetServer.text = networkSettings[Constants.testnetElectrumSrv] as? String ?? ""
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
>>>>>>> 747882b (App Settings: SPV/Personal node card UI/UX)
    }

    @IBAction func switchProxyChange(_ sender: UISwitch) {
        cardProxyDetail.isHidden = !sender.isOn
    }

    @IBAction func switchPSPVPersonalNode(_ sender: UISwitch) {
        cardSPVPersonalNodeDetails.isHidden = !sender.isOn

        if sender.isOn {
            fieldSPVbtcServer.text = Constants.btcElectrumSrvDefaultEndPoint
            fieldSPVliquidServer.text = Constants.liquidElectrumSrvDefaultEndPoint
            fieldSPVtestnetServer.text = Constants.testnetElectrumSrvDefaultEndPoint
        } else {
            fieldSPVbtcServer.text = ""
            fieldSPVliquidServer.text = ""
            fieldSPVtestnetServer.text = ""
        }
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
        let btcElectrumSrv = fieldSPVbtcServer.text ?? ""
        let liquidElectrumSrv = fieldSPVliquidServer.text ?? ""
        let testnetElectrumSrv = fieldSPVtestnetServer.text ?? ""

        networkSettings = [
            "proxy": switchProxy.isOn,
            "tor": switchTor.isOn,
            "socks5_hostname": socks5.split(separator: ":").first ?? "",
            "socks5_port": socks5.split(separator: ":").last ?? "",
            Constants.personalNodeEnabled: switchPSPVPersonalNode.isOn,
            Constants.btcElectrumSrv: btcElectrumSrv,
            Constants.liquidElectrumSrv: liquidElectrumSrv,
            Constants.testnetElectrumSrv: testnetElectrumSrv
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
