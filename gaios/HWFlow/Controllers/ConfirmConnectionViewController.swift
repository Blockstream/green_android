import UIKit

class ConfirmConnectionViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnConfirm: UIButton!
    @IBOutlet weak var lblSerial: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
    }

    func setContent() {
        lblTitle.text = "Confirm Connection".localized
        lblHint.text = "Check that your Jade has paired successfully".localized
        lblSerial.text = "Jade 1F6C60"
        btnConfirm.setTitle("Confirm Connection".localized, for: .normal)
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblTitle.textColor = .white
        [lblHint].forEach {
            $0.textColor = .white.withAlphaComponent(0.6)
            $0.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        }
        [lblSerial].forEach {
            $0.textColor = .white
            $0.font = UIFont.systemFont(ofSize: 16.0, weight: .bold)
        }
        btnConfirm.setStyle(.primary)
    }

    @IBAction func btnConfirm(_ sender: Any) {
//        let hwFlow = UIStoryboard(name: "HWFlow", bundle: nil)
//        if let vc = hwFlow.instantiateViewController(withIdentifier: "ConnectionFailViewController") as? ConnectionFailViewController {
//            self.navigationController?.pushViewController(vc, animated: true)
//        }
    }
}
