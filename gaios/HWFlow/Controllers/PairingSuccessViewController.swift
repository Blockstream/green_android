import Foundation
import UIKit
import PromiseKit
import RxBluetoothKit
import RxSwift

class PairingSuccessViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblSerial: UILabel!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblWarn: UILabel!
    @IBOutlet weak var btnContinue: UIButton!
    @IBOutlet weak var btnRemember: UIButton!
    @IBOutlet weak var rememberView: UIView!
    @IBOutlet weak var lblRemember: UILabel!
    @IBOutlet weak var iconRemember: UIImageView!
    @IBOutlet weak var imgDevice: UIImageView!

    var remember = false
    var peripheral: Peripheral!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
    }

    func setContent() {
        lblSerial.text = peripheral.name
        lblTitle.text = "Pairing Complete!".localized
        lblHint.text = "id_follow_the_instructions_on_your".localized
        lblWarn.text = "* If you forget your PIN, need to restore with recovery phrase".localized
        btnContinue.setTitle("id_continue".localized, for: .normal)
        lblRemember.text = "id_remember_my_device".localized
        imgDevice.image = UIImage(named: peripheral.isJade() ? "il_jade_unlock" : "il_ledger")
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblTitle.textColor = .white
        [lblHint, lblWarn].forEach {
            $0?.textColor = .white.withAlphaComponent(0.6)
            $0?.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        }
        btnContinue.setStyle(.primary)
        rememberView.borderWidth = 2.0
        rememberView.borderColor = .white
        rememberView.cornerRadius = 4.0
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

    func onJadeConnected(jadeHasPin: Bool) {
        let testnetAvailable = UserDefaults.standard.bool(forKey: AppStorage.testnetIsVisible) == true
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
                account.hidden = !self.remember
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
            vc.remember = remember
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func btnRemember(_ sender: Any) {
        remember.toggle()
        iconRemember.image = remember ? UIImage(named: "ic_checkbox_on") : UIImage(named: "ic_checkbox_off")
    }

    func onLedgerLogin(_ wm: WalletManager) {
        self.stopLoader()
        wm.account.hidden = !remember
        AccountsRepository.shared.upsert(wm.account)
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        let nav = storyboard.instantiateViewController(withIdentifier: "TabViewController") as? UINavigationController
        UIApplication.shared.keyWindow?.rootViewController = nav
    }

    func error(_ err: Error) {
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

