import UIKit
import RxBluetoothKit
import RxSwift

class LedgerWaitViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!

    private var activeToken, resignToken: NSObjectProtocol?
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
        activeToken = NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main, using: applicationDidBecomeActive)
        resignToken = NotificationCenter.default.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main, using: applicationWillResignActive)
        start()
        BLEViewModel.shared.scan(jade: false,
                                 completion: self.next,
                                 error: self.error)
    }

    override func viewWillDisappear(_ animated: Bool) {
        if let token = activeToken {
            NotificationCenter.default.removeObserver(token)
        }
        if let token = resignToken {
            NotificationCenter.default.removeObserver(token)
        }
        stop()
        BLEViewModel.shared.scanDispose?.dispose()
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

    func applicationDidBecomeActive(_ notification: Notification) {
        start()
    }

    func applicationWillResignActive(_ notification: Notification) {
        stop()
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

    func next(peripherals: [Peripheral]) {
        if peripherals.isEmpty { return  }
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "ListDevicesViewController") as? ListDevicesViewController {
            vc.isJade = false
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }
}
