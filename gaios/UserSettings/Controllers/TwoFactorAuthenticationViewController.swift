import UIKit
import PromiseKit

class TwoFactorAuthenticationViewController: UIViewController {

    @IBOutlet weak var lblEnable2faTitle: UILabel!
    @IBOutlet weak var lblEnable2faHint: UILabel!
    @IBOutlet weak var lbl2faMethods: UILabel!
    @IBOutlet weak var tableView2faMethods: UITableView!

    fileprivate var factors = [FactorItem]()
    private var connected = true
    private var updateToken: NSObjectProtocol?

    override func viewDidLoad() {
        super.viewDidLoad()

        title = "2FA"
        setContent()
        setStyle()
    }

    func setContent() {
        lblEnable2faTitle.text = "Enable 2FA to protect your wallet from unauthorized transactions or changes to critical security settings"
        lblEnable2faHint.text = "TIP: We reccomend you enable more than one 2FA method. If you only set up one 2FA method and then lose it, you'll have to wait at least one year until 2FA expires"
        lbl2faMethods.text = "2FA Methods"
    }

    func setStyle() {
        lblEnable2faTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        lblEnable2faHint.font = UIFont.systemFont(ofSize: 16.0, weight: .regular)
        lblEnable2faTitle.textColor = .white
        lblEnable2faHint.textColor = UIColor.customGrayLight()
        lbl2faMethods.font = UIFont.systemFont(ofSize: 20.0, weight: .heavy)
        lbl2faMethods.textColor = .white
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

    func reloadData() {
        let dataTwoFactorConfig = try? getSession().getTwoFactorConfig()
        guard dataTwoFactorConfig != nil else { return }
        guard let twoFactorConfig = try? JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig!, options: [])) else { return }
        factors.removeAll()
        factors.append(FactorItem(name: NSLocalizedString("id_email", comment: ""), enabled: twoFactorConfig.email.enabled && twoFactorConfig.email.confirmed, type: .email))
        factors.append(FactorItem(name: NSLocalizedString("id_sms", comment: ""), enabled: twoFactorConfig.sms.enabled && twoFactorConfig.sms.confirmed, type: .sms))
        factors.append(FactorItem(name: NSLocalizedString("id_call", comment: ""), enabled: twoFactorConfig.phone.enabled && twoFactorConfig.phone.confirmed, type: .phone))
        factors.append(FactorItem(name: NSLocalizedString("id_authenticator_app", comment: ""), enabled: twoFactorConfig.gauth.enabled && twoFactorConfig.gauth.confirmed, type: .gauth))
        tableView2faMethods.reloadData()
    }

    func updateConnection(_ notification: Notification) {
        let connected = notification.userInfo?["connected"] as? Bool
        self.connected = connected ?? false
    }

    func disable(_ type: TwoFactorType) {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap {
            TwoFactorConfigItem(enabled: false, confirmed: false, data: "")
        }.compactMap(on: bgq) { config in
            try JSONSerialization.jsonObject(with: JSONEncoder().encode(config), options: .allowFragments) as? [String: Any]
        }.compactMap(on: bgq) { details in
            try getGAService().getSession().changeSettingsTwoFactor(method: type.rawValue, details: details)
        }.then(on: bgq) { call in
            call.resolve(connected: { self.connected })
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.reloadData()
            getGAService().reloadTwoFactor()
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

extension TwoFactorAuthenticationViewController: UITableViewDataSource, UITableViewDelegate {

    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let item: FactorItem = factors[indexPath.row]
        if let cell = tableView.dequeueReusableCell(withIdentifier: "TwoFaMethodsCell") as? TwoFaMethodsCell {
            cell.configure(item)
            cell.selectionStyle = .none
            return cell
        }

        return UITableViewCell()
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
            let storyboard = UIStoryboard(name: "AuthenticatorFactors", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "SetEmailViewController")
            navigationController?.pushViewController(vc, animated: true)
        case .sms:
            let storyboard = UIStoryboard(name: "AuthenticatorFactors", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "SetPhoneViewController") as? SetPhoneViewController {
                vc.sms = true
                navigationController?.pushViewController(vc, animated: true)
            }
        case .phone:
            let storyboard = UIStoryboard(name: "AuthenticatorFactors", bundle: nil)
            if let vc = storyboard.instantiateViewController(withIdentifier: "SetPhoneViewController") as? SetPhoneViewController {
                vc.phoneCall = true
                navigationController?.pushViewController(vc, animated: true)
            }
        case .gauth:
            let storyboard = UIStoryboard(name: "AuthenticatorFactors", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "SetGauthViewController")
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 80.0
    }
}
