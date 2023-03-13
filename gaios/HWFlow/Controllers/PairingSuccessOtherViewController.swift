import UIKit

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

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
    }

    func setContent() {
        lblSerial.text = "Serial F2910-1100-5120"
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
        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
        if let vc = hwFlow.instantiateViewController(withIdentifier: "ConnectionFailViewController") as? ConnectionFailViewController {
            self.navigationController?.pushViewController(vc, animated: true)
        }
    }

    @IBAction func btnRemember(_ sender: Any) {
        remember.toggle()
        iconRemember.image = remember ? UIImage(named: "ic_checkbox_on") : UIImage(named: "ic_checkbox_off")
    }
}
