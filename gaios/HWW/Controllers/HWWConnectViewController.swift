import UIKit
import PromiseKit
import RxBluetoothKit

enum NetworkSection: Int, CaseIterable {
    case mainnet = 0
    case liquid = 1
    case testnet = 2
}

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
    @IBOutlet weak var deviceImageAlign: NSLayoutConstraint!
    @IBOutlet weak var btnSettings: UIButton!

    var peripheral: Peripheral!

    private var networks = [NetworkSection: [NetworkSecurityCase]]()
    private var cellH = 70.0
    private var headerH: CGFloat = 44.0
    private var headerH2: CGFloat = 64.0
    private var openAdditionalNetworks = false
    private var resetBle = false
    private var network = getGdkNetwork("mainnet")

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

        setContent()
        setStyle()

        hwwState = .connecting
        reloadData()

        BLEManager.shared.delegate = self
        BLEManager.shared.prepare(peripheral)

        AnalyticsManager.shared.recordView(.deviceInfo)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        let notificationCenter = NotificationCenter.default
        notificationCenter.addObserver(self, selector: #selector(appMovedToBackground), name: UIApplication.willResignActiveNotification, object: nil)
        notificationCenter.addObserver(self, selector: #selector(appBecomeActive), name: UIApplication.didBecomeActiveNotification, object: nil)
    }

    func setContent() {
        lblTitle.text = peripheral.name
        btnTryAgain.setTitle(NSLocalizedString("id_try_again", comment: ""), for: .normal)
        btnNeedHelp.setTitle(NSLocalizedString("id_need_help", comment: ""), for: .normal)
        btnLogin.setTitle(NSLocalizedString("id_login", comment: ""), for: .normal)
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
        btnTryAgain.setStyle(.primary)
        btnNeedHelp.setStyle(.outlined)
        arrowImage.image = UIImage(named: "ic_hww_arrow")?.maskWithColor(color: UIColor.customMatrixGreen())
        faailureImage.image = UIImage(named: "cancel")?.maskWithColor(color: UIColor.white)
        if BLEManager.shared.isJade(peripheral) {
            deviceImage.image = UIImage(named: "ic_hww_jade")
            deviceImageAlign.constant = 0
        } else {
            deviceImage.image = UIImage(named: "ic_hww_ledger")
            deviceImageAlign.constant = UIScreen.main.bounds.width * 0.27
        }
    }

    func reloadData() {
        let isEnabledTestnet = UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible)
        let showTestnet = isEnabledTestnet && openAdditionalNetworks
        let jade = BLEManager.shared.isJade(peripheral)
        var debug = false
#if DEBUG
        debug = true
#endif
        networks[.mainnet] = [.bitcoinSS, .bitcoinMS]
        networks[.liquid] = jade ? [.liquidMS] : []
        networks[.liquid]! += jade && debug ? [.liquidSS] : []
        networks[.testnet] = showTestnet ? [.testnetSS, .testnetMS] : []
        networks[.testnet]! += showTestnet && jade ? [.testnetLiquidMS] : []
        networks[.testnet]! += showTestnet && jade && debug ? [.testnetLiquidSS] : []
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

    func isTestnetVisible() -> Bool {
        return UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) == true
    }

    @objc func toggleTestnet() {
        openAdditionalNetworks = !openAdditionalNetworks
        reloadData()
    }

    func connect(_ peripheral: Peripheral, network: String) {
        hwwState = .connecting
        self.network = getGdkNetwork(network)
        // connect Ledger X
        if BLEManager.shared.isLedger(peripheral) {
            BLEManager.shared.connect(peripheral, network: self.network)
            return
        }
        // keep open connection with device, if connected
        if peripheral.isConnected && !resetBle {
            BLEManager.shared.connect(peripheral, network: self.network)
            return
        }
        // start a new connection with jade
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            BLEManager.shared.dispose()
            BLEManager.shared.manager.manager.cancelPeripheralConnection(peripheral.peripheral)
            return Guarantee()
        }.then(on: bgq) {
            after(seconds: 1)
        }.done { _ in
            self.hwwState = .followDevice
            BLEManager.shared.connect(peripheral, network: self.network)
        }
    }

    @IBAction func btnTryAgain(_ sender: Any) {
        if BLEManager.shared.isJade(peripheral) {
            hwwState = .connecting
        } else if BLEManager.shared.isLedger(peripheral) {
            hwwState = .selectNetwork
        }
        //BLEManager.shared.dispose()
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
        }
        hwwState = .connected
        BLEManager.shared.login(peripheral, network: network)
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
            vc.delegate = self
            present(vc, animated: true) {}
        }
    }
}

