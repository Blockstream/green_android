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
    @IBOutlet weak var deviceImageAlign: NSLayoutConstraint!
    @IBOutlet weak var btnSettings: UIButton!

    var peripheral: Peripheral!

    private var networks = [NetworkSection: [NetworkSecurityCase]]()
    private var cellH = 70.0
    private var headerH: CGFloat = 44.0
    private var headerH2: CGFloat = 64.0

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

    func updateState() {
        navigationItem.setHidesBackButton(true, animated: true)
        successCircle.isHidden = true
        failureCircle.isHidden = true
        btnTryAgain.isHidden = true
        btnNeedHelp.isHidden = true
        btnLogin.isHidden = true
        deviceImage.isHidden = false
        arrowImage.isHidden = true
        btnSettings.isHidden = true

        switch hwwState {
        case .prepared:
            hideLoader()
            btnLogin.isHidden = false
            lblStateHint.text = NSLocalizedString("id_connected_to_jade", comment: "")
            if BLEManager.shared.isLedger(peripheral) {
                lblStateHint.text = String(format: "id_select_the_s_app_on_your_ledger".localized, "bitcoin")
            }
        case .connecting:
            showLoader()
            lblStateHint.text = NSLocalizedString("id_connecting_to_your_device", comment: "")
        case .connected:
            showLoader()
            lblStateHint.text = NSLocalizedString("id_logging_in", comment: "")
        case .authenticated:
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
            btnLogin.isHidden = true
        case .upgradedFirmware:
            lblStateHint.text = NSLocalizedString("id_firmware_update_completed", comment: "")
            btnLogin.setTitle(NSLocalizedString("id_continue", comment: ""), for: .normal)
            btnLogin.isHidden = false
        case .none:
            break
        }
    }

    @objc func toggleTestnet() {
        updateState()
    }

    func connect(_ peripheral: Peripheral, testnet: Bool? = nil) {
        hwwState = .connecting
        // connect Ledger X
        if BLEManager.shared.isLedger(peripheral) {
            BLEManager.shared.connect(peripheral)
            return
        }
        // start a new connection with jade
        let bgq = DispatchQueue.global(qos: .background)
        _ = after(seconds: 1)
            .compactMap(on: bgq) {
                BLEManager.shared.dispose()
                BLEManager.shared.manager.manager.cancelPeripheralConnection(peripheral.peripheral)
            }.done { _ in
                self.hwwState = .followDevice
                after(seconds: 1).done { BLEManager.shared.connect(peripheral) }
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
        if hwwState == .prepared {
            acceptTerms()
            return
        }
        if hwwState == .upgradedFirmware {
            hwwState = .connecting
            BLEManager.shared.dispose()
            navigationController?.popViewController(animated: true)
            return
        }
        if hwwState == .connected {
            hwwState = .connecting
            BLEManager.shared.auth(peripheral)
            return
        }
        hwwState = .connecting
        BLEManager.shared.login(peripheral)
    }

    @IBAction func btnSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
            vc.delegate = self
            present(vc, animated: true) {}
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
        DispatchQueue.main.async {
            self.hideLoader()
            self.hwwState = .connectFailed
            switch error {
            case .powerOff(let txt):
                self.lblStateHint.text = txt
            case .notReady(let txt):
                self.lblStateHint.text = txt
            case .scanErr(let txt):
                self.lblStateHint.text = txt
            case .bleErr(let txt):
                self.lblStateHint.text = txt
            case .timeoutErr(let txt):
                self.lblStateHint.text = txt
            case .dashboardErr(let txt):
                self.lblStateHint.text = txt
            case .outdatedAppErr(let txt):
                self.lblStateHint.text = txt
            case .wrongAppErr(let txt):
                self.lblStateHint.text = txt
            case .authErr(let txt):
                self.lblStateHint.text = txt
            case .swErr(let txt):
                self.lblStateHint.text = txt
            case .genericErr(let txt):
                self.lblStateHint.text = txt
            case .firmwareErr(txt: let txt):
                self.lblStateHint.text = txt
            case .unauthorized(txt: let txt):
                self.lblStateHint.text = txt
            }
        }
    }

    func onPrepared( _ p: Peripheral) {
        DispatchQueue.main.async {
            self.hwwState = .prepared
        }
    }

    func onConnected(_ peripheral: Peripheral, firstInitialization: Bool) {
        DispatchQueue.main.async {
            self.hwwState = .followDevice
            // if hw unitialized and testnet available on settings,
            // allow network selection testnet / mainnet
            let testnetAvailable = UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) == true
            if firstInitialization {
                if testnetAvailable {
                    self.selectNetwork()
                    return
                }
            }
            BLEManager.shared.auth(peripheral)
        }
    }

    func onAuthenticated(_ peripheral: Peripheral) {
        if BLEManager.shared.isJade(peripheral) {
            AnalyticsManager.shared.initJade()
        }
        DispatchQueue.main.async {
            self.hwwState = .authenticated
            BLEManager.shared.login(peripheral)
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
                        return
                    }
                    BLEManager.shared.loginJade(peripheral, checkFirmware: false)
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
    }
}

extension HWWConnectViewController: DialogListViewControllerDelegate {

    func selectNetwork() {
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: "Select Network", type: .networkPrefs, items: NetworkPrefs.getItems())
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    func didSelectIndex(_ index: Int, with type: DialogType) {
        switch NetworkPrefs(rawValue: index) {
        case .mainnet:
            LandingViewController.chainType = .mainnet
            BLEManager.shared.auth(peripheral, testnet: false)
        case .testnet:
            LandingViewController.chainType = .testnet
            BLEManager.shared.auth(peripheral, testnet: true)
        case .none:
            break
        }
    }
}
extension HWWConnectViewController: LandingViewControllerDelegate {
    func acceptTerms() {
        let isAcceptTerms = UserDefaults.standard.bool(forKey: AppStorage.acceptedTerms)
        if isAcceptTerms {
            hwwState = .connecting
            connect(peripheral)
            return
        }
        let onBoardS = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = onBoardS.instantiateViewController(withIdentifier: "LandingViewController") as? LandingViewController {
            vc.landingScope = .hwTerms
            vc.delegate = self
            navigationController?.pushViewController(vc, animated: true)
        }
    }
    func didPressContinue() {
        navigationController?.popViewController(animated: true)
        UserDefaults.standard.set(true, forKey: AppStorage.acceptedTerms)
        hwwState = .connecting
        connect(peripheral)
    }
}
