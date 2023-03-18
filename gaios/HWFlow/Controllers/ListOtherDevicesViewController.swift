import UIKit
import RxSwift
import RxBluetoothKit

class ListOtherDevicesViewController: HWFlowBaseViewController {

    @IBOutlet weak var tableView: UITableView!

    var peripherals = [Peripheral]() {
        didSet {
            tableView.reloadData()
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        ["OtherDeviceCell" ].forEach {
            tableView.register(UINib(nibName: $0, bundle: nil), forCellReuseIdentifier: $0)
        }
        setContent()
        setStyle()
    }

    deinit {
        print("Deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        do {
            try BLEViewModel.shared.isReady()
            BLEViewModel.shared.scan(jade: false,
                                     completion: { self.peripherals = $0 },
                                     error: self.error)
        } catch { self.error(error) }
    }

    override func viewWillDisappear(_ animated: Bool) {
        BLEViewModel.shared.scanDispose?.dispose()
    }

    func setContent() {
        title = "".localized
    }

    func setStyle() {
    }

    func next(_ peripheral: Peripheral) {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "PairingSuccessOtherViewController") as? PairingSuccessOtherViewController {
            vc.peripheral = peripheral
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    func error(_ err: Error) {
        self.stopLoader()
        let bleError = BLEManager.shared.toBleError(err, network: nil)
        let txt = BLEManager.shared.toErrorString(bleError)
        showAlert(title: "id_error".localized, message: txt)
    }
}

extension ListOtherDevicesViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return peripherals.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let peripheral = peripherals[indexPath.row]
        if let cell = tableView.dequeueReusableCell(withIdentifier: OtherDeviceCell.identifier, for: indexPath) as? OtherDeviceCell {
            cell.configure(name: peripheral.name ?? "Nano X", type: "Ledger Nano X")
            cell.selectionStyle = .none
            return cell
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
        let peripheral = peripherals[indexPath.row]
        BLEViewModel.shared.pairing(peripheral,
                                    completion: self.next,
                                    error: self.error)
    }
}
