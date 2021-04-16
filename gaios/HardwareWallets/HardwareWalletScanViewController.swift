import UIKit
import PromiseKit
import RxSwift
import RxBluetoothKit
import CoreBluetooth

class HardwareWalletScanViewController: UIViewController {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var radarImageView: RadarImageView!

    var account: Account?
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
        radarImageView.stopSpinning()
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
        self.startAnimating()
        BLEManager.shared.prepare(peripheral)
    }

    func connect(_ peripheral: Peripheral, network: String) {
        account?.network = network
        AccountsManager.shared.current = account
        if BLEManager.shared.isLedger(peripheral) {
            BLEManager.shared.connect(peripheral, network: network)
            return
        }
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            BLEManager.shared.dispose()
            BLEManager.manager.manager.cancelPeripheralConnection(peripheral.peripheral)
            return Guarantee()
        }.then(on: bgq) {
            after(seconds: 1)
        }.done { _ in
            BLEManager.shared.connect(peripheral, network: network)
            DropAlert().info(message: NSLocalizedString("id_please_follow_the_instructions", comment: ""))
        }
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

    func didUpdatePeripherals(_ peripherals: [ScannedPeripheral]) {
        self.peripherals = peripherals
        tableView.reloadData()
    }

    func onConnect(_ peripheral: Peripheral) {
        self.stopAnimating()
        getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
    }

    func onPrepare(_ peripheral: Peripheral) {
        stopAnimating()
        let alert = UIAlertController(title: NSLocalizedString("id_login", comment: ""), message: "", preferredStyle: .actionSheet)
        if BLEManager.shared.isJade(peripheral) {
            alert.addAction(UIAlertAction(title: NSLocalizedString("Connect Liquid", comment: ""), style: .default) { _ in self.connect(peripheral, network: "liquid") })
        }
        alert.addAction(UIAlertAction(title: NSLocalizedString("Connect Mainnet", comment: ""), style: .default) {  _ in self.connect(peripheral, network: "mainnet") })
        alert.addAction(UIAlertAction(title: NSLocalizedString("Connect Testnet", comment: ""), style: .default) { _ in self.connect(peripheral, network: "testnet") })
        self.present(alert, animated: true, completion: nil)
    }

    func onCheckFirmware(_ peripheral: Peripheral, fw: [String: String], currentVersion: String) {
        stopAnimating()
        let notRequired = Jade.shared.isJadeFwValid(currentVersion)
        let alert = UIAlertController(title: notRequired ? NSLocalizedString("id_new_jade_firmware_available", comment: "") : NSLocalizedString("id_new_jade_firmware_required", comment: ""),
                                      message: String(format: NSLocalizedString("id_version_1s", comment: ""), fw["version"] ?? ""),
                                      preferredStyle: .actionSheet)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_update", comment: ""), style: .default) { _ in
            self.startAnimating()
            BLEManager.shared.updateFirmware(peripheral, fwFile: fw)
        })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in
            if notRequired {
                let network = AccountsManager.shared.current?.network ?? "mainnet"
                BLEManager.shared.login(peripheral, network: network)
            } else {
                BLEManager.shared.dispose()
            }
        })
        self.present(alert, animated: true, completion: nil)
    }

    func onUpdateFirmware(_ peripheral: Peripheral) {
        stopAnimating()
        let alert = UIAlertController(title: NSLocalizedString("id_firmware_update_completed", comment: ""), message: NSLocalizedString("id_select_your_jade_to_login", comment: ""), preferredStyle: .actionSheet)
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .cancel) { _ in })
        self.present(alert, animated: true, completion: nil)
    }

}
