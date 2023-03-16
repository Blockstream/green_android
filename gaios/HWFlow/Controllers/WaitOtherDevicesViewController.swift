import UIKit
import RxBluetoothKit
import RxSwift

class WaitOtherDevicesViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!
    var scanDispose: Disposable?

    let loadingIndicator: ProgressView = {
        let progress = ProgressView(colors: [UIColor.customMatrixGreen()], lineWidth: 2)
        progress.translatesAutoresizingMaskIntoConstraints = false
        return progress
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
    }

    deinit {
        print("Deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        start()
        scan()
    }

    override func viewWillDisappear(_ animated: Bool) {
        stop()
        scanDispose?.dispose()
    }

    func setContent() {
        lblTitle.text = "id_follow_the_instructions_of_your".localized
        lblHint.text = "id_please_follow_the_instructions".localized
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblHint.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        lblTitle.textColor = .white
        lblHint.textColor = .white.withAlphaComponent(0.6)
    }

    func start() {
        loaderPlaceholder.addSubview(loadingIndicator)

        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor
                .constraint(equalTo: loaderPlaceholder.centerXAnchor),
            loadingIndicator.centerYAnchor
                .constraint(equalTo: loaderPlaceholder.centerYAnchor),
            loadingIndicator.widthAnchor
                .constraint(equalToConstant: loaderPlaceholder.frame.width),
            loadingIndicator.heightAnchor
                .constraint(equalTo: loaderPlaceholder.widthAnchor)
        ])

        loadingIndicator.isAnimating = true
    }

    func stop() {
        loadingIndicator.isAnimating = false
    }

    func scan() {
        if BLEManager.shared.manager.state == .poweredOff {
            showError("id_turn_on_bluetooth_to_connect".localized)
        } else if BLEManager.shared.manager.state == .unauthorized {
            showError("id_give_bluetooth_permissions".localized)
        }
        scanDispose = BLEManager.shared.scanning()
            .filter { $0.contains { $0.isLedger() } }
            .take(1)
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in self.next() },
                       onError: { self.showError($0.localizedDescription) })
    }

    func next() {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "ListOtherDevicesViewController") as? ListOtherDevicesViewController {
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }
}
