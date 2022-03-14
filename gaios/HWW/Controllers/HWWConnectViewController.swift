import UIKit
import PromiseKit
import RxBluetoothKit

class HWWConnectViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblStateHint: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!
    @IBOutlet weak var successCircle: UIView!
    @IBOutlet weak var failureCircle: UIView!
    @IBOutlet weak var faailureImage: UIImageView!
    @IBOutlet weak var btnTryAgain: UIButton!
    @IBOutlet weak var btnNeedHelp: UIButton!
    @IBOutlet weak var btnLogin: UIButton!
    @IBOutlet weak var deviceImage: UIImageView!
    @IBOutlet weak var arrowImage: UIImageView!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var tableViewHeight: NSLayoutConstraint!
    @IBOutlet weak var deviceImageAlign: NSLayoutConstraint!
    @IBOutlet weak var singleSigWarnCard: UIView!
    @IBOutlet weak var lblSingleSigWarn: UILabel!
    @IBOutlet weak var iconSingleSigWarn: UIImageView!
    @IBOutlet weak var btnSettings: UIButton!

    var account: Account!
    var peripheral: Peripheral!

    var networks: [AvailableNetworks] = []

    var cellH = 70.0

    let loadingIndicator: ProgressView = {
        let progress = ProgressView(colors: [UIColor.customMatrixGreen()], lineWidth: 2)
        progress.translatesAutoresizingMaskIntoConstraints = false
        return progress
    }()

    var hwwState: HWWState! {
        didSet {
            self.updateState()
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        BLEManager.shared.delegate = self

        setContent()
        setStyle()

        hwwState = .connecting
        BLEManager.shared.prepare(peripheral)
        loadNetworks()

        let notificationCenter = NotificationCenter.default
        notificationCenter.addObserver(self, selector: #selector(appMovedToBackground), name: UIApplication.willResignActiveNotification, object: nil)
        notificationCenter.addObserver(self, selector: #selector(appBecomeActive), name: UIApplication.didBecomeActiveNotification, object: nil)
    }

    func setContent() {
        lblTitle.text = account.name
        btnTryAgain.setTitle(NSLocalizedString("id_try_again", comment: ""), for: .normal)
        btnNeedHelp.setTitle(NSLocalizedString("id_need_help", comment: ""), for: .normal)
        btnLogin.setTitle(NSLocalizedString("id_login", comment: ""), for: .normal)
        lblSingleSigWarn.text = NSLocalizedString("id_singlesig_wallets_are_not_yet", comment: "")
        btnSettings.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        let notificationCenter = NotificationCenter.default
        notificationCenter.removeObserver(self, name: UIApplication.willResignActiveNotification, object: nil)
        notificationCenter.removeObserver(self, name: UIApplication.didBecomeActiveNotification, object: nil)
    }

    @objc func appMovedToBackground() {
        hideLoader()
    }

    @objc func appBecomeActive() {
        if hwwState == .upgradingFirmware {
            showLoader()
        }
    }

    func setStyle() {
        successCircle.borderWidth = 2.0
        successCircle.layer.cornerRadius = successCircle.frame.size.width / 2.0
        successCircle.borderColor = UIColor.customMatrixGreen()
        failureCircle.borderWidth = 2.0
        failureCircle.layer.cornerRadius = successCircle.frame.size.width / 2.0
        failureCircle.borderColor = UIColor.white
        btnTryAgain.cornerRadius = 4.0
        btnNeedHelp.cornerRadius = 4.0
        arrowImage.image = UIImage(named: "ic_hww_arrow")?.maskWithColor(color: UIColor.customMatrixGreen())
        faailureImage.image = UIImage(named: "cancel")?.maskWithColor(color: UIColor.white)
        deviceImage.image = account.deviceImage()
        deviceImageAlign.constant = account.alignConstraint()
        lblSingleSigWarn.textColor = UIColor.customGrayLight()
        self.iconSingleSigWarn.image = UIImage(named: "ic_logo_green")!.maskWithColor(color: UIColor.customGrayLight())
        singleSigWarnCard.layer.borderWidth = 1.0
        singleSigWarnCard.layer.borderColor = UIColor.customGrayLight().cgColor
        singleSigWarnCard.cornerRadius = 8.0
    }

    func loadNetworks() {
        if UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) {
            if self.account.isLedger {
                self.networks = [AvailableNetworks.bitcoin, AvailableNetworks.testnet]
            } else {
                networks = AvailableNetworks.allCases
            }
        } else {
            if self.account.isLedger {
                self.networks = [AvailableNetworks.bitcoin]
            } else {
                networks = [AvailableNetworks.bitcoin, AvailableNetworks.liquid]
            }
        }
        tableViewHeight.constant = CGFloat(networks.count) * CGFloat(cellH)
        tableView.reloadData()
    }

    func updateState() {
        navigationItem.setHidesBackButton(true, animated: true)
        successCircle.isHidden = true
        failureCircle.isHidden = true
        btnTryAgain.isHidden = true
        btnNeedHelp.isHidden = true
        btnLogin.isHidden = true
        deviceImage.isHidden = false
        arrowImage.isHidden = true
        tableView.isHidden = true
        singleSigWarnCard.isHidden = true
        btnSettings.isHidden = true

        switch hwwState {
        case .connecting:
            showLoader()
            lblStateHint.text = NSLocalizedString("id_connecting_to_your_device", comment: "")
        case .connected:
            showLoader()
            lblStateHint.text = NSLocalizedString("id_logging_in", comment: "")
        case .connectFailed:
            hideLoader()
            navigationItem.setHidesBackButton(false, animated: true)
            lblStateHint.text = NSLocalizedString("id_connection_failed", comment: "")
            failureCircle.isHidden = false
            btnTryAgain.isHidden = false
            btnNeedHelp.isHidden = false
        case .selectNetwork:
            hideLoader()
            lblStateHint.text = NSLocalizedString("id_select_network", comment: "")
            deviceImage.isHidden = true
            tableView.isHidden = false
            singleSigWarnCard.isHidden = false
            btnSettings.isHidden = false
        case .followDevice:
            hideLoader()
            lblStateHint.text = NSLocalizedString("id_follow_the_instructions_on_your", comment: "")
            navigationItem.setHidesBackButton(false, animated: true)
            arrowImage.isHidden = false
        case .upgradingFirmware:
            showLoader()
            lblStateHint.text = NSLocalizedString("id_updating_firmware", comment: "")
            navigationItem.setHidesBackButton(false, animated: true)
        case .initialized:
            lblStateHint.text = NSLocalizedString("id_ready_to_start", comment: "")
            btnLogin.isHidden = false
        case .upgradedFirmware:
            lblStateHint.text = NSLocalizedString("id_firmware_update_completed", comment: "")
            btnLogin.setTitle(NSLocalizedString("id_continue", comment: ""), for: .normal)
            btnLogin.isHidden = false
        case .none:
            break
        }
    }

    @IBAction func btnTryAgain(_ sender: Any) {
        if self.account.isJade {
            hwwState = .connecting
        }
        if self.account.isLedger {
            hwwState = .selectNetwork
        }
        BLEManager.shared.dispose()
        BLEManager.shared.prepare(peripheral)
    }

    @IBAction func btnNeedHelp(_ sender: Any) {
        UIApplication.shared.open(ExternalUrls.jadeNeedHelp, options: [:], completionHandler: nil)
    }

    @IBAction func btnLogin(_ sender: Any) {
        if hwwState == .upgradedFirmware {
            hwwState = .connecting
            BLEManager.shared.dispose()
            navigationController?.popViewController(animated: true)
        } else {
            hwwState = .connected
            BLEManager.shared.login(peripheral)
        }
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
            vc.delegate = self
            present(vc, animated: true) {}
        }
    }

    func connect(_ peripheral: Peripheral, network: String) {
        hwwState = .connecting
        account?.network = network
        AccountsManager.shared.current = account
        if BLEManager.shared.isLedger(peripheral) {
            BLEManager.shared.connect(peripheral, network: network)
            return
        }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            BLEManager.shared.dispose()
            BLEManager.shared.manager.manager.cancelPeripheralConnection(peripheral.peripheral)
            return Guarantee()
        }.then(on: bgq) {
            after(seconds: 1)
        }.done { _ in
            self.hwwState = .followDevice
            BLEManager.shared.connect(peripheral, network: network)
        }
    }
}

