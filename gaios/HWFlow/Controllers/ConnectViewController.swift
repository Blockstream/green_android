import UIKit
import RxBluetoothKit
import RxSwift

class ConnectViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!
    @IBOutlet weak var image: UIImageView!
    @IBOutlet weak var retryButton: UIButton!

    var account: Account!
    var scanDispose: Disposable?
    var connectDispose: Disposable?

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
        scanDispose?.dispose()
        connectDispose?.dispose()
        scan()
    }

    override func viewDidAppear(_ animated: Bool) {
        scan()
    }

    override func viewWillDisappear(_ animated: Bool) {
        stop()
        scanDispose?.dispose()
    }

    func setContent() {
        if account.isJade {
            lblTitle.text = "Unlock your Jade to continue".localized
            image.image = UIImage(named: "il_jade_unlock")
        } else {
            lblTitle.text =  "id_please_follow_the_instructions".localized
            image.image = UIImage(named: "il_ledger")
        }
        retryButton.isHidden = true
        retryButton.setTitle("Retry".localized, for: .normal)
        retryButton.setStyle(.primary)
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblTitle.textColor = .white
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

    func next() {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        let nav = storyboard.instantiateViewController(withIdentifier: "TabViewController") as? UINavigationController
        UIApplication.shared.keyWindow?.rootViewController = nav
    }

    func error(_ err: Error) {
        stop()
        scanDispose?.dispose()
        connectDispose?.dispose()
        retryButton.isHidden = false
        let bleError = BLEManager.shared.toBleError(err, network: nil)
        let txt = BLEManager.shared.toErrorString(bleError)
        lblTitle.text = txt
        image.image = UIImage(named: "il_connection_fail")
    }

    func scan() {
        start()
        setContent()
        if BLEManager.shared.manager.state == .poweredOff {
            showError("id_turn_on_bluetooth_to_connect".localized)
        } else if BLEManager.shared.manager.state == .unauthorized {
            showError("id_give_bluetooth_permissions".localized)
        }
        scanDispose = BLEManager.shared.scanning()
            .filter { $0.contains { $0.identifier == self.account.uuid } }
            .take(1)
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { self.connect($0.first { $0.identifier == self.account.uuid }) },
                       onError: { self.error($0) })
    }

    func connect(_ peripheral: Peripheral?) {
        guard let peripheral = peripheral else {
            self.error(BLEManagerError.genericErr(txt: "No device found"))
            return
        }
        scanDispose?.dispose()
        connectDispose = BLEManager.shared.preparing(peripheral)
            .flatMap { _ in BLEManager.shared.connecting(peripheral) }
            .flatMap { _ in BLEManager.shared.authenticating(peripheral) }
            .flatMap { _ in BLEManager.shared.logging(peripheral, account: self.account) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { _ in self.next() },
                       onError: { self.error($0) })
    }
}
