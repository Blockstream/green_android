import UIKit
import RxBluetoothKit

class HWWScanViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var arrow: UIImageView!
    @IBOutlet weak var card: UIView!
    @IBOutlet weak var lblHead: UILabel!
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var deviceImage: UIImageView!
    @IBOutlet weak var deviceImageAlign: NSLayoutConstraint!

    var account: Account!
    private var peripherals = [ScannedPeripheral]()

    override func viewDidLoad() {
        super.viewDidLoad()
        BLEManager.shared.dispose()
        BLEManager.shared.scanDelegate = self
        setContent()
        setStyle()
    }

    func setContent() {
        lblTitle.text = account.name
        lblHint.text = NSLocalizedString("id_please_follow_the_instructions", comment: "")
        lblHead.text = NSLocalizedString("id_devices", comment: "")
        tableView.dataSource = self
        tableView.delegate = self
        tableView.tableFooterView = UIView()
    }

    func setStyle() {
        card.layer.cornerRadius = 5.0
        arrow.image = UIImage(named: "ic_hww_arrow")?.maskWithColor(color: UIColor.customMatrixGreen())
        deviceImage.image = account.deviceImage()
        deviceImageAlign.constant = account.alignConstraint()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        BLEManager.shared.start()
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        BLEManager.shared.disposeScan()
    }
}
extension HWWScanViewController: UITableViewDelegate, UITableViewDataSource {

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return peripherals.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let peripheral = peripherals[indexPath.row]
        if let cell = tableView.dequeueReusableCell(withIdentifier: "HWWCell") as? HWWCell {
            let connected = peripheral.peripheral.isConnected ? "Connected" : "Not connected"
            cell.configure(peripheral.advertisementData.localName ?? "", connected.uppercased())
            cell.selectionStyle = .none
            return cell
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 48.0
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let peripheral = peripherals[indexPath.row].peripheral
        let storyboard = UIStoryboard(name: "HWW", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "HWWConnectViewController") as? HWWConnectViewController {
            vc.account = account
            vc.peripheral = peripheral
            navigationController?.pushViewController(vc, animated: true)
        }
    }
}

extension HWWScanViewController: BLEManagerScanDelegate {

    func didUpdatePeripherals(_ peripherals: [ScannedPeripheral]) {
        let filterName = account.isJade ? "Jade" : "Nano"
        self.peripherals = peripherals.filter {
            $0.peripheral.name?.contains(filterName) ?? false
        }
        DispatchQueue.main.async {
            self.tableView.reloadData()
        }
    }

    func onError(_ error: BLEManagerError) {
        switch error {
        case .powerOff(let txt):
            showError(txt)
        case .notReady(let txt):
            showError(txt)
        case .scanErr(let txt):
            showError(txt)
        case .bleErr(let txt):
            showError(txt)
        default:
            showError(error.localizedDescription)
        }
    }
}
