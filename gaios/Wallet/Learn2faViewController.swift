import UIKit
import PromiseKit

class Learn2faViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!

    @IBOutlet weak var lblResetTitle: UILabel!
    @IBOutlet weak var lblResetHint: UILabel!
    @IBOutlet weak var lblHowtoTitle: UILabel!
    @IBOutlet weak var lblHowtoHint: UILabel!
    @IBOutlet weak var btnCancelReset: UIButton!
    @IBOutlet weak var lblPermanentTitle: UILabel!
    @IBOutlet weak var lblPermanentHint: UILabel!

    var resetDaysRemaining: Int? {
        get {
            guard let twoFactorConfig = getGAService().getTwoFactorReset() else { return nil }
            return twoFactorConfig.daysRemaining
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
    }

    func setContent() {
        title = ""
        lblTitle.text = NSLocalizedString("id_2fa_reset_in_progress", comment: "")

        lblResetTitle.text = String(format: NSLocalizedString("id_your_wallet_is_locked_for_a", comment: ""), resetDaysRemaining ?? 0)
        lblResetHint.text = NSLocalizedString("id_the_waiting_period_is_necessary", comment: "")
        lblHowtoTitle.text = NSLocalizedString("id_how_to_stop_this_reset", comment: "")
        lblHowtoHint.text = String(format: NSLocalizedString("id_if_you_have_access_to_a", comment: ""), resetDaysRemaining ?? 0)
        btnCancelReset.setTitle(NSLocalizedString("id_cancel_2fa_reset", comment: ""), for: .normal)
        lblPermanentTitle.text = NSLocalizedString("id_permanently_block_this_wallet", comment: "")
        lblPermanentHint.text = NSLocalizedString("id_if_you_did_not_request_the", comment: "")
    }

    func cancelTwoFactorReset() {
        let bgq = DispatchQueue.global(qos: .background)
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            try getGAService().getSession().cancelTwoFactorReset().resolve()
        }.ensure {
            self.stopAnimating()
        }.done { _ in
            self.logout()
        }.catch {_ in
            self.showAlert(title: NSLocalizedString("id_error", comment: ""), message: NSLocalizedString("id_cancel_twofactor_reset", comment: ""))
        }
    }

    @IBAction func BtnCancelReset(_ sender: Any) {
        cancelTwoFactorReset()
    }
}

extension Learn2faViewController {

    func logout() {
        DispatchQueue.main.async {
            let appDelegate = UIApplication.shared.delegate as? AppDelegate
            appDelegate?.logout(with: false)
        }
    }
}
