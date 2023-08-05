import UIKit
import CoreBluetooth
import AsyncBluetooth
import Combine
import gdk
import hw

enum PairingState: Int {
    case unknown
    case pairing
    case paired
}

class ConnectViewController: HWFlowBaseViewController {
    
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!
    @IBOutlet weak var image: UIImageView!
    @IBOutlet weak var retryButton: UIButton!
    
    var account: Account!
    var bleViewModel: BleViewModel?
    var scanViewModel: ScanViewModel?
    
    private var activeToken, resignToken: NSObjectProtocol?
    private var pairingState: PairingState = .unknown
    private var selectedItem: ScanListItem?
    private var scanCancellable: AnyCancellable?
    private var cancellables = Set<AnyCancellable>()
    
    
    let loadingIndicator: ProgressView = {
        let progress = ProgressView(colors: [UIColor.customMatrixGreen()], lineWidth: 2)
        progress.translatesAutoresizingMaskIntoConstraints = false
        return progress
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        loadNavigationBtns()
    }
    
    @MainActor
    func setContent() {
        if account.isJade {
            image.image = UIImage(named: "il_jade_unlock")
        } else {
            image.image = UIImage(named: "il_ledger")
        }
        retryButton.isHidden = true
        retryButton.setTitle("Retry".localized, for: .normal)
        retryButton.setStyle(.primary)
        lblTitle.text = "id_looking_for_device".localized
        
    }
    
    func onScannedDevice(_ item: ScanListItem) {
        pairingState = .unknown
        Task {
            do {
                await scanViewModel?.stopScan()
                bleViewModel?.type = item.type
                bleViewModel?.peripheralID = item.identifier
                // connection
                progress("id_connecting".localized)
                try await bleViewModel?.connect()
                if pairingState != .unknown && account.isJade {
                    // pairing roundtrip for jade
                    try? await bleViewModel?.disconnect()
                    try await Task.sleep(nanoseconds:  5 * 1_000_000_000)
                    startScan()
                    return
                }
                // ping if still connected and responding
                try await bleViewModel?.ping()
                print("pinged")
                // check version, only for jade
                let version = try await bleViewModel?.versionJade()
                if account.isJade {
                    if version?.jadeHasPin ?? false {
                        progress("id_unlock_jade_to_continue".localized)
                    } else {
                        progress("id_follow_the_instructions_on_jade".localized)
                    }
                } else {
                    progress("id_connect_your_ledger_to_use_it".localized)
                }
                for i in 0..<3 {
                    if let res = try await bleViewModel?.authenticating(), res == true {
                        break
                    } else if i == 2 {
                        throw HWError.Abort("Authentication failure")
                    }
                }
                
                if bleViewModel?.type == .Jade {
                    do {
                        // check firmware
                        let res = try await bleViewModel?.checkFirmware()
                        if let version = res?.0, let lastFirmware = res?.1 {
                            onCheckFirmware(version: version, lastFirmware: lastFirmware)
                            return
                        }
                    } catch {
                        print ("No new firmware found")
                    }
                }
                progress("id_logging_in".localized)
                self.account = try await bleViewModel?.login(account: account)
                onLogin(item)
            } catch {
                try? await bleViewModel?.disconnect()
                onError(error)
            }
        }
    }

    @MainActor
    func onLogin(_ item: ScanListItem) {
        print("account.uuid \(account.uuid?.description ?? "")")
        print("peripheral.identifier \(item.identifier)")
        account.uuid = item.identifier
        AccountsRepository.shared.upsert(account)
        AnalyticsManager.shared.hwwConnected(account: account)
        _ = AccountNavigator.goLogged(nv: navigationController)
    }

