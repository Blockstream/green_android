import UIKit

class WalletSuccessViewController: UIViewController {

    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var successCircle: UIView!
    @IBOutlet weak var btnWallet: UIButton!
    @IBOutlet weak var btnBackup: UIButton!
    @IBOutlet weak var lblRecoveryDone: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()
    }

    func setContent() {
        title = ""
        lblHint.text = "Success"
        lblRecoveryDone.text = "Now we’re all set up. Let’s go!"

        switch LandingViewController.flowType {
        case .add:
            lblRecoveryDone.isHidden = true
        case .restore:
            btnWallet.setTitle("Done", for: .normal)
            btnBackup.isHidden = true
        }
    }

    func setStyle() {
        btnWallet.cornerRadius = 4.0
        btnBackup.cornerRadius = 4.0
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

    @IBAction func btnWallet(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Wallet", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "TabViewController")
        self.navigationController?.pushViewController(vc, animated: true)
    }

    @IBAction func btnBackup(_ sender: Any) {
        navigationController?.popToRootViewController(animated: true)
    }

}
