import UIKit
import PromiseKit

protocol TwoFactorAuthenticationViewControllerDelegate: AnyObject {
    func userLogout()
}

class TwoFactorAuthenticationViewController: UIViewController {

    @IBOutlet weak var lblEnable2faTitle: UILabel!
    @IBOutlet weak var lblEnable2faHint: UILabel!
    @IBOutlet weak var lbl2faMethods: UILabel!
    @IBOutlet weak var tableView2faMethods: UITableView!
    @IBOutlet weak var lbl2faThresholdTitle: UILabel!
    @IBOutlet weak var lbl2faThresholdHint: UILabel!
    @IBOutlet weak var lbl2faThresholdCardTitle: UILabel!
    @IBOutlet weak var lbl2faThresholdCardHint: UILabel!
    @IBOutlet weak var thresholdCardDisclosure: UIImageView!
    @IBOutlet weak var btn2faThreshold: UIButton!
    @IBOutlet weak var bg2faThreshold: UIView!
    @IBOutlet weak var thresholdView: UIStackView!
    @IBOutlet weak var lblReset2faTitle: UILabel!
    @IBOutlet weak var lblReset2faCardTitle: UILabel!
    @IBOutlet weak var reset2faCardDisclosure: UIImageView!
    @IBOutlet weak var bgReset2fa: UIView!
    @IBOutlet weak var reset2faView: UIStackView!
    @IBOutlet weak var lbl2faExpiryTitle: UILabel!
    @IBOutlet weak var lbl2faExpiryHint: UILabel!
    @IBOutlet weak var tableViewCsvTime: DynamicTableView!
    @IBOutlet weak var lblRecoveryTool: UILabel!
    @IBOutlet weak var btnRecoveryTool: UIButton!

    private var csvTypes = Settings.CsvTime.all()
    private var csvValues = Settings.CsvTime.values()
    private var newCsv: Int?
    private var currentCsv: Int?

    fileprivate var factors = [TwoFactorItem]()
    private var connected = true
    private var updateToken: NSObjectProtocol?
    private var twoFactorConfig: TwoFactorConfig?
    var session: SessionManager!

    weak var delegate: TwoFactorAuthenticationViewControllerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        title = "2FA"
        setContent()
        setStyle()

        currentCsv = session.settings?.csvtime
        tableViewCsvTime.estimatedRowHeight = 80
        tableViewCsvTime.rowHeight = UITableView.automaticDimension

        if session.gdkNetwork.liquid {
            lbl2faExpiryTitle.isHidden = true
            lbl2faExpiryHint.isHidden = true
            tableViewCsvTime.isHidden = true
            lblRecoveryTool.isHidden = true
            reset2faView.isHidden = true
        }

