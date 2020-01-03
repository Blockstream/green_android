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
        enstablishDispose = peripheral.establishConnection()
            .timeoutIfNoEvent(self.timeout)
            .flatMap { Ledger.shared.open($0) }
            .subscribe(onNext: { _ in
                UserDefaults.standard.set(peripheral.name, forKey: "paired_device_name")
                UserDefaults.standard.set(peripheral.identifier.uuidString, forKey: "paired_device_uuid")
                self.navigationController?.popViewController(animated: true)
            }, onError: { err in
                print(err.localizedDescription)
                let alert = UIAlertController(title: "Select your paired device", message: "Pair yout Ledger Nano X with Ledger Live app and retry. Error: \(err.localizedDescription)", preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel))
                self.present(alert, animated: true, completion: nil)
            })
    }
}