extension HWWConnectViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return  isTestnetVisible() ? NetworkSection.allCases.count : NetworkSection.allCases.count - 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if let networkSection = NetworkSection(rawValue: section) {
            return networks[networkSection]?.count ?? 0
        }
        return 0
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        switch section {
        case NetworkSection.mainnet.rawValue:
            return headerH
        case NetworkSection.liquid.rawValue:
            return headerH
        case NetworkSection.testnet.rawValue:
            return headerH2
        default:
            return 1
        }
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 1
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        switch section {
        case NetworkSection.mainnet.rawValue:
            return headerView("Bitcoin")
        case NetworkSection.liquid.rawValue:
            if networks[.liquid]?.count ?? 0 == 0 {
                return headerView("")
            }
            return headerView("Liquid")
        default:
            return headerDisclosureView()
        }
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        if let cell = tableView.dequeueReusableCell(withIdentifier: "HWWNetworkSecurityCaseCell") as? HWWNetworkSecurityCaseCell,
           let section = NetworkSection(rawValue: indexPath.section),
           let networkSection = networks[section] {
            cell.configure(networkSection[indexPath.item])
            cell.selectionStyle = .none
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return CGFloat(cellH)
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if let section = NetworkSection(rawValue: indexPath.section),
            let networkSection = networks[section] {
            let item: NetworkSecurityCase = networkSection[indexPath.row]
            connect(peripheral, network: item.network)
        }
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

    func onPrepare(_: Peripheral, reset: Bool = false) {
        DispatchQueue.main.async {
            self.hwwState = .selectNetwork
            self.resetBle = reset
        }
    }

    func onAuthenticate(_ peripheral: Peripheral, network: GdkNetwork, firstInitialization: Bool) {
        DispatchQueue.main.async {
            if firstInitialization {
                self.hwwState = .initialized
            }
            self.hwwState = .connected
            BLEManager.shared.login(peripheral, network: network)
        }
    }

    func onLogin(_: Peripheral, account: Account) {
        AnalyticsManager.shared.loginWallet(loginType: .hardware, ephemeralBip39: false, account: account)
        DispatchQueue.main.async {
            getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        }
    }

    func onConnectivityChange(peripheral: Peripheral, status: Bool) {
    }

    func onCheckFirmware(_ peripheral: Peripheral,
                         fmw: Firmware,
                         currentVersion: String,
                         needCableUpdate: Bool) {

        let required = !Jade.shared.isJadeFwValid(currentVersion)
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogOTAViewController") as? DialogOTAViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.isRequired = required
            vc.needCableUpdate = needCableUpdate
            vc.firrmwareVersion = fmw.version

            vc.onSelect = { [weak self] (action: OTAAction) in
                switch action {
                case .update:
                    self?.hwwState = .upgradingFirmware
                    BLEManager.shared.updateFirmware(peripheral, fmw: fmw, currentVersion: currentVersion)
                case .readMore:
                    BLEManager.shared.dispose()
                    UIApplication.shared.open(ExternalUrls.otaReadMore, options: [:], completionHandler: nil)
                    self?.navigationController?.popViewController(animated: true)
                case .cancel:
                    if required {
                        BLEManager.shared.dispose()
                        self?.onError(BLEManagerError.genericErr(txt: NSLocalizedString("id_new_jade_firmware_required", comment: "")))
                    }
                    BLEManager.shared.login(peripheral, network: self!.network, checkFirmware: false)
                }
            }
            present(vc, animated: false, completion: nil)
        }
    }

    func onUpdateFirmware(_ peripheral: Peripheral, version: String, prevVersion: String) {
        self.hwwState = .upgradedFirmware
        if prevVersion <= "0.1.30" && version >= "0.1.31" {
            let msg = NSLocalizedString("id_the_new_firmware_requires_you", comment: "")
             let alert = UIAlertController(title: NSLocalizedString("id_firmware_update_completed", comment: ""), message: msg, preferredStyle: .actionSheet)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_more_info", comment: ""), style: .cancel) { _ in
                BLEManager.shared.dispose()
                UIApplication.shared.open(ExternalUrls.jadeMoreInfo, options: [:], completionHandler: nil)
            })
            self.present(alert, animated: true, completion: nil)
        }
    }

    func onComputedHash(_ hash: String) {
        if self.hwwState == .upgradingFirmware {

            let titleAttributes: [NSAttributedString.Key: Any] = [
                .foregroundColor: UIColor.white
            ]
            let hashAttributes: [NSAttributedString.Key: Any] = [
                .foregroundColor: UIColor.customGrayLight(),
                .font: UIFont.systemFont(ofSize: 16)
            ]
            let title = NSLocalizedString("id_updating_firmware", comment: "")
            let hint = "\n\n" + "Firmware hash:" + "\n" + hash

            let attributedTitleString = NSMutableAttributedString(string: title)
            attributedTitleString.setAttributes(titleAttributes, for: title)
            let attributedHintString = NSMutableAttributedString(string: hint)
            attributedHintString.setAttributes(hashAttributes, for: hint)
            attributedTitleString.append(attributedHintString)

            lblStateHint.attributedText = attributedTitleString
        }
    }
}