    @MainActor
    func onCheckFirmware(version: JadeVersionInfo, lastFirmware: Firmware) {
        let storyboard = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "UpdateFirmwareViewController") as? UpdateFirmwareViewController {
            vc.firmware = lastFirmware
            vc.version = version.jadeVersion
            vc.delegate = self
            vc.modalPresentationStyle = .overFullScreen
            self.present(vc, animated: false, completion: nil)
        }
    }

    func loadNavigationBtns() {
        // Troubleshoot
        let settingsBtn = UIButton(type: .system)
        settingsBtn.titleLabel?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        settingsBtn.tintColor = UIColor.gGreenMatrix()
        settingsBtn.setTitle("id_troubleshoot".localized, for: .normal)
        settingsBtn.addTarget(self, action: #selector(troubleshootBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
    }

    @objc func troubleshootBtnTapped() {
        SafeNavigationManager.shared.navigate( ExternalUrls.jadeTroubleshoot )
    }

    @IBAction func retryBtnTapped(_ sender: Any) {
        Task {
            await scanViewModel?.stopScan()
            setContent()
            startScan()
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        scanCancellable = scanViewModel?.objectWillChange.sink(receiveValue: { [weak self] in
            DispatchQueue.main.async {
                if self?.selectedItem != nil { return }
                 if let item = self?.scanViewModel?.peripherals.filter({ $0.identifier == self?.account.uuid || $0.name == self?.account.name }).first {
                    self?.selectedItem = item
                    self?.onScannedDevice(item)
                }
            }
        })
        scanViewModel?.centralManager.eventPublisher
            .sink {
                switch $0 {
                case .didUpdateState(let state):
                    switch state {
                    case .poweredOn:
                        DispatchQueue.main.async {
                            self.progress("id_looking_for_device".localized)
                            self.startScan()
                        }
                    case .poweredOff:
                        DispatchQueue.main.async {
                            self.progress("id_enable_bluetooth".localized)
                            self.stopScan()
                        }
                    default:
                        break
                    }
                default:
                    break
                }
            }
            .store(in: &cancellables)
        startScan()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        activeToken = NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main, using: applicationDidBecomeActive)
        resignToken = NotificationCenter.default.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main, using: applicationWillResignActive)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if let token = activeToken {
            NotificationCenter.default.removeObserver(token)
        }
        if let token = resignToken {
            NotificationCenter.default.removeObserver(token)
        }
        stopScan()
    }

    @MainActor
    func startScan() {
        Task {
            selectedItem = nil
            do {
                try await scanViewModel?.scan(deviceType: self.account.isJade ? .Jade : .Ledger)
                self.progress("id_looking_for_device".localized)
            } catch {
                switch error {
                case BluetoothError.bluetoothUnavailable:
                    progress("id_enable_bluetooth".localized)
                default:
                    progress(error.localizedDescription)
                }
            }
        }
    }
    
    @MainActor
    func stopScan() {
        Task {
            await scanViewModel?.stopScan()
            scanCancellable?.cancel()
            cancellables.forEach { $0.cancel() }
        }
    }

    func applicationDidBecomeActive(_ notification: Notification) {
        print("applicationDidBecomeActive")
        pairingState = .paired
        start()
    }

    func applicationWillResignActive(_ notification: Notification) {
        print("applicationWillResignActive")
        pairingState = .pairing
        stop()
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblTitle.textColor = .white
    }

    @MainActor
    func start() {
        loaderPlaceholder.addSubview(loadingIndicator)

        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor
                .constraint(equalTo: loaderPlaceholder.centerXAnchor),
            loadingIndicator.centerYAnchor
                .constraint(equalTo: loaderPlaceholder.centerYAnchor),
            loadingIndicator.widthAnchor
                .constraint(equalToConstant: loaderPlaceholder.frame.width),
            loadingIndicator.heightAnchor
                .constraint(equalTo: loaderPlaceholder.widthAnchor)
        ])

        loadingIndicator.isAnimating = true
    }

    @MainActor
    func stop() {
        loadingIndicator.isAnimating = false
    }

    @MainActor
    func progress(_ txt: String) {
        self.lblTitle.text = txt
    }

    @MainActor
    override func onError(_ err: Error) {
        stop()
        retryButton.isHidden = false
        let txt = bleViewModel?.toBleError(err, network: nil).localizedDescription ?? "id_operation_failure"
        lblTitle.text = txt.localized
        image.image = UIImage(named: "il_connection_fail")
        print ("error: \(txt)")
    }
}

extension ConnectViewController: UpdateFirmwareViewControllerDelegate {
    @MainActor
    func didUpdate(version: String, firmware: Firmware) {
        Task {
            do {
                startLoader(message: "id_updating_firmware".localized)
                let binary = try await bleViewModel?.fetchFirmware(firmware: firmware)
                let hash = bleViewModel?.jade?.bleJade.sha256(binary ?? Data())
                let hashHex = hash?.hex.separated(by: " ", every: 8)
                let text = progressLoaderMessage(title: "id_updating_firmware".localized,
                                                 subtitle: "Hash: \(hashHex ?? "")")
                startLoader(message: text)
                let res = try await bleViewModel?.updateFirmware(firmware: firmware, binary: binary ?? Data())
                try await bleViewModel?.disconnect()
                try await Task.sleep(nanoseconds:  5 * 1_000_000_000)
                await MainActor.run {
                    self.stopLoader()
                    if let res = res, res {
                        DropAlert().success(message: "id_firmware_update_completed".localized)
                        startScan()
                    } else {
                        DropAlert().error(message: "id_operation_failure".localized)
                    }
                }
            } catch {
                self.stopLoader()
                onError(error)
            }
        }
    }

    @MainActor
    func didSkip() {
        Task {
            progress("id_logging_in".localized)
            self.account = try await bleViewModel?.login(account: account)
            if let item = selectedItem {
                onLogin(item)
            }
        }
    }
}
