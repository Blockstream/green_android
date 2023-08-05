import UIKit
import CoreBluetooth
import AsyncBluetooth
import Combine

class ScanViewController: HWFlowBaseViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var btnTroubleshoot: UIButton!
    private var scanCancellable: AnyCancellable?
    private var cancellables = Set<AnyCancellable>()
    var deviceType = DeviceType.Jade

    var scanViewModel: ScanViewModel!

    override func viewDidLoad() {
        super.viewDidLoad()

        ["JadeDeviceCell", "OtherDeviceCell"].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
        if deviceType == .Jade {
            loadNavigationBtns()
        }
        setContent()
        setStyle()
    }

    deinit {
        print("Deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        startScan()
        scanCancellable = scanViewModel.objectWillChange.sink(receiveValue: { [weak self] in
            DispatchQueue.main.async {
                self?.tableView.reloadData()
            }
        })
        scanViewModel?.centralManager.eventPublisher
            .sink {
                switch $0 {
                case .didUpdateState(let state):
                    DispatchQueue.main.async {
                        self.onCentralManagerUpdateState(state)
                    }
                default:
                    break
                }
            }
            .store(in: &cancellables)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        stopScan()
        scanCancellable?.cancel()
        cancellables.forEach { $0.cancel() }
    }

    @MainActor
    func onCentralManagerUpdateState(_ state: CBManagerState) {
        switch state {
        case .poweredOn:
            startScan()
        case .poweredOff:
            showAlert(title: "id_error".localized, message: "id_enable_bluetooth".localized)
            scanViewModel?.reset()
            stopScan()
        default:
            break
        }
    }

    @MainActor
    func startScan() {
        Task {
            do {
                scanViewModel?.reset()
                try await scanViewModel?.scan(deviceType: deviceType)
            } catch {
                switch error {
                case BluetoothError.bluetoothUnavailable:
                    self.showAlert(title: "id_error".localized, message: "id_enable_bluetooth".localized)
                default:
                    self.showAlert(title: "id_error".localized, message: error.localizedDescription)
                }
            }
        }
    }
    
    @MainActor
    func stopScan() {
        Task {
            await scanViewModel?.stopScan()
        }
    }

    func setContent() {
        title = "".localized
        btnTroubleshoot.setTitle("id_troubleshoot".localized, for: .normal)
        btnTroubleshoot.isHidden = deviceType != .Jade
    }

    func setStyle() {
        btnTroubleshoot.setStyle(.inline)
    }

    func next() {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "PairingSuccessViewController") as? PairingSuccessViewController {
            vc.bleViewModel = BleViewModel.shared
            vc.scanViewModel = scanViewModel
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    func loadNavigationBtns() {
        let settingsBtn = UIButton(type: .system)
        settingsBtn.titleLabel?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        settingsBtn.tintColor = UIColor.gGreenMatrix()
        settingsBtn.setTitle("id_setup_guide".localized, for: .normal)
        settingsBtn.addTarget(self, action: #selector(setupBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
    }

    @objc func setupBtnTapped() {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "SetupJadeViewController") as? SetupJadeViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func btnTroubleshoot(_ sender: Any) {
        SafeNavigationManager.shared.navigate( ExternalUrls.jadeTroubleshoot )
    }
}

extension ScanViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return scanViewModel.peripherals.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let peripheral = scanViewModel.peripherals[indexPath.row]
        if peripheral.jade {
            if let cell = tableView.dequeueReusableCell(withIdentifier: JadeDeviceCell.identifier, for: indexPath) as? JadeDeviceCell {
                cell.configure(text: peripheral.name)
                cell.selectionStyle = .none
                return cell
            }
        } else {
            if let cell = tableView.dequeueReusableCell(withIdentifier: OtherDeviceCell.identifier, for: indexPath) as? OtherDeviceCell {
                cell.configure(text: peripheral.name)
                cell.selectionStyle = .none
                return cell
            }
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 0.1
    }

    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0.1
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return UITableView.automaticDimension
    }

    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return nil
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let peripheral = scanViewModel.peripherals[indexPath.row]
        stopScan()
        Task {
            BleViewModel.shared.type = peripheral.type
            BleViewModel.shared.peripheralID = peripheral.identifier
            try? await BleViewModel.shared.connect()
            await MainActor.run { self.next() }
        }
    }
}
