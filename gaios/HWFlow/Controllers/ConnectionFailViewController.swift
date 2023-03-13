import UIKit

class ConnectionFailViewController: HWFlowBaseViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnRetry: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
    }

    func setContent() {

        lblTitle.text = "id_connection_failed".localized
        lblHint.text = "Try reconnecting your device and trying again".localized
        btnRetry.setTitle("Retry".localized, for: .normal)
    }

    func setStyle() {
        lblTitle.font = UIFont.systemFont(ofSize: 26.0, weight: .bold)
        lblTitle.textColor = .white
        [lblHint].forEach {
            $0.textColor = .white.withAlphaComponent(0.6)
            $0.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        }
        btnRetry.setStyle(.primary)
    }

    @IBAction func btnRetry(_ sender: Any) {
    }
}