extension HWWConnectViewController: UITableViewDelegate, UITableViewDataSource {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return networks.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        if let cell = tableView.dequeueReusableCell(withIdentifier: "HWWNetworkCell") as? HWWNetworkCell {
            cell.configure(networks[indexPath.row])
            cell.selectionStyle = .none
            return cell
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return CGFloat(cellH)
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        connect(peripheral, network: networks[indexPath.row].rawValue)
    }
}

extension HWWConnectViewController {
    func showLoader() {
        if loadingIndicator.isAnimating { return }
        self.view.addSubview(loadingIndicator)

        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor
                .constraint(equalTo: self.loaderPlaceholder.centerXAnchor),
            loadingIndicator.centerYAnchor
                .constraint(equalTo: self.loaderPlaceholder.centerYAnchor),
            loadingIndicator.widthAnchor
                .constraint(equalToConstant: self.loaderPlaceholder.frame.width),
            loadingIndicator.heightAnchor
                .constraint(equalTo: self.loadingIndicator.widthAnchor)
        ])

        loadingIndicator.isAnimating = true
    }

    func hideLoader() {
        if !loadingIndicator.isAnimating { return }
        loadingIndicator.isAnimating = false
    }
}

extension HWWConnectViewController: BLEManagerDelegate {

    func onError(_ error: BLEManagerError) {
        hideLoader()
        hwwState = .connectFailed
        switch error {
        case .powerOff(let txt):
            lblStateHint.text = txt
        case .notReady(let txt):
            lblStateHint.text = txt
        case .scanErr(let txt):
            lblStateHint.text = txt
        case .bleErr(let txt):
            lblStateHint.text = txt
        case .timeoutErr(let txt):
            lblStateHint.text = txt
        case .dashboardErr(let txt):
            lblStateHint.text = txt
        case .outdatedAppErr(let txt):
            lblStateHint.text = txt
        case .wrongAppErr(let txt):
            lblStateHint.text = txt
        case .authErr(let txt):
            lblStateHint.text = txt
        case .swErr(let txt):
            lblStateHint.text = txt
        case .genericErr(let txt):
            lblStateHint.text = txt
        case .firmwareErr(txt: let txt):
            lblStateHint.text = txt
        case .unauthorized(txt: let txt):
            lblStateHint.text = txt
        }
    }

