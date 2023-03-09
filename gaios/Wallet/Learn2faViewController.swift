import UIKit
import PromiseKit

protocol Learn2faViewControllerDelegate: AnyObject {
    func userLogout()
}

class Learn2faViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!

    @IBOutlet weak var lblResetTitle: UILabel!
    @IBOutlet weak var lblResetHint: UILabel!
    @IBOutlet weak var lblHowtoTitle: UILabel!
    @IBOutlet weak var lblHowtoHint: UILabel!
    @IBOutlet weak var btnCancelReset: UIButton!
    @IBOutlet weak var lblPermanentTitle: UILabel!
    @IBOutlet weak var lblPermanentHint: UILabel!
    @IBOutlet weak var btnUndoReset: UIButton!

    var message: TwoFactorResetMessage!
    weak var delegate: Learn2faViewControllerDelegate?
    var session: SessionManager? { WalletManager.current?.sessions[message.network] }
    var isDisputeActive: Bool { self.session?.twoFactorConfig?.twofactorReset.isDisputeActive ?? false }

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()

        AnalyticsManager.shared.recordView(.twoFactorReset, sgmt: AnalyticsManager.shared.sessSgmt(AccountsRepository.shared.current))
    }

    func setContent() {
        title = ""
        if isDisputeActive {
            lblTitle.text = NSLocalizedString("id_2fa_dispute_in_progress", comment: "")
            lblResetTitle.text = NSLocalizedString("id_your_wallet_is_locked_under_2fa", comment: "")
            lblResetHint.text = NSLocalizedString("id_the_1_year_2fa_reset_process", comment: "")
            lblHowtoTitle.text = NSLocalizedString("id_how_to_stop_this_reset", comment: "")
            lblHowtoHint.text = NSLocalizedString("id_if_you_are_the_rightful_owner", comment: "")
            btnCancelReset.setTitle(NSLocalizedString("id_cancel_2fa_reset", comment: ""), for: .normal)
            lblPermanentTitle.text = NSLocalizedString("id_undo_2fa_dispute", comment: "")
            lblPermanentHint.text = NSLocalizedString("id_if_you_initiated_the_2fa_reset", comment: "")
            // when in dispute, use the button to undo a dispute
            btnUndoReset.setTitle(NSLocalizedString("id_undo_2fa_dispute", comment: ""), for: .normal)
            return
        }
        let resetDaysRemaining = session?.twoFactorConfig?.twofactorReset.daysRemaining
        lblTitle.text = NSLocalizedString("id_2fa_reset_in_progress", comment: "")
        lblResetTitle.text = String(format: NSLocalizedString("id_your_wallet_is_locked_for_a", comment: ""), resetDaysRemaining ?? 0)
        lblResetHint.text = NSLocalizedString("id_the_waiting_period_is_necessary", comment: "")
        lblHowtoTitle.text = NSLocalizedString("id_how_to_stop_this_reset", comment: "")
        lblHowtoHint.text = String(format: NSLocalizedString("id_if_you_have_access_to_a", comment: ""), resetDaysRemaining ?? 0)
        btnCancelReset.setTitle(NSLocalizedString("id_cancel_2fa_reset", comment: ""), for: .normal)
        lblPermanentTitle.text = NSLocalizedString("id_permanently_block_this_wallet", comment: "")
        lblPermanentHint.text = NSLocalizedString("id_if_you_did_not_request_the", comment: "")
        // when not in dispute, use the button to dispute
        btnUndoReset.setTitle(NSLocalizedString("id_dispute_twofactor_reset", comment: ""), for: .normal)
    }

    func canceltwoFactorReset() {
        //AnalyticsManager.shared.recordView(.walletSettings2FACancelDispute, sgmt: AnalyticsManager.shared.twoFacSgmt(AccountsRepository.shared.current, walletType: wallet?.type, twoFactorType: nil))

        let bgq = DispatchQueue.global(qos: .background)
        guard let session = session else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            session.cancelTwoFactorReset()
        }.then(on: bgq) { _ in
            session.loadTwoFactorConfig()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            DropAlert().success(message: "Reset Cancelled")
            self.dismiss(animated: true, completion: nil)
        }.catch {_ in
            self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: NSLocalizedString("id_cancel_twofactor_reset", comment: ""))
        }
    }

    func disputeReset(email: String) {
        //AnalyticsManager.shared.recordView(.walletSettings2FADispute, sgmt: AnalyticsManager.shared.twoFacSgmt(AccountsRepository.shared.current, walletType: wallet?.type, twoFactorType: nil))

        let bgq = DispatchQueue.global(qos: .background)
        guard let session = session else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            session.resetTwoFactor(email: email, isDispute: true)
        }.then(on: bgq) { _ in
            session.loadTwoFactorConfig()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            DropAlert().success(message: "Reset Disputed")
            self.dismiss(animated: true, completion: nil)
        }.catch {_ in
            self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: NSLocalizedString("id_dispute_twofactor_reset", comment: ""))
        }
    }

    func undoReset(email: String) {
        //AnalyticsManager.shared.recordView(.walletSettings2FAUndoDispute, sgmt: AnalyticsManager.shared.twoFacSgmt(AccountsRepository.shared.current, walletType: wallet?.type, twoFactorType: nil))

        let bgq = DispatchQueue.global(qos: .background)
        guard let session = session else { return }
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            session.undoTwoFactorReset(email: email)
        }.then(on: bgq) { _ in
            session.loadTwoFactorConfig()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            DropAlert().success(message: "Reset Undone")
            self.dismiss(animated: true, completion: nil)
        }.catch {_ in
            self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: NSLocalizedString("id_undo_2fa_dispute", comment: ""))
        }
    }

    @IBAction func BtnCancelReset(_ sender: Any) {
        canceltwoFactorReset()
    }

    @IBAction func BtnUndoReset(_ sender: Any) {
        let alertTitle = isDisputeActive ? NSLocalizedString("id_undo_2fa_dispute", comment: "") : NSLocalizedString("id_dispute_twofactor_reset", comment: "")
        let alertMsg = isDisputeActive ? "Provide the email you previously used to dispute" : ""
        let alert = UIAlertController(title: alertTitle, message: alertMsg, preferredStyle: .alert)
        alert.addTextField { (textField) in textField.placeholder = NSLocalizedString("id_email", comment: "") }
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
        alert.addAction(UIAlertAction(title: NSLocalizedString("id_next", comment: ""), style: .default) { _ in
            let email = alert.textFields![0].text!
            if self.isDisputeActive {
                self.undoReset(email: email)
            } else {
                self.disputeReset(email: email)
            }
        })
        self.present(alert, animated: true, completion: nil)
    }
}
