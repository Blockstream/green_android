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

    var jade = true
    private var peripherals = [Peripheral]()

    override func viewDidLoad() {
        super.viewDidLoad()
        BLEManager.shared.scanDelegate = self
        setContent()
        setStyle()

        view.accessibilityIdentifier = AccessibilityIdentifiers.HWWScanScreen.view
        lblTitle.accessibilityIdentifier = AccessibilityIdentifiers.HWWScanScreen.titleLbl

        AnalyticsManager.shared.recordView(.deviceList)
    }

    func setContent() {
        // Hardware wallets accounts are store in temporary memory
        lblTitle.text = jade ? "Blockstream Jade" : "Ledger Nano X"
        lblHint.text = NSLocalizedString("id_please_follow_the_instructions", comment: "")
        lblHead.text = NSLocalizedString("id_devices", comment: "")
        tableView.dataSource = self
        tableView.delegate = self
        tableView.tableFooterView = UIView()
    }

    func setStyle() {
        card.layer.cornerRadius = 5.0
        arrow.image = UIImage(named: "ic_hww_arrow")?.maskWithColor(color: UIColor.customMatrixGreen())
        if jade {
            deviceImage.image = UIImage(named: "ic_hww_jade")
            deviceImageAlign.constant = 0
        } else {
            deviceImage.image = UIImage(named: "ic_hww_ledger")
            deviceImageAlign.constant = UIScreen.main.bounds.width * 0.27
        }
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
            let connected = peripheral.isConnected ? "Connected" : "Not connected"
            cell.configure(peripheral.name ?? "", connected.uppercased())
            cell.selectionStyle = .none
            return cell
        }

        return UITableViewCell()
    }

    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 48.0
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let peripheral = peripherals[indexPath.row]
        let storyboard = UIStoryboard(name: "HWW", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "HWWConnectViewController") as? HWWConnectViewController {
            vc.peripheral = peripheral
            navigationController?.pushViewController(vc, animated: true)
        }
    }
}

extension HWWScanViewController: BLEManagerScanDelegate {

    func didUpdatePeripherals(_ peripherals: [Peripheral]) {
        let filterName = jade ? "Jade" : "Nano"
        self.peripherals = peripherals.filter {
            $0.name?.contains(filterName) ?? false
        }
        DispatchQueue.main.async {
            self.tableView.reloadData()
        }
    }

    func onError(_ error: BLEManagerError) {
        switch error {
        case .unauthorized(let txt):
            let alert = UIAlertController(title: NSLocalizedString("id_warning", comment: ""), message: txt, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_continue", comment: ""), style: .default) { _ in })
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_settings", comment: ""), style: .cancel) { _ in
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            })
            self.present(alert, animated: true, completion: nil)
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
