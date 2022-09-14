import UIKit
import PromiseKit

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

        view.accessibilityIdentifier = AccessibilityIdentifiers.WalletSuccessScreen.view
        btnWallet.accessibilityIdentifier = AccessibilityIdentifiers.WalletSuccessScreen.nextBtn
    }

    func setContent() {
        title = ""
        lblHint.text = NSLocalizedString("id_success", comment: "")
        lblRecoveryDone.text = NSLocalizedString("id_lets_get_you_set_up", comment: "")

        switch LandingViewController.flowType {
        case .add:
            lblRecoveryDone.isHidden = true
            AnalyticsManager.shared.recordView(.onBoardSuccess, sgmt: AnalyticsManager.shared.onBoardSgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strCreate))
        case .restore:
            btnWallet.setTitle(NSLocalizedString("id_done", comment: ""), for: .normal)
            btnBackup.isHidden = true
            AnalyticsManager.shared.recordView(.onBoardSuccess, sgmt: AnalyticsManager.shared.onBoardSgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strRestore))
        case .watchonly:
            break
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
        guard let account = AccountDao.shared.current else {
            fatalError("account must exist")
        }
        let wm = WalletManager.getOrAdd(for: account)
        Guarantee()
            .compactMap { OnBoardManager.shared.session }
            .then { $0.getCredentials(password: "") }
            .then { wm.login($0) }
            .done {
                let appDelegate = UIApplication.shared.delegate as? AppDelegate
                appDelegate?.instantiateViewControllerAsRoot(storyboard: "Wallet", identifier: "TabViewController")
            }.catch { _ in
                print ("login error")
            }
    }

    @IBAction func btnBackup(_ sender: Any) {
        navigationController?.popToRootViewController(animated: true)
    }

}
