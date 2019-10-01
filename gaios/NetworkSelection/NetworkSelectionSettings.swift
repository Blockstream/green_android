import Foundation
import UIKit

class NetworkSelectionSettings: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var cancelButton: UIButton!
    @IBOutlet weak var cancelButtonHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var swipeIndicator: UIView!
    @IBOutlet weak var dismissButton: UIButton!
    @IBOutlet weak var dismissButtonHeightConstraint: NSLayoutConstraint!

    var onSelection: (() -> Void)?
    var isLanding: Bool = true

    private var selectedNetwork = getNetwork()
    private var networks = [GdkNetwork]()

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.dataSource = self
        tableView.delegate = self
        tableView.separatorStyle = .none
        tableView.estimatedRowHeight = 85
        tableView.rowHeight = UITableView.automaticDimension
        networks = getGdkNetworks().sorted { $0.name < $1.name }
        titleLabel.text = NSLocalizedString("id_choose_your_network", comment: "")
        configureHeader()
        if isLanding {
            configureFooter()
        } else {
            let downSwipe = UISwipeGestureRecognizer(target: self, action: #selector(dismissModal))
            downSwipe.direction = .down
            view.addGestureRecognizer(downSwipe)
        }
        configureCancelButton()
    }

    func configureHeader() {
        swipeIndicator.isHidden = isLanding
        dismissButton.isHidden = !isLanding
        dismissButtonHeightConstraint.constant = isLanding ? 18 : 0
        dismissButton.addTarget(self, action: #selector(dismissModal), for: .touchUpInside)
    }

    func configureFooter() {
        let nib = Bundle.main.loadNibNamed("NetworkSelectionSettingsView", owner: self, options: nil)
        tableView.tableFooterView = nib?.first as? NetworkSelectionSettingsView
        tableView.keyboardDismissMode = UIScrollView.KeyboardDismissMode.onDrag
        guard let content = tableView.tableFooterView as? NetworkSelectionSettingsView else { return }
        let attributes = [NSAttributedString.Key.foregroundColor: UIColor.customTitaniumLight()]
        content.socks5Hostname.attributedPlaceholder = NSAttributedString(string: "Socks5 Hostname",
                                                                          attributes: attributes)
        content.socks5Port.attributedPlaceholder = NSAttributedString(string: "Socks5 Port",
                                                                     attributes: attributes)
        content.proxyLabel.text = NSLocalizedString("id_connect_through_a_proxy", comment: "")
        content.proxySettingsLabel.text = NSLocalizedString("id_proxy_settings", comment: "")
        content.socks5Hostname.text = NSLocalizedString("id_socks5_hostname", comment: "")
        content.socks5Port.text = NSLocalizedString("id_socks5_port", comment: "")
        content.torLabel.text = NSLocalizedString("id_connect_with_tor", comment: "")
        content.saveButton.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)
        content.saveButton.addTarget(self, action: #selector(saveAndDismiss), for: .touchUpInside)
        content.proxySwitch.addTarget(self, action: #selector(changeProxy), for: .valueChanged)
        content.socks5Hostname.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: content.socks5Hostname.frame.height))
        content.socks5Port.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 10, height: content.socks5Port.frame.height))
        content.socks5Hostname.leftViewMode = .always
        content.socks5Port.leftViewMode = .always
        let defaults = getUserNetworkSettings()
        content.proxySettings.isHidden = !(defaults["proxy"] as? Bool ?? false)
        content.proxySwitch.isOn = defaults["proxy"] as? Bool ?? false
        content.socks5Hostname.text = defaults["socks5_hostname"] as? String ?? ""
        content.socks5Port.text = defaults["socks5_port"] as? String ?? ""
        content.torSwitch.isOn = defaults["tor"] as? Bool ?? false
    }

    func configureCancelButton() {
        cancelButton.isHidden = isLanding
        cancelButtonHeightConstraint.constant = isLanding ? 0 : 42
        cancelButton.setTitle(NSLocalizedString("id_cancel", comment: ""), for: .normal)
        cancelButton.addTarget(self, action: #selector(dismissModal), for: .touchUpInside)
        self.view.layoutIfNeeded()
    }

    override func viewDidLayoutSubviews() {
        view.roundCorners([.topRight, .topLeft], radius: 12)
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        view.endEditing(true)
        return true
    }

    @objc func changeProxy(_ sender: UISwitch) {
        guard let content = tableView.tableFooterView as? NetworkSelectionSettingsView else { return }
        content.proxySettings.isHidden = !sender.isOn
    }

    @objc func dismissModal() {
        self.dismiss(animated: true, completion: nil)
    }

    @objc func saveAndDismiss() {
        let defaults = UserDefaults.standard
        var settings = getUserNetworkSettings()
        if !isLanding {
            settings["network"] = selectedNetwork
        } else {
            guard let content = tableView.tableFooterView as? NetworkSelectionSettingsView else { return }
            let socks5Hostname = content.socks5Hostname.text ?? ""
            let socks5Port = content.socks5Port.text ?? ""
            var errorMessage = ""
            if content.proxySwitch.isOn && ( socks5Hostname.isEmpty || socks5Port.isEmpty ) {
                errorMessage = NSLocalizedString("id_socks5_proxy_and_port_must_be", comment: "")
            } else if content.torSwitch.isOn && !content.proxySwitch.isOn {
                errorMessage = NSLocalizedString("id_please_set_and_enable_socks5", comment: "")
            } else {
                settings = ["network": selectedNetwork,
                            "proxy": content.proxySwitch.isOn,
                            "tor": content.torSwitch.isOn,
                            "socks5_hostname": socks5Hostname,
                            "socks5_port": socks5Port]
            }
            if !errorMessage.isEmpty {
                let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: errorMessage, preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: NSLocalizedString("id_ok", comment: ""), style: .default) { _ in })
                present(alert, animated: true, completion: nil)
                return
            }
        }

        defaults.set(settings, forKey: "network_settings")
        defaults.synchronize()

        if let onSelection = onSelection {
            onSelection()
        }
        self.dismiss(animated: true, completion: nil)
    }
}

extension NetworkSelectionSettings: UITableViewDataSource, UITableViewDelegate {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return networks.count
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, estimatedHeightForRowAt indexPath: IndexPath) -> CGFloat {
        return 84
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "networkSelectionCell",
                                                       for: indexPath as IndexPath) as? NetworkSelectionTableCell else { return UITableViewCell() }
        let network = networks[indexPath.row]
        cell.configure(with: network, selected: network.network == selectedNetwork)
        cell.setNeedsLayout()
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let network = networks[indexPath.row]
        if selectedNetwork != network.network {
            tableView.reloadData()
            selectedNetwork = network.network
            if !isLanding {
                saveAndDismiss()
            }
        }
    }
}
