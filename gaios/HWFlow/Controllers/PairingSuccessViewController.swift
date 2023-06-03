import Foundation
import UIKit
import PromiseKit
import RxBluetoothKit
import RxSwift

class PairingSuccessViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnContinue: UIButton!
    @IBOutlet weak var rememberView: UIView!
    @IBOutlet weak var lblRemember: UILabel!
    @IBOutlet weak var imgDevice: UIImageView!
    @IBOutlet weak var rememberSwitch: UISwitch!
    @IBOutlet weak var btnAppSettings: UIButton!

    var peripheral: Peripheral!

    override func viewDidLoad() {
        super.viewDidLoad()

        rememberSwitch.isOn = true
        mash.isHidden = true
        setContent()
        setStyle()
        if peripheral.isJade() {
            loadNavigationBtns()
        }
        AnalyticsManager.shared.hwwConnect(account: AccountsRepository.shared.current)
    }

    func setContent() {
        lblTitle.text = peripheral.name
        lblHint.text = "id_follow_the_instructions_on_your".localized
        btnContinue.setTitle("id_continue".localized, for: .normal)
        lblRemember.text = "id_remember_device_connection".localized
        imgDevice.image = UIImage(named: peripheral.isJade() ? "il_jade_welcome_1" : "il_ledger")
        lblHint.text = peripheral.isJade() ? "Blockstream" : ""
        btnAppSettings.setTitle(NSLocalizedString("id_app_settings", comment: ""), for: .normal)
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 24.0, weight: .bold)
        lblTitle.textColor = .white
        [lblHint, lblRemember].forEach {
            $0?.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
            $0?.textColor = .white
        }
        btnContinue.setStyle(.primary)
        btnAppSettings.setStyle(.inline)
        btnAppSettings.setTitleColor(.white.withAlphaComponent(0.6), for: .normal)
    }

    func loadNavigationBtns() {
        let settingsBtn = UIButton(type: .system)
        settingsBtn.titleLabel?.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        settingsBtn.tintColor = UIColor.gGreenMatrix()
        settingsBtn.setTitle("id_setup_guide".localized, for: .normal)
        settingsBtn.addTarget(self, action: #selector(setupBtnTapped), for: .touchUpInside)
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: settingsBtn)
    }

    @objc func setupBtnTapped() {
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "SetupJadeViewController") as? SetupJadeViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func btnContinue(_ sender: Any) {
        if peripheral.isJade() {
            self.startLoader(message: "id_connecting".localized)
            BLEViewModel.shared.connecting(peripheral,
                                           completion: self.onJadeConnected,
                                           error: self.error)
        } else {
            self.startLoader(message: "id_logging_in".localized)
            BLEViewModel.shared.initialize(peripheral: peripheral,
                                           testnet: false,
                                           progress: { _ in },
                                           completion: self.onLedgerLogin,
                                           error: self.error)
        }
    }

    @IBAction func btnAppSettings(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "WalletSettingsViewController") as? WalletSettingsViewController {
            navigationController?.pushViewController(vc, animated: true)
        }
    }

    func onJadeConnected(jadeHasPin: Bool) {
        let testnetAvailable = AppSettings.shared.testnet
        if !jadeHasPin {
            if testnetAvailable {
                self.selectNetwork()
                return
            }
            self.onJadeInitialize(testnet: false)
        } else {
            self.onJadeLogin()
        }
    }

    func onJadeLogin() {
        _ = BLEManager.shared.account(self.peripheral)
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { account in
                self.stopLoader()
                var account = account
                account.hidden = !(self.rememberSwitch.isOn)
                BLEViewModel.shared.dispose()
                let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
                if let vc = hwFlow.instantiateViewController(withIdentifier: "ConnectViewController") as? ConnectViewController {
                    vc.account = account
                    self.navigationController?.pushViewController(vc, animated: true)
                }
            }, onError: { self.error($0) })
    }

    func onJadeInitialize(testnet: Bool) {
        self.stopLoader()
        BLEViewModel.shared.dispose()
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "PinCreateViewController") as? PinCreateViewController {
            vc.testnet = testnet
            vc.peripheral = peripheral
            vc.remember = rememberSwitch.isOn
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    func onLedgerLogin(_ wm: WalletManager) {
        self.stopLoader()
        wm.account.hidden = !(rememberSwitch.isOn)
        AccountsRepository.shared.upsert(wm.account)
        AccountNavigator.goLogged(account: wm.account, nv: navigationController)

        AnalyticsManager.shared.hwwConnected(account: AccountsRepository.shared.current)
    }

    override func error(_ err: Error) {
        self.stopLoader()
        let bleError = BLEManager.shared.toBleError(err, network: nil)
        let txt = BLEManager.shared.toErrorString(bleError)
        showAlert(title: "id_error".localized, message: txt)
    }
}

extension PairingSuccessViewController: DialogListViewControllerDelegate {

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
            onJadeInitialize(testnet: false)
        case .testnet:
            onJadeInitialize(testnet: true)
        case .none:
            break
        }
    }
}
