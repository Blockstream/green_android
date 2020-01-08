import UIKit
import PromiseKit
import RxSwift
import RxBluetoothKit

class HardwareWalletScanViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var radarImageView: RadarImageView!

    let manager = CentralManager(queue: .main)
    let timeout = RxTimeInterval.seconds(10)
    var peripherals = [ScannedPeripheral]()

    var scanningDispose: Disposable?
    var enstablishDispose: Disposable?

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.dataSource = self
        tableView.delegate = self
        tableView.tableFooterView = UIView()
        self.peripherals = []

        scanningDispose = self.manager.observeState()
            .filter { $0 == .poweredOn }
            .take(1)
            .flatMap { _ in self.manager.scanForPeripherals(withServices: nil) }
            .filter { $0.peripheral.name?.contains("Nano") ?? false }
            .do(onNext: { print($0.peripheral.name ?? "") })
            .subscribe(onNext: { p in
                let isContained = !self.peripherals.filter { $0.rssi == p.rssi }.isEmpty
                if !isContained {
                    self.peripherals.append(p)
                    self.tableView.reloadData()
                }
            }, onError: { err in
                print(err.localizedDescription)
                let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: err.localizedDescription, preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel))
                self.present(alert, animated: true, completion: nil)
            })
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        radarImageView.startSpinning()
    }

    deinit {
        scanningDispose?.dispose()
        enstablishDispose?.dispose()
    }
}

extension HardwareWalletScanViewController: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return peripherals.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if let cell = tableView.dequeueReusableCell(withIdentifier: "HardwareDeviceCell",
                                                    for: indexPath as IndexPath) as? HardwareDeviceCell {
            let p = peripherals[indexPath.row]
            cell.nameLabel.text = p.advertisementData.localName
            cell.connectionStatusLabel.text = p.peripheral.identifier.uuidString == UserDefaults.standard.string(forKey: "paired_device_uuid") ? "Current selected" : ""
            cell.accessoryType = p.advertisementData.isConnectable ?? false ? .disclosureIndicator : .none
            return cell
        }
        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let peripheral = peripherals[indexPath.row].peripheral
        enstablishDispose?.dispose()
        self.connect(peripheral: peripheral)
    }
}

extension HardwareWalletScanViewController {

    enum DeviceError: Error {
        case dashboard
        case wrong_app
    }

    func network() -> String {
        return getGdkNetwork(getNetwork()).network.lowercased() == "testnet" ? "Bitcoin Test" : "Bitcoin"
    }

    func connect(peripheral: Peripheral) {
        _ = peripheral.establishConnection()
            .timeoutIfNoEvent(self.timeout)
            .flatMap { Ledger.shared.open($0) }
            .timeoutIfNoEvent(self.timeout)
            .flatMap { _ in Ledger.shared.application() }
            .flatMap { res -> Observable<Bool> in
                let name = res["name"] as? String
                if name!.contains("OLOS") {
                    // open app from dashboard
                    return Observable<Bool>.error(DeviceError.dashboard)
                } else if name! != self.network() {
                    // change app
                    return Observable<Bool>.error(DeviceError.wrong_app)
                }
                // correct open app
                return Observable.just(true)
            }
            .subscribe(onNext: { _ in
                print("Login on progress")
                self.login()
            }, onError: { err in
                if err as? RxError != nil {
                    print("Turn on your Ledger and try again")
                } else if let derr = err as? DeviceError {
                    if derr == .dashboard {
                        print("Open \(self.network()) app on your Ledger")
                    } else if derr == .wrong_app {
                        print("Quit current app and open \(self.network()) app on your Ledger")
                    }
                }
            }, onCompleted: {}, onDisposed: {})
    }

    func login() {
        let bgq = DispatchQueue.global(qos: .background)
        let session = getGAService().getSession()
        let appDelegate = getAppDelegate()!
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try appDelegate.connect()
        }.compactMap(on: bgq) { _ -> TwoFactorCall in
            return try session.registerUser(mnemonic: "", hw_device: ["device": (Ledger.shared.hwDevice as Any) ])
        }.then(on: bgq) { call in
            call.resolve()
        }.compactMap(on: bgq) {_ -> TwoFactorCall in
            try session.login(mnemonic: "", hw_device: ["device": Ledger.shared.hwDevice])
        }.then(on: bgq) { call in
            call.resolve()
        }.ensure {
            print("ensure")
            self.stopAnimating()
        }.done { _ in
            print("done")
            appDelegate.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
        }.catch { e in
            print("error \(e.localizedDescription)")
        }
    }
}

extension Observable {
    func timeoutIfNoEvent(_ dueTime: RxTimeInterval) -> Observable<Element> {
        let timeout = Observable
            .never()
            .timeout(dueTime, scheduler: MainScheduler.instance)

        return self.amb(timeout)
    }
}
