import Foundation
import UIKit

class NetworkSelectionSettings: UITableViewController {

    var onSave: (() -> Void)?
    var saveTitle = NSLocalizedString("id_save", comment: "")
    private var selectedNetwork = getNetwork()
    private var networks = [GdkNetwork]()

    override func viewDidLoad() {
        super.viewDidLoad()
        let nib = Bundle.main.loadNibNamed("NetworkSelectionSettingsView", owner: self, options: nil)
        tableView.tableFooterView = nib?.first as? NetworkSelectionSettingsView
        tableView.keyboardDismissMode = UIScrollView.KeyboardDismissMode.onDrag
        tableView.estimatedRowHeight = 85
        tableView.rowHeight = UITableView.automaticDimension
        networks = getGdkNetworks().sorted { $0.name < $1.name }
        loadHeader()
        loadFooter()
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return networks.count
    }

    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "networkSelectionCell", for: indexPath as IndexPath) as? NetworkSelectionTableCell else { return UITableViewCell() }
        let network = networks[indexPath.row]
        cell.configure(with: network, selected: network.network == selectedNetwork)
        cell.setNeedsLayout()
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let network = networks[indexPath.row]
        selectedNetwork = network.network
        tableView.reloadData()
    }

    @objc func save(_ sender: UIButton) {
        guard let content = tableView.tableFooterView as? NetworkSelectionSettingsView else { return }
        let socks5Hostname = content.socks5Hostname.text ?? ""
        let socks5Port = content.socks5Port.text ?? ""
        var errorMessage = ""
        if content.proxySwitch.isOn && ( socks5Hostname.isEmpty || socks5Port.isEmpty ) {
            errorMessage = NSLocalizedString("id_socks5_proxy_and_port_must_be", comment: "")
        } else if content.torSwitch.isOn && !content.proxySwitch.isOn {
            errorMessage = NSLocalizedString("id_please_set_and_enable_socks5", comment: "")
        } else {
            // save network setup
            UserDefaults.standard.set(["network": selectedNetwork, "proxy": content.proxySwitch.isOn, "tor": content.torSwitch.isOn, "socks5_hostname": socks5Hostname, "socks5_port": socks5Port], forKey: "network_settings")
            onSave!()
            dismissModal()
            return
        }
        // show warning alert
        let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: errorMessage, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_ok", comment: ""), style: .default) { _ in })
        present(alert, animated: true, completion: nil)
    }

    @objc func changeProxy(_ sender: UISwitch) {
        guard let content = tableView.tableFooterView as? NetworkSelectionSettingsView else { return }
        content.proxySettings.isHidden = !sender.isOn
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        view.endEditing(true)
        return true
    }

    func loadHeader() {
        guard let header = tableView.tableHeaderView as? NetworkSelectionHeaderView else { return }
        header.titleLabel.text = NSLocalizedString("id_choose_your_network", comment: "")
        header.cancelButton.addTarget(self, action: #selector(NetworkSelectionSettings.dismissModal), for: .touchUpInside)
    }

    @objc func dismissModal() {
        self.dismiss(animated: true, completion: nil)
    }

    func loadFooter() {
        guard let content = tableView.tableFooterView as? NetworkSelectionSettingsView else { return }
        content.socks5Hostname.attributedPlaceholder = NSAttributedString(string: "Socks5 Hostname",
                                                                          attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
        content.socks5Port.attributedPlaceholder = NSAttributedString(string: "Socks5 Port",
                                                                      attributes: [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()])
        content.proxyLabel.text = NSLocalizedString("id_connect_through_a_proxy", comment: "")
        content.proxySettingsLabel.text = NSLocalizedString("id_proxy_settings", comment: "")
        content.socks5Hostname.text = NSLocalizedString("id_socks5_hostname", comment: "")
        content.socks5Port.text = NSLocalizedString("id_socks5_port", comment: "")
        content.torLabel.text = NSLocalizedString("id_connect_with_tor", comment: "")
        content.saveButton.setTitle(saveTitle, for: .normal)
        content.saveButton.addTarget(self, action: #selector(save), for: .touchUpInside)
        content.proxySwitch.addTarget(self, action: #selector(changeProxy), for: .valueChanged)
        content.socks5Hostname.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: content.socks5Hostname.frame.height))
        content.socks5Port.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: content.socks5Port.frame.height))
        content.socks5Hostname.leftViewMode = .always
        content.socks5Port.leftViewMode = .always

        let defaults = getUserNetworkSettings()
        content.proxySettings.isHidden = !(defaults?["proxy"] as? Bool ?? false)
        content.proxySwitch.isOn = defaults?["proxy"] as? Bool ?? false
        content.socks5Hostname.text = defaults?["socks5_hostname"] as? String ?? ""
        content.socks5Port.text = defaults?["socks5_port"] as? String ?? ""
        content.torSwitch.isOn = defaults?["tor"] as? Bool ?? false
    }
}

class NetworkSelectionHeaderView: UIView {
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var cancelButton: UIButton!
}

class NetworkSelectionSettingsView: UIView {
    @IBOutlet weak var proxySettings: UIView!
    @IBOutlet weak var proxyLabel: UILabel!
    @IBOutlet weak var proxySwitch: UISwitch!
    @IBOutlet weak var saveButton: UIButton!
    @IBOutlet weak var torSwitch: UISwitch!
    @IBOutlet weak var torLabel: UILabel!
    @IBOutlet weak var socks5Hostname: UITextField!
    @IBOutlet weak var socks5Port: UITextField!
    @IBOutlet weak var proxySettingsLabel: UILabel!

    override func layoutSubviews() {
        super.layoutSubviews()
    }
}
