import UIKit
import RxBluetoothKit
import RxSwift

class JadeWaitViewController: HWFlowBaseViewController {

    @IBOutlet weak var imgDevice: UIImageView!
    @IBOutlet weak var lblStepNumber: UILabel!
    @IBOutlet weak var lblStepTitle: UILabel!
    @IBOutlet weak var lblStepHint: UILabel!
    @IBOutlet weak var lblLoading: UILabel!
    @IBOutlet weak var btnTrouble: UIButton!
    @IBOutlet weak var infoBox: UIView!
    @IBOutlet weak var loaderPlaceholder: UIView!

    let viewModel = JadeWaitViewModel()
    var timer: Timer?
    var idx = 0

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
        update()
    }

    deinit {
        print("Deinit")
    }

    override func viewDidAppear(_ animated: Bool) {
        timer = Timer.scheduledTimer(timeInterval: 3.0, target: self, selector: #selector(fireTimer), userInfo: nil, repeats: true)
        do {
            try BLEViewModel.shared.isReady()
            BLEViewModel.shared.scan(jade: true,
                                     completion: self.next,
                                     error: self.error)
        } catch { showError(error) }
    }

    override func viewWillDisappear(_ animated: Bool) {
        stop()
        timer?.invalidate()
        BLEViewModel.shared.scanDispose?.dispose()
    }

    @objc func fireTimer() {
        refresh()
    }

    func setContent() {
        lblLoading.text = "id_looking_for_device".localized
        btnTrouble.setTitle("id_troubleshoot".localized, for: .normal)
    }

    func refresh() {

        UIView.animate(withDuration: 0.25, animations: {
            [self.lblStepNumber, self.lblStepTitle, self.lblStepHint, self.imgDevice].forEach {
                $0?.alpha = 0.0
            }}, completion: { _ in
                if self.idx < 2 { self.idx += 1 } else { self.idx = 0}
                self.update()
                UIView.animate(withDuration: 0.4, animations: {
                    [self.lblStepNumber, self.lblStepTitle, self.lblStepHint, self.imgDevice].forEach {
                        $0?.alpha = 1.0
                    }
                })
            })
    }

    func update() {
        self.imgDevice.image = self.viewModel.steps[idx].img
        self.lblStepNumber.text = self.viewModel.steps[idx].titleStep
        self.lblStepTitle.text = self.viewModel.steps[idx].title
        self.lblStepHint.text = self.viewModel.steps[idx].hint
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
        infoBox.cornerRadius = 5.0
        lblStepNumber.font = UIFont.systemFont(ofSize: 12.0, weight: .black)
        lblStepTitle.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        lblStepHint.font = UIFont.systemFont(ofSize: 12.0, weight: .regular)
        lblLoading.font = UIFont.systemFont(ofSize: 12.0, weight: .bold)
        lblStepNumber.textColor = UIColor.gGreenMatrix()
        lblStepTitle.textColor = .white
        lblStepHint.textColor = .white.withAlphaComponent(0.6)
        lblLoading.textColor = .white
        btnTrouble.setStyle(.outlinedWhite)
    }

    @objc func setupBtnTapped() {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "SetupJadeViewController") as? SetupJadeViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
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

    @IBAction func btnTrouble(_ sender: Any) {
        //next()
    }

    func next(peripheral: [Peripheral]) {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "ListDevicesViewController") as? ListDevicesViewController {
            vc.isJade = true
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    func error(_ err: Error) {
        let bleError = BLEManager.shared.toBleError(err, network: nil)
        let txt = BLEManager.shared.toErrorString(bleError)
        showAlert(title: "id_error".localized, message: txt)
    }
}
