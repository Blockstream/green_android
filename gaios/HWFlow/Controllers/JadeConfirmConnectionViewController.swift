import UIKit
import RxBluetoothKit
import RxSwift

class JadeConfirmConnectionViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnConfirm: UIButton!
    @IBOutlet weak var lblSerial: UILabel!
    var peripheral: Peripheral!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
    }

    func setContent() {
        lblTitle.text = "Confirm Connection".localized
        lblHint.text = "Check that your Jade has paired successfully".localized
        lblSerial.text = peripheral.name ?? "Jade"
        btnConfirm.setTitle("Confirm Connection".localized, for: .normal)
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblTitle.textColor = .white
        [lblHint].forEach {
            $0?.textColor = .white.withAlphaComponent(0.6)
            $0?.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        }
        [lblSerial].forEach {
            $0?.textColor = .white
            $0?.font = UIFont.systemFont(ofSize: 16.0, weight: .bold)
        }
        btnConfirm.setStyle(.primary)
    }

    @IBAction func btnConfirm(_ sender: Any) {
        connect()
    }

    func connect() {
        startLoader()
        BLEViewModel.shared.connecting(peripheral,
                                       completion: self.next,
                                       error: self.error)
    }

    func next(jadeHasPin: Bool) {
        let testnetAvailable = UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) == true
        if !jadeHasPin {
            if testnetAvailable {
                self.selectNetwork()
                return
            }
            self.nextPin(testnet: false)
        } else {
            self.nextLogin()
        }
    }

    func nextPin(testnet: Bool) {
        self.stopLoader()
        BLEViewModel.shared.dispose()
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "PinCreateViewController") as? PinCreateViewController {
            vc.testnet = testnet
            vc.peripheral = peripheral
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    func nextLogin() {
        _ = BLEManager.shared.account(self.peripheral)
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { account in
                self.stopLoader()
                BLEViewModel.shared.dispose()
                let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
                if let vc = hwFlow.instantiateViewController(withIdentifier: "ConnectViewController") as? ConnectViewController {
                    vc.account = account
                    self.navigationController?.pushViewController(vc, animated: true)
                }
            }, onError: { self.error($0) })
    }

    func error(_ err: Error) {
        self.stopLoader()
        let bleError = BLEManager.shared.toBleError(err, network: nil)
        let txt = BLEManager.shared.toErrorString(bleError)
        showAlert(title: "id_error".localized, message: txt)
    }
}

extension JadeConfirmConnectionViewController: DialogListViewControllerDelegate {

    func selectNetwork() {
        self.stopLoader()
        let storyboard = UIStoryboard(name: "Dialogs", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogListViewController") as? DialogListViewController {
            vc.delegate = self
            vc.viewModel = DialogListViewModel(title: "Select Network", type: .networkPrefs, items: NetworkPrefs.getItems())
            vc.modalPresentationStyle = .overFullScreen
            present(vc, animated: false, completion: nil)
        }
    }

    func didSelectIndex(_ index: Int, with type: DialogType) {
        switch NetworkPrefs(rawValue: index) {
        case .mainnet:
            nextPin(testnet: false)
        case .testnet:
            nextPin(testnet: true)
        case .none:
            break
        }
    }
}
