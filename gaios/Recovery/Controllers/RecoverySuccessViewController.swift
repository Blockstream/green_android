import UIKit

class RecoverySuccessViewController: UIViewController {

    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var successCircle: UIView!
    @IBOutlet weak var btnNext: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()

        view.accessibilityIdentifier = AccessibilityIdentifiers.RecoverySuccessScreen.view
        btnNext.accessibilityIdentifier = AccessibilityIdentifiers.RecoverySuccessScreen.nextBtn
    }

    func setContent() {
        title = ""
        lblHint.text = NSLocalizedString("id_success", comment: "")
        btnNext.setTitle(NSLocalizedString("id_continue", comment: ""), for: .normal)
    }

    func setStyle() {
        btnNext.cornerRadius = 4.0
        successCircle.borderWidth = 2.0
        successCircle.layer.cornerRadius = successCircle.frame.size.width / 2.0
        successCircle.borderColor = UIColor.customMatrixGreen()
    }

    func setActions() {

    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }

    @IBAction func btnNext(_ sender: Any) {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WalletNameViewController")
        navigationController?.pushViewController(vc, animated: true)
    }

}
