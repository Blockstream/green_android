import UIKit
import gdk
import greenaddress

class RecoveryTransactionsViewController: UIViewController {

    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnMoreInfo: UIButton!
    @IBOutlet weak var item1: UIView!
    @IBOutlet weak var item2: UIView!
    @IBOutlet weak var item3: UIView!
    @IBOutlet weak var bg1: UIView!
    @IBOutlet weak var bg2: UIView!
    @IBOutlet weak var bg3: UIView!
    @IBOutlet weak var lblTitle1: UILabel!
    @IBOutlet weak var lblTitle2: UILabel!
    @IBOutlet weak var lblTitle3: UILabel!
    @IBOutlet weak var actionSwitch: UISwitch!

    var viewModel: RecoveryTransactionsViewModel!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        update()
        item3.isHidden = true /// for future usage
    }

    func setContent() {
        title = "id_recovery_transactions".localized
        lblTitle1.text = "id_recovery_transaction_emails".localized
        lblTitle2.text = "id_request_recovery_transactions".localized
        lblTitle3.text = "id_set_an_email_for_recovery".localized
        lblHint.text = "id_if_you_have_some_coins_on_the".localized
        btnMoreInfo.setTitle("id_more_info".localized, for: .normal)
    }

    func setStyle() {
        [bg1, bg2, bg3].forEach{ $0?.cornerRadius = 5.0 }
        [lblTitle1, lblTitle2, lblTitle3].forEach{ $0?.setStyle(.titleCard)}
        lblHint.setStyle(.txtCard)
        btnMoreInfo.setStyle(.outlined)
    }

    @MainActor
    func update() {
        emailIsSet(false)
        Task {
            do {
                let twoFactorEmail = try await viewModel.getTwoFactorItemEmail()
                if let twoFactorEmail = twoFactorEmail {
                    if let maskedData = twoFactorEmail.maskedData, maskedData.count > 1 {
                        self.emailIsSet(true)
                    } else {
                        self.emailIsSet(false)
                    }
                }
            } catch { print(error) }
            if let notifications = viewModel?.session.settings?.notifications {
                actionSwitch.isOn = notifications.emailIncoming == true
            }
        }
    }

    func emailIsSet(_ flag: Bool) {
        [item1, item2].forEach{
            $0?.alpha = flag ? 1.0 : 0.5
            $0?.isUserInteractionEnabled = flag
        }
    }

    func enableRecoveryTransactions(_ enable: Bool) {
        guard let session = viewModel?.session, let settings = viewModel?.session.settings else { return }
        settings.notifications = SettingsNotifications(emailIncoming: enable,
                                                       emailOutgoing: enable)
        Task {
            do {
                _ = try await session.changeSettings(settings: settings)
                self.update()
            } catch {
                self.showError(error)
            }
        }
    }

    @IBAction func actionSwitchChange(_ sender: Any) {
        enableRecoveryTransactions(actionSwitch.isOn)
    }

    @IBAction func btnRequest(_ sender: Any) {
        guard let session = viewModel?.session else { return }
        self.startAnimating()
        Task {
            do {
                try await session.session?.sendNlocktimes()
                await MainActor.run {
                    DropAlert().success(message: "id_recovery_transaction_request".localized)
                }
            } catch {
                self.showError(error)
            }
        }
        self.stopAnimating()
    }

    @IBAction func btnSetEmail(_ sender: Any) {

    }

    @IBAction func btnMoreInfo(_ sender: Any) {
        SafeNavigationManager.shared.navigate( ExternalUrls.helpRecoveryTransactions )
    }
}
