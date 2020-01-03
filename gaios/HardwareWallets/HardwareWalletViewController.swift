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

    var pairedDeviceName: String? {
        get { return UserDefaults.standard.string(forKey: "paired_device_name") }
    }
    var pairedDeviceUUID: UUID? {
        get {
            guard let uuid = UserDefaults.standard.string(forKey: "paired_device_uuid") else { return nil }
            return UUID(uuidString: uuid)
        }
    }
    var network: String = { getGdkNetwork(getNetwork()).network.lowercased() == "testnet" ? "Bitcoin Test" : "Bitcoin" }()

    @IBOutlet weak var stateLabel: UILabel!
    @IBOutlet weak var deviceView: UIView!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var pairButton: UIButton!
    @IBOutlet weak var descriptionLabel: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(connect))
        deviceView.addGestureRecognizer(tapGesture)
        deviceView.isUserInteractionEnabled = true

        _ = self.manager.observeState()
            .startWith(manager.state)
            .subscribe { self.reload($0.element!)}
    }

    override func viewWillAppear(_ animated: Bool) {
        deviceView.isHidden = true
        if let device = pairedDeviceName, let uuid = pairedDeviceUUID {
            deviceView.isHidden = false
            nameLabel.text = device
            descriptionLabel.text = uuid.uuidString
        }
    }

    override func viewDidLayoutSubviews() {
        pairButton.insets(for: UIEdgeInsets(top: 5, left: 5, bottom: 5, right: 5), image: 14)
    }

    @objc func connect() {
        guard let peripheral = self.manager.retrievePeripherals(withIdentifiers: [pairedDeviceUUID!]).first else {
            stateLabel.text = "Not found \(pairedDeviceUUID!)"
            return
        }
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
                } else if name! != self.network {
                    // change app
                    return Observable<Bool>.error(DeviceError.wrong_app)
                }
                // correct open app
                return Observable.just(true)
            }
            .subscribe(onNext: { _ in
                self.stateLabel.text = "Login on progress"
                self.login()
            }, onError: { err in
                if err as? RxError != nil {
                    self.stateLabel.text = "Turn on your Ledger and try again"
                } else if let derr = err as? DeviceError {
                    if derr == .dashboard {
                        self.stateLabel.text = "Open \(self.network) app on your Ledger"
                    } else if derr == .wrong_app {
                        self.stateLabel.text = "Quit current app and open \(self.network) app on your Ledger"
                    }
                }
            }, onCompleted: {}, onDisposed: {})
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

    @IBAction func pairButtonTapped(_ sender: Any) {
        performSegue(withIdentifier: "scan", sender: nil)
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
