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

    var account: Account!
    var peripheral: Peripheral!
    var data: [[NetworkSecurityCase]] = [[]]
    var cellH = 70.0
    var headerH: CGFloat = 44.0
    var headerH2: CGFloat = 64.0
    var isHiddenTestnet: Bool = true

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
        loadData()

        let notificationCenter = NotificationCenter.default
        notificationCenter.addObserver(self, selector: #selector(appMovedToBackground), name: UIApplication.willResignActiveNotification, object: nil)
        notificationCenter.addObserver(self, selector: #selector(appBecomeActive), name: UIApplication.didBecomeActiveNotification, object: nil)
    }

    func setContent() {
        lblTitle.text = account.name
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
        deviceImage.image = account.deviceImage()
        deviceImageAlign.constant = account.alignConstraint()
    }

    func loadData() {
        if UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) {
            if isHiddenTestnet == false {
                if self.account.isLedger {
                    data = [[.bitcoinMS], [], [.testnetMS]]
                } else {
                    data = [[.bitcoinSS, .bitcoinMS], [.liquidMS], [.testnetSS, .testnetMS, .testnetLiquidMS]]
                }
            } else {
                if self.account.isLedger {
                    data = [[.bitcoinMS], [], []]
                } else {
                    data = [[.bitcoinSS, .bitcoinMS], [.liquidMS], []]
                }
            }
        } else {
            if self.account.isLedger {
                data = [[.bitcoinMS], [], []]
            } else {
                data = [[.bitcoinSS, .bitcoinMS], [.liquidMS], []]
            }
        }
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
        isHiddenTestnet = !isHiddenTestnet
        loadData()
        tableView.reloadData()
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
}

extension HWWConnectViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return  isTestnetVisible() ? NetworkSection.allCases.count : NetworkSection.allCases.count - 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case NetworkSection.mainnet.rawValue:
            return data[0].count
        case NetworkSection.liquid.rawValue:
            return data[1].count
        case NetworkSection.testnet.rawValue:
            return data[2].count
        default:
            return 0
        }
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
            if data[section].count == 0 {
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

        if let cell = tableView.dequeueReusableCell(withIdentifier: "HWWNetworkSecurityCaseCell") as? HWWNetworkSecurityCaseCell {
            cell.configure(data[indexPath.section][indexPath.item])
            cell.selectionStyle = .none
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return CGFloat(cellH)
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let item: NetworkSecurityCase = data[indexPath.section][indexPath.row]
        connect(peripheral, network: item.network())
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
        loadData()
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
        if !isHiddenTestnet {
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
