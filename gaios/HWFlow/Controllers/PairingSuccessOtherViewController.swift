import Foundation
import UIKit
import PromiseKit
import RxBluetoothKit
import RxSwift

class PairingSuccessOtherViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblSerial: UILabel!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var lblWarn: UILabel!
    @IBOutlet weak var btnContinue: UIButton!
    @IBOutlet weak var btnRemember: UIButton!
    @IBOutlet weak var rememberView: UIView!
    @IBOutlet weak var lblRemember: UILabel!
    @IBOutlet weak var iconRemember: UIImageView!

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
        lblHint.text = "Follow the instruction on Ledger Wallet".localized
        lblWarn.text = "* If you forget your PIN, need to restore with recovery phrase".localized
        btnContinue.setTitle("id_continue".localized, for: .normal)
        lblRemember.text = "id_remember_my_device".localized
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblTitle.textColor = .white
        [lblHint, lblWarn].forEach {
            $0.textColor = .white.withAlphaComponent(0.6)
            $0.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        }
        btnContinue.setStyle(.primary)
        rememberView.borderWidth = 2.0
        rememberView.borderColor = .white
        rememberView.cornerRadius = 4.0
    }

    @IBAction func btnContinue(_ sender: Any) {
        self.startLoader(message: "id_logging_in".localized)
        BLEManager.shared.authenticating(self.peripheral)
            .flatMap { _ in BLEManager.shared.account(self.peripheral) }
            .flatMap { BLEManager.shared.logging(self.peripheral, account: $0) }
            .observeOn(MainScheduler.instance)
            .subscribe(onNext: { self.next($0) },
                       onError: { self.error($0) })
    }

    @IBAction func btnRemember(_ sender: Any) {
        remember.toggle()
        iconRemember.image = remember ? UIImage(named: "ic_checkbox_on") : UIImage(named: "ic_checkbox_off")
    }

    func next(_ wm: WalletManager) {
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