    func onPrepare(_: Peripheral) {
        DispatchQueue.main.async {
            self.hwwState = .selectNetwork
        }
    }

    func onAuthenticate(_ peripheral: Peripheral, network: String, firstInitialization: Bool) {
        DispatchQueue.main.async {
            if firstInitialization {
                self.hwwState = .initialized
            } else {
                self.hwwState = .connected
                BLEManager.shared.login(peripheral)
            }
        }
    }

    func onLogin(_: Peripheral) {
        DispatchQueue.main.async {
            getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        }
    }

    func onConnectivityChange(peripheral: Peripheral, status: Bool) {
    }

    func onCheckFirmware(_ peripheral: Peripheral,
                         fmw: [String: String],
                         currentVersion: String,
                         needCableUpdate: Bool) {

        let required = !Jade.shared.isJadeFwValid(currentVersion)
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogOTAViewController") as? DialogOTAViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.isRequired = required
            vc.needCableUpdate = needCableUpdate
            vc.firrmwareVersion = fmw["version"] ?? ""

            vc.onSelect = { [weak self] (action: OTAAction) in
                switch action {
                case .update:
                    self?.hwwState = .upgradingFirmware
                    BLEManager.shared.updateFirmware(peripheral, fmwFile: fmw, currentVersion: currentVersion)
                case .readMore:
                    BLEManager.shared.dispose()
                    UIApplication.shared.open(ExternalUrls.otaReadMore, options: [:], completionHandler: nil)
                    self?.navigationController?.popViewController(animated: true)
                case .cancel:
                    if required {
                        BLEManager.shared.dispose()
                        self?.onError(BLEManagerError.genericErr(txt: NSLocalizedString("id_new_jade_firmware_required", comment: "")))
                    } else {
                        BLEManager.shared.login(peripheral, checkFirmware: false)
                    }
                }
            }
            present(vc, animated: false, completion: nil)
        }
    }

    func onUpdateFirmware(_ peripheral: Peripheral, version: String, prevVersion: String) {
        self.hwwState = .upgradedFirmware
        if prevVersion <= "0.1.30" && version >= "0.1.31" {
            let msg = "The new firmware requires you to unpair your Jade from the iOS Bluetooth settings."
             let alert = UIAlertController(title: NSLocalizedString("id_firmware_update_completed", comment: ""), message: msg, preferredStyle: .actionSheet)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_more_info", comment: ""), style: .cancel) { _ in
                BLEManager.shared.dispose()
                UIApplication.shared.open(ExternalUrls.jadeMoreInfo, options: [:], completionHandler: nil)
            })
            self.present(alert, animated: true, completion: nil)
        }
    }
}

extension HWWConnectViewController: WalletSettingsViewControllerDelegate {
    func didSet(tor: Bool) {
        //
    }
    func didSet(testnet: Bool) {
        loadNetworks()
    }
}
