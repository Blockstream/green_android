import UIKit
import PromiseKit

class TwoFactorAuthenticationViewController: UIViewController {

    @IBOutlet weak var lblEnable2faTitle: UILabel!
    @IBOutlet weak var lblEnable2faHint: UILabel!
    @IBOutlet weak var lbl2faMethods: UILabel!
    @IBOutlet weak var tableView2faMethods: UITableView!
    @IBOutlet weak var lbl2faThresholdTitle: UILabel!
    @IBOutlet weak var lbl2faThresholdHint: UILabel!
    @IBOutlet weak var lbl2faThresholdCardTitle: UILabel!
    @IBOutlet weak var lbl2faThresholdCardHint: UILabel!
    @IBOutlet weak var btn2faThreshold: UIButton!
    @IBOutlet weak var bg2faThreshold: UIView!
    @IBOutlet weak var thresholdView: UIStackView!
    @IBOutlet weak var lbl2faExpiryTitle: UILabel!
    @IBOutlet weak var lbl2faExpiryHint: UILabel!
    @IBOutlet weak var tableViewCsvTime: DynamicTableView!
    @IBOutlet weak var lblRecoveryTool: UILabel!
    @IBOutlet weak var btnRecoveryTool: UIButton!

    var csvTypes = Settings.CsvTime.all
    var csvValues = Settings.CsvTime.values()
    var newCsv: Int?
    var currentCsv: Int?

    fileprivate var factors = [FactorItem]()
    private var connected = true
    private var updateToken: NSObjectProtocol?
    var twoFactorConfig: TwoFactorConfig?

    override func viewDidLoad() {
        super.viewDidLoad()

        title = "2FA"
        setContent()
        setStyle()

        currentCsv = Settings.shared?.csvtime
        tableViewCsvTime.estimatedRowHeight = 80
        tableViewCsvTime.rowHeight = UITableView.automaticDimension
    }

    func setContent() {
        lblEnable2faTitle.text = "Enable 2FA to protect your wallet from unauthorized transactions or changes to critical security settings"
        lblEnable2faHint.text = "TIP: We reccomend you enable more than one 2FA method. If you only set up one 2FA method and then lose it, you'll have to wait at least one year until 2FA expires"
        lbl2faMethods.text = "2FA Methods"
        lbl2faThresholdTitle.text = "2FA Threshold"
        lbl2faThresholdHint.text = "Spend your bitcoin without 2FA up to a certain threshold. After spending bitcoin up to this amount, you will need to reset your threshold to continue spending without 2FA."
        lbl2faThresholdCardTitle.text = "2FA Threshold"
        lbl2faThresholdCardHint.text = "Set 2FA threshold"
        lbl2faExpiryTitle.text = "2FA Expiry"
        lbl2faExpiryHint.text = "Customize 2FA expiration of your coins"
        lblRecoveryTool.text = "Your 2FA expires, so that if you lose access to your 2FA method, or the Blockstream Green service becomes unavailable, you can always recover your bitcoin using this open source tool"
        btnRecoveryTool.setTitle("Recovery Tool", for: .normal)
    }