        AnalyticsManager.shared.recordView(.walletSettings2FA, sgmt: AnalyticsManager.shared.sessSgmt(AccountsManager.shared.current))
    }

    func setContent() {
        lblEnable2faTitle.text = NSLocalizedString("id_enable_twofactor_authentication", comment: "")
        lblEnable2faHint.text = NSLocalizedString("id_tip_we_recommend_you_enable", comment: "")
        lbl2faMethods.text = NSLocalizedString("id_2fa_methods", comment: "")
        lbl2faThresholdTitle.text = NSLocalizedString("id_2fa_threshold", comment: "")
        lbl2faThresholdHint.text = NSLocalizedString("id_spend_your_bitcoin_without_2fa", comment: "")
        lbl2faThresholdCardTitle.text = NSLocalizedString("id_2fa_threshold", comment: "")
        lbl2faThresholdCardHint.text = NSLocalizedString("id_set_twofactor_threshold", comment: "")
        lbl2faExpiryTitle.text = NSLocalizedString("id_2fa_expiry", comment: "")
        lbl2faExpiryHint.text = NSLocalizedString("id_customize_2fa_expiration_of", comment: "")
        lblRecoveryTool.text = NSLocalizedString("id_your_2fa_expires_so_that_if_you", comment: "")
        btnRecoveryTool.setTitle(NSLocalizedString("id_recovery_tool", comment: ""), for: .normal)
        lblReset2faTitle.text = NSLocalizedString("id_request_twofactor_reset", comment: "")
        lblReset2faCardTitle.text = NSLocalizedString("id_i_lost_my_2fa", comment: "")
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
        thresholdCardDisclosure.image = UIImage(named: "rightArrow")?.maskWithColor(color: .white)
        lblReset2faTitle.font = UIFont.systemFont(ofSize: 20.0, weight: .heavy)
        lblReset2faCardTitle.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        reset2faCardDisclosure.image = UIImage(named: "rightArrow")?.maskWithColor(color: .white)
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
        guard let dataTwoFactorConfig = try? session.session?.getTwoFactorConfig() else { return }
        guard let twoFactorConfig = try? JSONDecoder().decode(TwoFactorConfig.self, from: JSONSerialization.data(withJSONObject: dataTwoFactorConfig, options: [])) else { return }
        self.twoFactorConfig = twoFactorConfig
        factors.removeAll()
        factors.append(TwoFactorItem(name: NSLocalizedString("id_email", comment: ""), enabled: twoFactorConfig.email.enabled && twoFactorConfig.email.confirmed, maskedData: twoFactorConfig.email.data, type: .email))
        factors.append(TwoFactorItem(name: NSLocalizedString("id_sms", comment: ""), enabled: twoFactorConfig.sms.enabled && twoFactorConfig.sms.confirmed, maskedData: twoFactorConfig.sms.data, type: .sms))
        factors.append(TwoFactorItem(name: NSLocalizedString("id_call", comment: ""), enabled: twoFactorConfig.phone.enabled && twoFactorConfig.phone.confirmed, maskedData: twoFactorConfig.phone.data, type: .phone))
        factors.append(TwoFactorItem(name: NSLocalizedString("id_authenticator_app", comment: ""), enabled: twoFactorConfig.gauth.enabled && twoFactorConfig.gauth.confirmed, type: .gauth))
        tableView2faMethods.reloadData()
        tableViewCsvTime.reloadData()

        thresholdView.isHidden = true
        if self.twoFactorConfig?.anyEnabled ?? false,
            let settings = session.settings,
            let twoFactorConfig = self.twoFactorConfig {

            let limits = twoFactorConfig.limits
            var (amount, den) = ("", "")
            if limits.isFiat {
                let balance = Balance.fromFiat(limits.fiat ?? "0")
                (amount, den) = balance?.toDenom() ?? ("", "")
            } else {
                let denom = settings.denomination.rawValue
                let balance = Balance.fromDenomination(limits.get(TwoFactorConfigLimits.CodingKeys(rawValue: denom)!) ?? "0")
                (amount, den) = balance?.toFiat() ?? ("", "")
            }
            let thresholdValue = String(format: "%@ %@", amount, den)
            thresholdView.isHidden = false
            lbl2faThresholdCardTitle.text = NSLocalizedString("id_twofactor_threshold", comment: "")
            lbl2faThresholdCardHint.text = String(format: NSLocalizedString(thresholdValue == "" ? "" : "%@", comment: ""), thresholdValue)
        }
    }

    func updateConnection(_ notification: Notification) {
        if let data = notification.userInfo,
              let json = try? JSONSerialization.data(withJSONObject: data, options: []),
              let connection = try? JSONDecoder().decode(Connection.self, from: json) {
            self.connected = connection.connected
        }
    }

    func disable(_ type: TwoFactorType) {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap {
            TwoFactorConfigItem(enabled: false, confirmed: false, data: "")
        }.then(on: bgq) { config in
            self.session.changeSettingsTwoFactor(method: type, config: config)
        }.then(on: bgq) { _ in
            self.session.loadTwoFactorConfig()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.reloadData()
            let notification = NSNotification.Name(rawValue: EventType.TwoFactorReset.rawValue)
            NotificationCenter.default.post(name: notification, object: nil, userInfo: nil)
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
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            self.session.setCSVTime(value: csv.value()!)
        }.then(on: bgq) { _ in
            self.session.loadSettings()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            // update values when change successful
            self.newCsv = nil
            self.currentCsv = csv.value()
            DropAlert().success(message: String(format: "%@: %@", NSLocalizedString("id_twofactor_authentication_expiry", comment: ""), csv.label()))
            self.reloadData()
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

    func showResetTwoFactor() {
        let hint = "jane@example.com"
        let alert = UIAlertController(title: NSLocalizedString("id_request_twofactor_reset", comment: ""), message: NSLocalizedString("id_resetting_your_twofactor_takes", comment: ""), preferredStyle: .alert)
        alert.addTextField { textField in
            textField.placeholder = hint
            textField.keyboardType = .emailAddress
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_save", comment: ""), style: .default) { _ in
            let textField = alert.textFields!.first
            let email = textField!.text
            self.resetTwoFactor(email: email!)
        })
        self.present(alert, animated: true, completion: nil)
    }

    func resetTwoFactor(email: String) {
        //AnalyticsManager.shared.recordView(.walletSettings2FAReset, sgmt: AnalyticsManager.shared.twoFacSgmt(AccountsManager.shared.current, walletType: wallet?.type, twoFactorType: nil))

        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            self.session.resetTwoFactor(email: email, isDispute: false)
        }.then(on: bgq) {_ in
            self.session.loadTwoFactorConfig()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            DropAlert().success(message: NSLocalizedString("id_2fa_reset_in_progress", comment: ""))
            let notification = NSNotification.Name(rawValue: EventType.TwoFactorReset.rawValue)
            NotificationCenter.default.post(name: notification, object: nil, userInfo: nil)
            self.delegate?.userLogout()
        }.catch { error in
            var text: String
            if let error = error as? TwoFactorCallError {
                switch error {
                case .failure(let localizedDescription), .cancel(let localizedDescription):
                    text = localizedDescription
                }
                self.showError(text)
            }
        }
    }

    @IBAction func btnReset2fa(_ sender: Any) {
        showResetTwoFactor()
    }

    @IBAction func btn2faThreshold(_ sender: Any) {
        let storyboard = UIStoryboard(name: "UserSettings", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "TwoFactorLimitViewController") as? TwoFactorLimitViewController {
            vc.session = session
            navigationController?.pushViewController(vc, animated: true)
        }
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
            let item: TwoFactorItem = factors[indexPath.row]
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
            let selectedFactor: TwoFactorItem = self.factors[indexPath.row]
            if selectedFactor.enabled {
                disable(selectedFactor.type)
                return
            }
            switch selectedFactor.type {
            case .email:
                let storyboard = UIStoryboard(name: "AuthenticatorFactors", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "SetEmailViewController") as? SetEmailViewController {
                    vc.session = session
                    navigationController?.pushViewController(vc, animated: true)
                }
            case .sms:
                let storyboard = UIStoryboard(name: "AuthenticatorFactors", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "SetPhoneViewController") as? SetPhoneViewController {
                    vc.sms = true
                    vc.session = session
                    navigationController?.pushViewController(vc, animated: true)
                }
            case .phone:
                let storyboard = UIStoryboard(name: "AuthenticatorFactors", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "SetPhoneViewController") as? SetPhoneViewController {
                    vc.phoneCall = true
                    vc.session = session
                    navigationController?.pushViewController(vc, animated: true)
                }
            case .gauth:
                let storyboard = UIStoryboard(name: "AuthenticatorFactors", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "SetGauthViewController") as? SetGauthViewController {
                    vc.session = session
                    navigationController?.pushViewController(vc, animated: true)
                }
            }

            //AnalyticsManager.shared.recordView(.walletSettings2FASetup, sgmt: AnalyticsManager.shared.twoFacSgmt(AccountsManager.shared.current, walletType: wallet?.type, twoFactorType: selectedFactor.type))
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
