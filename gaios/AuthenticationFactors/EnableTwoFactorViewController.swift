import Foundation
import UIKit
import PromiseKit

class EnableTwoFactorViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {

    @IBOutlet weak var tableview: UITableView!
    @IBOutlet weak var walletButton: UIButton!
    private var connected = true
    private var updateToken: NSObjectProtocol?

    struct FactorItem {
        var name: String
        var image: UIImage
        var enabled: Bool
        var type: TwoFactorType
    }
    fileprivate var factors = [FactorItem]()
    var isHiddenWalletButton: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_twofactor_authentication", comment: "")
        walletButton.setTitle(NSLocalizedString("id_go_to_wallet", comment: ""), for: .normal)
        walletButton.isHidden = isHiddenWalletButton
        navigationItem.setHidesBackButton(!isHiddenWalletButton, animated: false)

        self.tableview.rowHeight = 70
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateToken = NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: EventType.Network.rawValue), object: nil, queue: .main, using: updateConnection)
        reloadData()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = updateToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell =
            tableview.dequeueReusableCell(withIdentifier: "cell",
                                          for: indexPath as IndexPath)
        cell.textLabel?.text = self.factors[indexPath.row].name
        cell.imageView?.image = self.factors[indexPath.row].image
        let uiswitch = UISwitch()
        uiswitch.isOn = self.factors[indexPath.row].enabled
        uiswitch.isUserInteractionEnabled = false
        cell.accessoryView = uiswitch

        return cell
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.factors.count
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let selectedFactor: FactorItem = self.factors[indexPath.row]
        if selectedFactor.enabled {
            disable(selectedFactor.type)
            return
        }
        switch selectedFactor.type {
        case .email:
            self.performSegue(withIdentifier: "email", sender: nil)
        case .sms:
            self.performSegue(withIdentifier: "phone", sender: "sms")
        case .phone:
            self.performSegue(withIdentifier: "phone", sender: "call")
        case .gauth:
            self.performSegue(withIdentifier: "gauth", sender: nil)
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        walletButton.updateGradientLayerFrame()
    }

    func updateConnection(_ notification: Notification) {
        let connected = notification.userInfo?["connected"] as? Bool
        self.connected = connected ?? false
    }

    func reloadData() {
        let dataTwoFactorConfig = try? SessionsManager.current?.getTwoFactorConfig()
        guard dataTwoFactorConfig != nil else { return }
        guard let twoFactorConfig = try? JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig!, options: [])) else { return }
        factors.removeAll()
        factors.append(FactorItem(name: NSLocalizedString("id_email", comment: ""), image: UIImage.init(named: "2fa_email")!, enabled: twoFactorConfig.email.enabled && twoFactorConfig.email.confirmed, type: .email))
        factors.append(FactorItem(name: NSLocalizedString("id_sms", comment: ""), image: UIImage.init(named: "2fa_sms")!, enabled: twoFactorConfig.sms.enabled && twoFactorConfig.sms.confirmed, type: .sms))
        factors.append(FactorItem(name: NSLocalizedString("id_call", comment: ""), image: UIImage.init(named: "2fa_call")!, enabled: twoFactorConfig.phone.enabled && twoFactorConfig.phone.confirmed, type: .phone))
        factors.append(FactorItem(name: NSLocalizedString("id_authenticator_app", comment: ""), image: UIImage.init(named: "2fa_google")!, enabled: twoFactorConfig.gauth.enabled && twoFactorConfig.gauth.confirmed, type: .gauth))
        tableview.reloadData()
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let nextController = segue.destination as? SetPhoneViewController {
            if sender as? String == "sms" {
                nextController.sms = true
            } else {
                nextController.phoneCall = true
            }
        }
    }

    @IBAction func walletButtonClick(_ sender: UIButton) {
        getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
    }

    func disable(_ type: TwoFactorType) {
        let bgq = DispatchQueue.global(qos: .background)
        guard let session = SessionsManager.current else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap {
            TwoFactorConfigItem(enabled: false, confirmed: false, data: "")
        }.compactMap(on: bgq) { config in
            try JSONSerialization.jsonObject(with: JSONEncoder().encode(config), options: .allowFragments) as? [String: Any]
        }.then(on: bgq) { details in
            try session.changeSettingsTwoFactor(method: type.rawValue, details: details).resolve(connected: { self.connected })
        }.then(on: bgq) { _ in
            session.loadTwoFactorConfig()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.reloadData()
        }.catch { error in
            if let twofaError = error as? TwoFactorCallError {
                switch twofaError {
                case .failure(let localizedDescription), .cancel(let localizedDescription):
                    DropAlert().error(message: localizedDescription)
                }
            } else {
                DropAlert().error(message: error.localizedDescription)
            }
        }
    }
}
