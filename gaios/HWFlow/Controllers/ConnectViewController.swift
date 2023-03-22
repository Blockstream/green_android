import UIKit
import RxBluetoothKit
import RxSwift

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

    private var activeToken, resignToken: NSObjectProtocol?
    private var pairingState: PairingState = .unknown
    private var peripheral: Peripheral?

    let loadingIndicator: ProgressView = {
        let progress = ProgressView(colors: [UIColor.customMatrixGreen()], lineWidth: 2)
        progress.translatesAutoresizingMaskIntoConstraints = false
        return progress
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
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
        print("sss")
    }

    @IBAction func retryBtnTapped(_ sender: Any) {
        BLEViewModel.shared.dispose()
        scan()
    }

    override func viewDidAppear(_ animated: Bool) {
        activeToken = NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main, using: applicationDidBecomeActive)
        resignToken = NotificationCenter.default.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main, using: applicationWillResignActive)
        scan()
    }

    override func viewWillDisappear(_ animated: Bool) {
        if let token = activeToken {
            NotificationCenter.default.removeObserver(token)
        }
        if let token = resignToken {
            NotificationCenter.default.removeObserver(token)
        }
        stop()
        BLEViewModel.shared.scanDispose?.dispose()
    }

    func setContent() {
        if account.isJade {
            image.image = UIImage(named: "il_jade_unlock")
        } else {
            image.image = UIImage(named: "il_ledger")
        }
        retryButton.isHidden = true
        retryButton.setTitle("Retry".localized, for: .normal)
        retryButton.setStyle(.primary)
    }

    func applicationDidBecomeActive(_ notification: Notification) {
        print("applicationDidBecomeActive")
        self.pairingState = .paired
        start()
    }

    func applicationWillResignActive(_ notification: Notification) {
        print("applicationWillResignActive")
        self.pairingState = .pairing
        stop()
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblTitle.textColor = .white
        lblTitle.text = ""
    }

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

    func stop() {
        loadingIndicator.isAnimating = false
    }

    func next(_ peripheral: Peripheral) {
        print("account.uuid \(account.uuid!)")
        print("peripheral.identifier \(peripheral.identifier)")
        account.uuid = peripheral.identifier
        AccountsRepository.shared.upsert(account)
        AccountNavigator.goLogged(account: account, nv: navigationController)
    }

    override func error(_ err: Error) {
        stop()
        BLEViewModel.shared.dispose()
        retryButton.isHidden = false
        let bleError = BLEManager.shared.toBleError(err, network: nil)
        let txt = BLEManager.shared.toErrorString(bleError)
        lblTitle.text = txt
        image.image = UIImage(named: "il_connection_fail")
    }

    func scan() {
        start()
        setContent()
        self.progress(account.isJade ? "Power on Jade".localized : "id_please_follow_the_instructions".localized)
        BLEViewModel.shared.scan(jade: account.isJade,
                                 completion: { self.connect($0) },
                                 error: self.error)
    }

    func progress(_ txt: String) {
        DispatchQueue.main.async {
            self.lblTitle.text = txt
        }
    }

    func connect(_ peripherals: [Peripheral]) {
        let peripheral = peripherals.filter { $0.peripheral.identifier == account.uuid || $0.peripheral.name == account.name }.first
        guard let peripheral = peripheral else { return }
        self.progress("id_connecting".localized)
        var timer: Timer?
        BLEViewModel.shared.pairing(peripheral,
                                    completion: { _ in
            timer = Timer.scheduledTimer(withTimeInterval: 3.0,
                                         repeats: true) {_ in
                print("pairingState \(self.pairingState)")
                if self.pairingState == .unknown {
                    timer?.invalidate()
                    self.login(peripheral)
                } else if self.pairingState == .paired {
                    timer?.invalidate()
                    self.pairingState = .unknown
                    BLEViewModel.shared.dispose()
                    BLEManager.shared.manager.manager.cancelPeripheralConnection(peripheral.peripheral)
                    self.scan()
                }
            }
        }, error: self.error)
    }

    func login(_ peripheral: Peripheral) {
        self.peripheral = peripheral
        BLEViewModel.shared.login(account: account,
                                  peripheral: peripheral,
                                  progress: { self.progress(self.account.isJade ? $0 : "id_logging_in".localized) },
                                  completion: { peripheral.isJade() ? self.jadeFirmwareUpgrade() : self.next(peripheral) },
                                  error: self.error)
    }
}

extension ConnectViewController: UpdateFirmwareViewControllerDelegate {
    func didUpdate(version: String, firmware: Firmware) {
        startLoader(message: "id_updating_firmware".localized)
        let repair = version <= "0.1.30" && firmware.version >= "0.1.31"
        BLEViewModel.shared.updateFirmware(
            peripheral: self.peripheral!,
            firmware: firmware,
            progress: { self.startLoader(message: self.progressLoaderMessage(title: $0, subtitle: $1)) },
            completion: {
                self.stopLoader()
                if repair {
                    self.showAlert(title: "id_firmware_update_completed".localized, message: "id_new_jade_firmware_required".localized)
                }
                if $0 {
                    DropAlert().success(message: "id_firmware_update_completed".localized)
                } else {
                    DropAlert().error(message: "id_operation_failure".localized)
                }
                BLEViewModel.shared.dispose()
                self.scan()
            },
            error: { _ in self.stopLoader(); DropAlert().error(message: "id_operation_failure".localized) })
    }

    func didSkip() {
        self.next(self.peripheral!)
    }

    func progressLoaderMessage(title: String, subtitle: String) -> NSMutableAttributedString {
        let titleAttributes: [NSAttributedString.Key: Any] = [
            .foregroundColor: UIColor.white
        ]
        let hashAttributes: [NSAttributedString.Key: Any] = [
            .foregroundColor: UIColor.customGrayLight(),
            .font: UIFont.systemFont(ofSize: 16)
        ]
        let hint = "\n\n" + subtitle
        let attributedTitleString = NSMutableAttributedString(string: title)
        attributedTitleString.setAttributes(titleAttributes, for: title)
        let attributedHintString = NSMutableAttributedString(string: hint)
        attributedHintString.setAttributes(hashAttributes, for: hint)
        attributedTitleString.append(attributedHintString)
        return attributedTitleString
    }

    func jadeFirmwareUpgrade() {
        _ = BLEViewModel.shared.checkFirmware(Jade.shared.peripheral)
            .subscribe(onNext: { (version, lastFirmware) in
                guard let version = version, let lastFirmware = lastFirmware else { return }
                let storyboard = UIStoryboard(name: "HWFlow", bundle: nil)
                if let vc = storyboard.instantiateViewController(withIdentifier: "UpdateFirmwareViewController") as? UpdateFirmwareViewController {
                    vc.firmware = lastFirmware
                    vc.version = version
                    vc.delegate = self
                    vc.modalPresentationStyle = .overFullScreen
                    self.present(vc, animated: false, completion: nil)
                }
            }, onError: { _ in self.next(self.peripheral!)})
    }
}
