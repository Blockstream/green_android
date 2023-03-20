import UIKit
import RxBluetoothKit
import RxSwift

class ConnectViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!
    @IBOutlet weak var image: UIImageView!
    @IBOutlet weak var retryButton: UIButton!

    var account: Account!
    private var error = false

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

    func loadNavigationBtns() {
        // Troubleshoot
        let settingsBtn = UIButton(type: .system)
        settingsBtn.titleLabel?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        settingsBtn.tintColor = UIColor.gGreenMatrix()
        settingsBtn.setTitle("id_troubleshoot".localized, for: .normal)
        settingsBtn.addTarget(self, action: #selector(troubleshootBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
    }

    @objc func troubleshootBtnTapped() {
        print("sss")
    }

    @IBAction func retryBtnTapped(_ sender: Any) {
        BLEViewModel.shared.dispose()
        scan()
    }

    override func viewDidAppear(_ animated: Bool) {
        scan()
    }

    override func viewWillDisappear(_ animated: Bool) {
        stop()
        BLEViewModel.shared.scanDispose?.dispose()
    }

    func setContent() {
        if account.isJade {
            image.image = UIImage(named: "il_jade_unlock")
        } else {
            image.image = UIImage(named: "il_ledger")
        }
        retryButton.isHidden = true
        retryButton.setTitle("Retry".localized, for: .normal)
        retryButton.setStyle(.primary)
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblTitle.textColor = .white
        lblTitle.text = ""
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

    func next(_ peripheral: Peripheral) {
        print("account.uuid \(account.uuid!)")
        print("peripheral.identifier \(peripheral.identifier)")
        account.uuid = peripheral.identifier
        AccountsRepository.shared.upsert(account)
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        let nav = storyboard.instantiateViewController(withIdentifier: "TabViewController") as? UINavigationController
        UIApplication.shared.keyWindow?.rootViewController = nav
    }

    func error(_ err: Error) {
        stop()
        BLEViewModel.shared.dispose()
        retryButton.isHidden = false
        let bleError = BLEManager.shared.toBleError(err, network: nil)
        let txt = BLEManager.shared.toErrorString(bleError)
        lblTitle.text = txt
        image.image = UIImage(named: "il_connection_fail")
        error = true
    }

    func scan() {
        start()
        setContent()
        self.progress(account.isJade ? "Power on Jade".localized : "id_please_follow_the_instructions".localized)
        do {
            try BLEViewModel.shared.isReady()
            BLEViewModel.shared.scan(jade: account.isJade,
                                    completion: { self.connect($0) },
                                     error: self.error)
        } catch { self.error(error) }
    }

    func progress(_ txt: String) {
        DispatchQueue.main.async {
            self.lblTitle.text = txt
        }
    }
    func connect(_ peripherals: [Peripheral]) {
        let peripheral = peripherals.filter { $0.peripheral.identifier == account.uuid || $0.peripheral.name == account.name }.first
        guard let peripheral = peripheral else {
            return
        }
        self.progress("id_connecting".localized)
        BLEViewModel.shared.pairing(peripheral,
                                    completion: { _ in
            if peripheral.isJade() && (self.error || peripheral.identifier != self.account.uuid) {
                sleep(10)
            }
            self.login(peripheral)
        }, error: self.error )
    }

    func login(_ peripheral: Peripheral) {
        BLEViewModel.shared.login(account: account,
                                  peripheral: peripheral,
                                  progress: { self.progress(self.account.isJade ? $0 : "id_logging_in".localized) },
                                  completion: { self.next(peripheral) },
                                  error: self.error)
    }
}
