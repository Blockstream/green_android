import UIKit
import gdk
import hw

class PinCreateViewController: HWFlowBaseViewController {

    @IBOutlet weak var imgDevice: UIImageView!

    @IBOutlet weak var lblStepNumber: UILabel!
    @IBOutlet weak var lblStepTitle: UILabel!
    @IBOutlet weak var lblStepHint: UILabel!
    @IBOutlet weak var lblWarn: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!
    @IBOutlet weak var btnContinue: UIButton!

    var remember = false
    var testnet = false
    var bleViewModel: BleViewModel?
    var scanViewModel: ScanViewModel?
    var account: Account?

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

    deinit {
        print("Deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        loadingIndicator.isAnimating = true
    }

    override func viewWillDisappear(_ animated: Bool) {
        stop()
    }

    func setContent() {
        lblStepNumber.text = "SETUP YOUR JADE"
        lblStepTitle.text = "Create a PIN"
        lblStepHint.text = "Enter and confirm a unique PIN that will be entered to unlock Jade."
        lblWarn.text = "If you forget your PIN, you will need to restore with your recovery phrase"
        btnContinue.setTitle("id_continue".localized, for: .normal)
    }

    func loadNavigationBtns() {
        let settingsBtn = UIButton(type: .system)
        settingsBtn.titleLabel?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        settingsBtn.tintColor = UIColor.gGreenMatrix()
        settingsBtn.setTitle("id_setup_guide".localized, for: .normal)
        settingsBtn.addTarget(self, action: #selector(setupBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
    }

    func setStyle() {
        [lblStepNumber].forEach {
            $0?.font = UIFont.systemFont(ofSize: 12.0, weight: .black)
            $0?.textColor = UIColor.gGreenMatrix()
        }
        [lblStepTitle].forEach {
            $0?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
            $0?.textColor = .white
        }
        [lblStepHint, lblWarn].forEach {
            $0?.font = UIFont.systemFont(ofSize: 12.0, weight: .regular)
            $0?.textColor = .white.withAlphaComponent(0.6)
        }
        btnContinue.setStyle(.primary)
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
        loadingIndicator.isHidden = false
    }

    func stop() {
        loadingIndicator.isAnimating = false
        loadingIndicator.isHidden = true
    }

    @IBAction func continueBtnTapped(_ sender: Any) {
        btnContinue.isHidden = true
        start()
        Task {
            do {
                try await bleViewModel?.connect()
                try await bleViewModel?.initialize(testnet: testnet)
                if let account = try await bleViewModel?.defaultAccount() {
                    self.account = try await bleViewModel?.login(account: account)
                }
                // check firmware
                let res = try? await bleViewModel?.checkFirmware()
                if let version = res?.0, let lastFirmware = res?.1 {
                    onCheckFirmware(version: version, lastFirmware: lastFirmware)
                    return
                } else {
                    next()
                }
            } catch {
                onError(error)
            }
        }
    }

    @objc func setupBtnTapped() {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "SetupJadeViewController") as? SetupJadeViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
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

    @MainActor
    func next() {
        account?.hidden = !remember
        if let account = account {
            AccountsRepository.shared.current = account
            AnalyticsManager.shared.loginWalletEnd(account: account, loginType: .hardware)
            _ = AccountNavigator.goLogged(nv: navigationController)
        }
    }

    @MainActor
    override func onError(_ err: Error) {
        btnContinue.isHidden = false
        let txt = bleViewModel?.toBleError(err, network: nil).localizedDescription
        showError(txt?.localized ?? "")
        Task { try? await bleViewModel?.disconnect() }
    }
}

extension PinCreateViewController: UpdateFirmwareViewControllerDelegate {
    @MainActor
    func didUpdate(version: String, firmware: Firmware) {
        AnalyticsManager.shared.otaStartJade(account: AccountsRepository.shared.current, firmware: firmware)
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
                self.stopLoader()
                await MainActor.run {
                    btnContinue.isHidden = false
                    if let res = res, res {
                        AnalyticsManager.shared.otaCompleteJade(account: AccountsRepository.shared.current, firmware: firmware)
                        DropAlert().success(message: "id_firmware_update_completed".localized)
                        connectViewController()
                    } else {
                        DropAlert().error(message: "id_operation_failure".localized)
                    }
                }
            } catch {
                onError(error)
            }
        }
    }

    func connectViewController() {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "ConnectViewController") as? ConnectViewController {
            vc.account = account
            vc.bleViewModel = bleViewModel
            vc.scanViewModel = scanViewModel
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    func didSkip() {
        self.next()
    }
}
