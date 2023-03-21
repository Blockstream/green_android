import UIKit
import RxBluetoothKit
import RxSwift

class PinCreateViewController: HWFlowBaseViewController {

    @IBOutlet weak var imgDevice: UIImageView!

    @IBOutlet weak var lblStepNumber: UILabel!
    @IBOutlet weak var lblStepTitle: UILabel!
    @IBOutlet weak var lblStepHint: UILabel!
    @IBOutlet weak var lblWarn: UILabel!
    @IBOutlet weak var loaderPlaceholder: UIView!
    @IBOutlet weak var btnContinue: UIButton!

    var remember = false
    var testnet = false
    var peripheral: Peripheral!

    let loadingIndicator: ProgressView = {
        let progress = ProgressView(colors: [UIColor.customMatrixGreen()], lineWidth: 2)
        progress.translatesAutoresizingMaskIntoConstraints = false
        return progress
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        loadNavigationBtns()
    }

    deinit {
        print("Deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        loadingIndicator.isAnimating = true
    }

    override func viewWillDisappear(_ animated: Bool) {
        stop()
    }

    func setContent() {
        lblStepNumber.text = "SETUP YOUR JADE"
        lblStepTitle.text = "Create a PIN"
        lblStepHint.text = "Enter and confirm a unique PIN that will be entered to unlock Jade."
        lblWarn.text = "If you forget your PIN, you will need to restore with your recovery phrase"
        btnContinue.setTitle("id_continue".localized, for: .normal)
    }

    func loadNavigationBtns() {
        let settingsBtn = UIButton(type: .system)
        settingsBtn.titleLabel?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        settingsBtn.tintColor = UIColor.gGreenMatrix()
        settingsBtn.setTitle("id_setup_guide".localized, for: .normal)
        settingsBtn.addTarget(self, action: #selector(setupBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
    }

    func setStyle() {
        [lblStepNumber].forEach {
            $0?.font = UIFont.systemFont(ofSize: 12.0, weight: .black)
            $0?.textColor = UIColor.gGreenMatrix()
        }
        [lblStepTitle].forEach {
            $0?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
            $0?.textColor = .white
        }
        [lblStepHint, lblWarn].forEach {
            $0?.font = UIFont.systemFont(ofSize: 12.0, weight: .regular)
            $0?.textColor = .white.withAlphaComponent(0.6)
        }
        btnContinue.setStyle(.primary)
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
        loadingIndicator.isHidden = false
    }

    func stop() {
        loadingIndicator.isAnimating = false
        loadingIndicator.isHidden = true
    }

    @IBAction func continueBtnTapped(_ sender: Any) {
        btnContinue.isHidden = true
        start()
        BLEViewModel.shared.initialize(peripheral: peripheral,
                                       testnet: testnet,
                                       progress: { _ in },
                                       completion: self.next,
                                       error: self.error)
    }

    @objc func setupBtnTapped() {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "SetupJadeViewController") as? SetupJadeViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func next(_ wm: WalletManager) {
        wm.account.hidden = !remember
        AccountsRepository.shared.upsert(wm.account)
        AnalyticsManager.shared.loginWallet(loginType: .hardware, ephemeralBip39: false, account: wm.account)
        getAppDelegate()!.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
    }

    override func error(_ err: Error) {
        btnContinue.isHidden = false
        self.stop()
        let bleError = BLEManager.shared.toBleError(err, network: nil)
        let txt = BLEManager.shared.toErrorString(bleError)
        showAlert(title: "id_error".localized, message: txt)
    }
}
