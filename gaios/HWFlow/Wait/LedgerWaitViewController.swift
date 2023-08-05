import UIKit
import CoreBluetooth
import AsyncBluetooth
import Combine

class LedgerWaitViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!
    
    var scanViewModel: ScanViewModel?
    private var scanCancellable: AnyCancellable?
    private var activeToken, resignToken: NSObjectProtocol?
    private var selectedItem: ScanListItem?
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
    }

    deinit {
        print("Deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        activeToken = NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main, using: applicationDidBecomeActive)
        resignToken = NotificationCenter.default.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main, using: applicationWillResignActive)
        start()
        startScan()
        scanViewModel?.centralManager.eventPublisher
            .sink { [weak self] in
                switch $0 {
                case .didUpdateState(let state):
                    DispatchQueue.main.async {
                        self?.onCentralManagerUpdateState(state)
                    }
                default:
                    break
                }
            }.store(in: &cancellables)
        scanCancellable = scanViewModel?.objectWillChange.sink(receiveValue: { [weak self] in
            DispatchQueue.main.async {
                self?.onUpdateScanViewModel()
            }
        })
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        if let token = activeToken {
            NotificationCenter.default.removeObserver(token)
        }
        if let token = resignToken {
            NotificationCenter.default.removeObserver(token)
        }
        stopScan()
        scanCancellable?.cancel()
        cancellables.forEach { $0.cancel() }
    }

    @MainActor
    func onUpdateScanViewModel() {
        if selectedItem != nil { return }
        if let peripheral = scanViewModel?.peripherals.filter({ $0.ledger }).first {
            selectedItem = peripheral
            stopScan()
            next()
        }
    }

    @MainActor
    func onCentralManagerUpdateState(_ state: CBManagerState) {
        switch state {
        case .poweredOn:
            startScan()
        case .poweredOff:
            showAlert(title: "id_error".localized, message: "id_enable_bluetooth".localized)
            stopScan()
        default:
            break
        }
    }

    @MainActor
    func startScan() {
        Task {
            selectedItem = nil
            do {
                try await scanViewModel?.scan(deviceType: .Ledger)
            } catch {
                switch error {
                case BluetoothError.bluetoothUnavailable:
                    showAlert(title: "id_error".localized, message: "id_enable_bluetooth".localized)
                default:
                    showAlert(title: "id_error".localized, message: error.localizedDescription)
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
        lblTitle.text = "id_follow_the_instructions_of_your".localized
        lblHint.text = "id_please_follow_the_instructions".localized
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblHint.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        lblTitle.textColor = .white
        lblHint.textColor = .white.withAlphaComponent(0.6)
    }

    func applicationDidBecomeActive(_ notification: Notification) {
        start()
    }

    func applicationWillResignActive(_ notification: Notification) {
        stop()
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

    func next() {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "ScanViewController") as? ScanViewController {
            vc.deviceType = .Ledger
            vc.scanViewModel = scanViewModel
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }
}
