import UIKit
import PromiseKit
import RxSwift
import RxBluetoothKit

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

        startAnimating()
        BLEManager.shared.connect(peripheral: peripheral)
    }
}

extension HardwareWalletScanViewController: BLEManagerDelegate {
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

    func didUpdatePeripherals(_ peripherals: [ScannedPeripheral]) {
        self.peripherals = peripherals
        tableView.reloadData()
    }

    func onConnect() {
        self.stopAnimating()
        getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
    }
}