    func setStyle() {
        lblEnable2faTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        lblEnable2faHint.font = UIFont.systemFont(ofSize: 16.0, weight: .regular)
        lblEnable2faTitle.textColor = .white
        lblEnable2faHint.textColor = UIColor.customGrayLight()
        lbl2faMethods.font = UIFont.systemFont(ofSize: 20.0, weight: .heavy)
        lbl2faMethods.textColor = .white
        lbl2faThresholdTitle.font = UIFont.systemFont(ofSize: 20.0, weight: .heavy)
        lbl2faThresholdHint.font = UIFont.systemFont(ofSize: 16.0, weight: .regular)
        lbl2faThresholdCardTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        lbl2faThresholdCardHint.font = UIFont.systemFont(ofSize: 16.0, weight: .regular)
        lbl2faThresholdTitle.textColor = .white
        lbl2faThresholdHint.textColor = UIColor.customGrayLight()
        lbl2faThresholdCardTitle.textColor = .white
        lbl2faThresholdCardHint.textColor = UIColor.customGrayLight()
        bg2faThreshold.layer.cornerRadius = 5.0
        lbl2faExpiryTitle.font = UIFont.systemFont(ofSize: 20.0, weight: .heavy)
        lbl2faExpiryHint.font = UIFont.systemFont(ofSize: 16.0, weight: .regular)
        lbl2faExpiryTitle.textColor = .white
        lbl2faExpiryHint.textColor = UIColor.customGrayLight()
        lblRecoveryTool.font = UIFont.systemFont(ofSize: 16.0, weight: .regular)
        lblRecoveryTool.textColor = UIColor.customGrayLight()
        btnRecoveryTool.setStyle(.primary)
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
        self.twoFactorConfig = twoFactorConfig
        factors.removeAll()
        factors.append(FactorItem(name: NSLocalizedString("id_email", comment: ""), enabled: twoFactorConfig.email.enabled && twoFactorConfig.email.confirmed, type: .email))
        factors.append(FactorItem(name: NSLocalizedString("id_sms", comment: ""), enabled: twoFactorConfig.sms.enabled && twoFactorConfig.sms.confirmed, type: .sms))
        factors.append(FactorItem(name: NSLocalizedString("id_call", comment: ""), enabled: twoFactorConfig.phone.enabled && twoFactorConfig.phone.confirmed, type: .phone))
        factors.append(FactorItem(name: NSLocalizedString("id_authenticator_app", comment: ""), enabled: twoFactorConfig.gauth.enabled && twoFactorConfig.gauth.confirmed, type: .gauth))
        tableView2faMethods.reloadData()
        tableViewCsvTime.reloadData()

        thresholdView.isHidden = true
        if self.twoFactorConfig?.anyEnabled ?? false, let settings = Settings.shared {

            thresholdView.isHidden = false
            var thresholdValue = ""

            if let twoFactorConfig = self.twoFactorConfig {
                var balance: Balance?
                let limits = twoFactorConfig.limits
                let denom = settings.denomination.rawValue
                if limits.isFiat {
                    balance = Balance.convert(details: ["fiat": limits.fiat])
                } else {
                    balance = Balance.convert(details: [denom: limits.get(TwoFactorConfigLimits.CodingKeys(rawValue: denom)!)!])
                }
                let (amount, den) = balance?.get(tag: limits.isFiat ? "fiat" : "btc") ?? ("", "")
                thresholdValue = String(format: "%@ %@", amount ?? "N.A.", den)
            }

            lbl2faThresholdCardTitle.text = NSLocalizedString("id_twofactor_threshold", comment: "")
            lbl2faThresholdCardHint.text = String(format: NSLocalizedString(thresholdValue == "" ? "" : "%@", comment: ""), thresholdValue)
        }
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

    func setCsvTimeLock(csv: Settings.CsvTime) {
        var details = [String: Any]()
        details["value"] = csv.value()
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try getGAService().getSession().setCSVTime(details: details)
        }.then(on: bgq) { call in
            call.resolve()
        }.map(on: bgq) { _ in
            if let data = try? getSession().getSettings() {
                Settings.shared = Settings.from(data)
            }
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            // update values when change successful
            self.newCsv = nil
            self.currentCsv = csv.value()
            DropAlert().success(message: String(format: "%@: %@", NSLocalizedString("id_twofactor_authentication_expiry", comment: ""), csv.label()))
            self.navigationController?.popViewController(animated: true)
        }.catch { _ in
            DropAlert().error(message: "Error changing csv time")
        }
    }

    func didSelectCSV() {
        if let csv = newCsv, let index = csvValues? .firstIndex(of: csv) {
            if csv != currentCsv {
                setCsvTimeLock(csv: csvTypes[index])
            } else {
                self.showAlert(title: NSLocalizedString("Error", comment: ""), message: "Select a new value to change csv")
            }
        }
    }

    @IBAction func btn2faThreshold(_ sender: Any) {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "TwoFactorLimitViewController")
        navigationController?.pushViewController(vc, animated: true)
    }

    @IBAction func btnRecoveryTool(_ sender: Any) {
        if let url = URL(string: "https://github.com/greenaddress/garecovery") {
            UIApplication.shared.open(url)
        }
    }
}

extension TwoFactorAuthenticationViewController: UITableViewDataSource, UITableViewDelegate {

    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if tableView == tableView2faMethods {
            let item: FactorItem = factors[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TwoFaMethodsCell") as? TwoFaMethodsCell {
                cell.configure(item)
                cell.selectionStyle = .none
                return cell
            }
        } else if tableView == tableViewCsvTime {
            let item: Settings.CsvTime = csvTypes[indexPath.row]
            if let cell = tableView.dequeueReusableCell(withIdentifier: "TwoFaCsvTimeCell") as? TwoFaCsvTimeCell {
                cell.configure(item: item, current: currentCsv)
                cell.selectionStyle = .none
                return cell
            }
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if tableView == tableView2faMethods {
            return self.factors.count
        } else if tableView == tableViewCsvTime {
            return csvTypes.count
        } else {
            return 0
        }
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {

        if tableView == tableView2faMethods {
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
        } else if tableView == tableViewCsvTime {
            let selected = csvTypes[indexPath.row]
            newCsv = selected.value()
            didSelectCSV()
        }
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        if tableView == tableView2faMethods {
            return 80.0
        } else if tableView == tableViewCsvTime {
            return UITableView.automaticDimension
        } else {
            return 0
        }
    }
}
