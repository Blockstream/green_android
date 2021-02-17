import UIKit
import PromiseKit
import RxSwift
import RxBluetoothKit
import CoreBluetooth

class HardwareWalletScanViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var radarImageView: RadarImageView!

    var peripherals = [ScannedPeripheral]()

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.dataSource = self
        tableView.delegate = self
        tableView.tableFooterView = UIView()

        BLEManager.shared.delegate = self
        BLEManager.shared.start()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        radarImageView.startSpinning()
    }

    override func viewWillDisappear(_ animated: Bool) {
        BLEManager.shared.disposeScan()
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
        BLEManager.shared.prepare(peripheral)
    }
}

extension HardwareWalletScanViewController: BLEManagerDelegate {
    func onConnectivityChange(peripheral: Peripheral, status: Bool) {

    }

    func onError(_ error: BLEManagerError) {

        self.stopAnimating()
        switch error {
        case .powerOff(let txt):
            showError(txt)
        case .notReady(let txt):
            showError(txt)
        case .scanErr(let txt):
            showError(txt)
        case .bleErr(let txt):
            showError(txt)
        case .timeoutErr(let txt):
            showError(txt)
        case .dashboardErr(let txt):
            showError(txt)
        case .outdatedAppErr(let txt):
            showError(txt)
        case .wrongAppErr(let txt):
            showError(txt)
        case .authErr(let txt):
            showError(txt)
        case .swErr(let txt):
            showError(txt)
        case .genericErr(let txt):
            showError(txt)
        }
    }

    func ota(_ peripheral: Peripheral) {
        /*startAnimating()
        HWResolver.shared.hw = Jade.shared
        let appDelegate = UIApplication.shared.delegate as? AppDelegate

        enstablishDispose = Observable.just(peripheral)
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { p -> Observable<Peripheral> in
                if p.isConnected {
                    return Observable.just(peripheral)
                }
                return p.establishConnection()
            }.flatMap {
                Jade.shared.open($0)
            }.compactMap { _ in
                appDelegate?.disconnect()
                try appDelegate?.connect()
            }.flatMap { _ in
                Jade.shared.auth()
            }.flatMap { _ in
                Jade.shared.ota()
            }.observeOn(MainScheduler.instance)
            .subscribe(onNext: { res in
                self.stopAnimating()
                print("res: \(res)")
            }, onError: { err in
                self.stopAnimating()
                print("err: \(err)")
            })*/
    }

    func didUpdatePeripherals(_ peripherals: [ScannedPeripheral]) {
        self.peripherals = peripherals
        tableView.reloadData()
    }

    func onConnect(_ peripheral: Peripheral) {
        self.stopAnimating()
        getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
    }

    func onPrepare(_ peripheral: Peripheral) {
        let alert = UIAlertController(title: NSLocalizedString("WELCOME TO JADE", comment: ""), message: "", preferredStyle: .actionSheet)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .cancel) { _ in
            self.startAnimating()
            BLEManager.shared.dispose()
            BLEManager.manager.manager.cancelPeripheralConnection(peripheral.peripheral)
            BLEManager.shared.connect(peripheral)
            if BLEManager.shared.isJade(peripheral) {
                DropAlert().info(message: NSLocalizedString("id_hardware_wallet_check_ready", comment: ""))
            }
        })
        self.present(alert, animated: true, completion: nil)
    }
}
