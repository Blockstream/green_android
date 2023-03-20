import UIKit
import RxBluetoothKit
import RxSwift

class LedgerWaitViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!

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
        do {
            try BLEViewModel.shared.isReady()
            BLEViewModel.shared.scan(jade: false,
                                     completion: self.next,
                                     error: self.error)
        } catch { self.error(error) }
    }

    override func viewWillDisappear(_ animated: Bool) {
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

    func next(peripheral: [Peripheral]) {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "ListDevicesViewController") as? ListDevicesViewController {
            vc.isJade = false
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    func error(_ err: Error) {
        let bleError = BLEManager.shared.toBleError(err, network: nil)
        let txt = BLEManager.shared.toErrorString(bleError)
        showAlert(title: "id_error".localized, message: txt)
    }
}
