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
            cell.nameLabel.text = p.name
            cell.connectionStatusLabel.text = p.peripheral.identifier.uuidString == UserDefaults.standard.string(forKey: "paired_device_uuid") ? "Current selected" : ""
            cell.accessoryType = p.isConnected ? .disclosureIndicator : .none
            cell.peripheral = p
            cell.delegate = self
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

    func connectJade(peripheral: Peripheral) {
        startAnimating()
        HWResolver.shared.hw = Jade.shared

        if peripheral.isConnected {
            self.reconnectJade(peripheral: peripheral)
        }

        // dummy 1st connection
        let dispose = peripheral.establishConnection().subscribe()
        _ = manager.observeConnect(for: peripheral).take(1).compactMap { _ in sleep(1) }
            .subscribe(onNext: { x in
                let alert = UIAlertController(title: NSLocalizedString("WELCOME TO JADE", comment: ""), message: "", preferredStyle: .actionSheet)
                alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .cancel) { _ in
                    dispose.dispose()
                    self.manager.manager.cancelPeripheralConnection(peripheral.peripheral)
                    self.reconnectJade(peripheral: peripheral)
                })
                self.present(alert, animated: true, completion: nil)
        })
    }

    func reconnectJade(peripheral: Peripheral) {
        let appDelegate = UIApplication.shared.delegate as? AppDelegate
        let session = getGAService().getSession()
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
                Jade.shared.version()
            }.flatMap { version -> Observable<[String: Any]> in
                let hasPin = version["JADE_HAS_PIN"] as? Bool
                // check genuine firmware
                return Jade.shared.addEntropy()
            }.flatMap { _ in
                Jade.shared.auth(network: getNetwork())
            }.compactMap { _ in
                _ = try session.registerUser(mnemonic: "", hw_device: ["device": (Jade.shared.hwDevice as Any) ]).resolve().wait()
                _ = try session.login(mnemonic: "", hw_device: ["device": Jade.shared.hwDevice]).resolve().wait()
            }.observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in
                self.stopAnimating()
                getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
            }, onError: { err in
                self.stopAnimating()
                switch err {
                case is BluetoothError:
                    let bleErr = err as? BluetoothError
                    self.showError(NSLocalizedString("id_communication_timed_out_make", comment: "") + ": \(bleErr?.description ?? "")")
                case RxError.timeout:
                    self.showError(NSLocalizedString("id_communication_timed_out_make", comment: ""))
                case DeviceError.dashboard:
                    self.showError(String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.network()))
                case DeviceError.wrong_app:
                self.showError(String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.network()))
                case is AuthenticationTypeHandler.AuthError:
                    let authErr = err as? AuthenticationTypeHandler.AuthError
                    self.showError(authErr?.localizedDescription ?? "")
                case is JadeError:
                    switch err {
                    case JadeError.Abort(let txt):
                        self.showError(txt)
                    case JadeError.URLError(let txt):
                        self.showError(txt)
                    default:
                        self.showError(err.localizedDescription)
                    }
                default:
                    self.showError(err.localizedDescription)
                }
            })
    }

    func connect(peripheral: Peripheral) {
        startAnimating()
        let appDelegate = UIApplication.shared.delegate as? AppDelegate
        let session = getGAService().getSession()
        HWResolver.shared.hw = Ledger.shared

        enstablishDispose = peripheral.establishConnection()
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { Ledger.shared.open($0) }
            .observeOn(SerialDispatchQueueScheduler(qos: .background))
            .flatMap { _ in Ledger.shared.application() }
            .compactMap { res in
                let name = res["name"] as? String ?? ""
                let versionString = res["version"] as? String ?? ""
                let version = versionString.split(separator: ".").map {Int($0)}
                if name.contains("OLOS") {
                    throw DeviceError.dashboard // open app from dashboard
                } else if name != self.network() {
                    throw DeviceError.wrong_app // change app
                } else if name == "Bitcoin" && (version[0]! < 1 || version[1]! < 4) {
                    throw DeviceError.outdated_app
                }
            }.observeOn(SerialDispatchQueueScheduler(qos: .background))
            .compactMap { _ in
                appDelegate?.disconnect()
                try appDelegate?.connect()
                _ = try session.registerUser(mnemonic: "", hw_device: ["device": (Ledger.shared.hwDevice as Any) ]).resolve().wait()
                _ = try session.login(mnemonic: "", hw_device: ["device": Ledger.shared.hwDevice]).resolve().wait()
            }.observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in
                self.stopAnimating()
                getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
            }, onError: { err in
                self.stopAnimating()
                switch err {
                case is BluetoothError:
                    let bleErr = err as? BluetoothError
                    self.showError(NSLocalizedString("id_communication_timed_out_make", comment: "") + ": \(bleErr?.localizedDescription ?? "")")
                case RxError.timeout:
                    self.showError(NSLocalizedString("id_communication_timed_out_make", comment: ""))
                case DeviceError.dashboard:
                    self.showError(String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.network()))
                case DeviceError.outdated_app:
                    self.showError("Outdated Ledger app: update the bitcoin app via Ledger Manager")
                case DeviceError.wrong_app:
                self.showError(String(format: NSLocalizedString("id_select_the_s_app_on_your_ledger", comment: ""), self.network()))
                case is AuthenticationTypeHandler.AuthError:
                    let authErr = err as? AuthenticationTypeHandler.AuthError
                    self.showError(authErr?.localizedDescription ?? "")
                case is Ledger.SWError:
                    self.showError(NSLocalizedString("id_invalid_status_check_that_your", comment: ""))
                default:
                    self.showError(err.localizedDescription)
                }
            })
    }

    func onConnect() {
        self.stopAnimating()
        getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
    }
}
