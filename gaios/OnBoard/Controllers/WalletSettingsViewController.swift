import UIKit

class WalletSettingsViewController: UIViewController {

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

        setContent()
        setStyle()
        setActions()
        reload()
        hideKeyboardWhenTappedAround()
    }

    func setContent() {
        title = ""
        lblTitle.text = NSLocalizedString("id_connection_amp_validation", comment: "")
        lblHint.text = NSLocalizedString("id_these_settings_apply_for_every", comment: "")

        lblTorTitle.text = NSLocalizedString("id_connect_with_tor", comment: "")
        lblTorHint.text = NSLocalizedString("id_private_but_less_stable", comment: "")
        lblProxyTitle.text = NSLocalizedString("id_connect_through_a_proxy", comment: "")
        lblProxyHint.text = ""
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
        fieldProxyIp.setLeftPaddingPoints(10.0)
        fieldProxyIp.setRightPaddingPoints(10.0)

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
    }

    @IBAction func switchProxyChange(_ sender: UISwitch) {
        cardProxyDetail.isHidden = !sender.isOn
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
            "socks5_hostname": socks5.split(separator: ":").first ?? "",
            "socks5_port": socks5.split(separator: ":").last ?? ""]
        dismiss(animated: true, completion: nil)
    }

}