extension HWWConnectViewController: WalletSettingsViewControllerDelegate {
    func didSet(tor: Bool) {
        //
    }
    func didSet(testnet: Bool) {
        reloadData()
    }
}

extension HWWConnectViewController {
    func headerView(_ txt: String) -> UIView {
        if txt == "" {
            let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: 1.0))
            section.backgroundColor = .clear
            return section
        }
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH))
        section.backgroundColor = UIColor.customTitaniumDark()
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 20.0, weight: .heavy)
        title.text = txt
        title.textColor = .white
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 24),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -24)
        ])

        return section
    }

    func headerDisclosureView() -> UIView {
        let color = UIColor.accountGray()
        let section = UIView(frame: CGRect(x: 0, y: 0, width: tableView.frame.width, height: headerH2))
        section.backgroundColor = UIColor.customTitaniumDark()
        let title = UILabel(frame: .zero)
        title.font = .systemFont(ofSize: 16.0, weight: .regular)
        title.text = NSLocalizedString("id_additional_networks", comment: "")
        title.textColor = color
        title.numberOfLines = 0

        title.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(title)

        NSLayoutConstraint.activate([
            title.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            title.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 30),
            title.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -24)
        ])

        let line = UIView(frame: .zero)
        line.backgroundColor = color
        line.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(line)

        NSLayoutConstraint.activate([
            line.leftAnchor.constraint(equalTo: section.leftAnchor),
            line.rightAnchor.constraint(equalTo: section.rightAnchor),
            line.topAnchor.constraint(equalTo: section.topAnchor, constant: 10.0),
            line.heightAnchor.constraint(equalToConstant: 1.0)
        ])

        let arrow = UIImageView(frame: .zero)
        arrow.image = UIImage(named: "rightArrow")?.maskWithColor(color: color)
        if openAdditionalNetworks {
            arrow.transform = CGAffineTransform(rotationAngle: CGFloat.pi/2)
        }
        arrow.translatesAutoresizingMaskIntoConstraints = false
        section.addSubview(arrow)

        NSLayoutConstraint.activate([
            arrow.centerYAnchor.constraint(equalTo: section.centerYAnchor),
            arrow.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 10.0)
        ])

        let toggleBtn = UIButton(frame: .zero)
        toggleBtn.setTitle("", for: .normal)
        toggleBtn.translatesAutoresizingMaskIntoConstraints = false
        toggleBtn.addTarget(self, action: #selector(toggleTestnet), for: .touchUpInside)
        section.addSubview(toggleBtn)

        NSLayoutConstraint.activate([
            toggleBtn.topAnchor.constraint(equalTo: section.topAnchor, constant: 0.0),
            toggleBtn.bottomAnchor.constraint(equalTo: section.bottomAnchor, constant: -10.0),
            toggleBtn.leadingAnchor.constraint(equalTo: section.leadingAnchor, constant: 20.0),
            toggleBtn.trailingAnchor.constraint(equalTo: section.trailingAnchor, constant: -20.0)
        ])
        return section
    }
}
