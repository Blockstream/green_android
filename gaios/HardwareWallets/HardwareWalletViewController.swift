import UIKit
import PromiseKit
import RxSwift
import RxBluetoothKit

class HardwareWalletViewController: UIViewController {

    var disposeScanning: Disposable?
    var manager = CentralManager(queue: .main)
    let timeout = RxTimeInterval.seconds(10)

    enum DeviceError: Error {
        case dashboard
        case wrong_app
    }

    var network: String = { getGdkNetwork(getNetwork()).network.lowercased() == "testnet" ? "Bitcoin Test" : "Bitcoin" }()

    @IBOutlet weak var stateLabel: UILabel!
    @IBOutlet weak var deviceView: UIView!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var pairButton: UIButton!
    @IBOutlet weak var descriptionLabel: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()
        deviceView.isHidden = true
    }

    override func viewDidLayoutSubviews() {
        pairButton.insets(for: UIEdgeInsets(top: 5, left: 5, bottom: 5, right: 5), image: 14)
    }

    func reload(_ state: BluetoothState) {
        switch state {
        case .poweredOff: self.stateLabel.text = "Bluetooth Off"
        case .poweredOn: self.stateLabel.text = "Bluetooth On"
        case .unknown: self.stateLabel.text = "Bluetooth Error"
        case .resetting: self.stateLabel.text = "Bluetooth Resetting"
        case .unsupported: self.stateLabel.text = "Bluetooth Not Supported"
        case .unauthorized: self.stateLabel.text = "Bluetooth Unauthorized"
        }
    }

    @IBAction func pairButtonTapped(_ sender: Any) {
        performSegue(withIdentifier: "scan", sender: nil)
    }
}
