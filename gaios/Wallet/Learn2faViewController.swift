import UIKit

class Learn2faViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!

    @IBOutlet weak var lblResetTitle: UILabel!
    @IBOutlet weak var lblResetHint: UILabel!
    @IBOutlet weak var lblHowtoTitle: UILabel!
    @IBOutlet weak var lblHowtoHint: UILabel!
    @IBOutlet weak var btnCancelReset: UIButton!
    @IBOutlet weak var lblPermanentTitle: UILabel!
    @IBOutlet weak var lblPermanentHint: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
    }

    func setContent() {
        title = ""
        lblTitle.text = "2FA Reset in Progress"

        lblResetTitle.text = "Your wallet is locked for a Two-Factor Authentication reset. The reset will be completed in %d days."
        lblResetHint.text = "The waiting period is necessary to ensure the security of your wallet, to prevent thieves from getting around your Two-Factor protection."
        lblHowtoTitle.text = "How to Stop This Reset"
        lblHowtoHint.text = "If you have access to a Two-Factor method for this wallet, cancel the reset to be able to spend and receive immediately, or wait %d days."
        btnCancelReset.setTitle("Cancel 2FA Reset", for: .normal)
        lblPermanentTitle.text = "Permanently Block This Wallet"
        lblPermanentHint.text = "If you did not request the reset, but you cannot cancel the reset process because you can't access any existing Two-Factor Authorization methods, please contact our support."
    }

    @IBAction func BtnCancelReset(_ sender: Any) {
        print("on btn cancel reset")
    }
}
