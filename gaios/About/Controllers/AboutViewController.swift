import UIKit

class AboutViewController: UIViewController {

    @IBOutlet weak var lblVersion: UILabel!
    @IBOutlet weak var lblCopyright: UILabel!

    @IBOutlet weak var btnFeedback: UIButton!
    @IBOutlet weak var btnHelp: UIButton!
    @IBOutlet weak var btnTerms: UIButton!
    @IBOutlet weak var btnPrivacy: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
    }

    func setContent() {
        btnFeedback.setTitle(NSLocalizedString("id_give_us_your_feedback", comment: ""), for: .normal)
        btnHelp.setTitle(NSLocalizedString("id_visit_the_blockstream_help", comment: ""), for: .normal)
        btnTerms.setTitle(NSLocalizedString("id_terms_of_service", comment: ""), for: .normal)
        btnPrivacy.setTitle(NSLocalizedString("id_privacy_policy", comment: ""), for: .normal)
        lblVersion.text = String(format: NSLocalizedString("id_version_1s", comment: ""), "\(Bundle.main.versionNumber)")
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy"
        lblCopyright.text = "© \(formatter.string(from: Date())) Blockstream Corporation Inc."
    }

    func setStyle() {
        [btnFeedback, btnHelp, btnTerms, btnPrivacy].forEach { $0.setStyle(.outlined) }
    }

    func navigate(_ url: URL) {
        SafeNavigationManager.shared.navigate(url)
    }

    @IBAction func didTap(_ sender: UIButton) {

        switch sender.tag {
        case 1: //web
            navigate(ExternalUrls.aboutBlockstreamGreenWebSite)
        case 2: //tw
            navigate(ExternalUrls.aboutBlockstreamTwitter)
        case 3: //in
            navigate(ExternalUrls.aboutBlockstreamLinkedIn)
        case 4: //fb
            navigate(ExternalUrls.aboutBlockstreamFacebook)
        case 5: //tel
            navigate(ExternalUrls.aboutBlockstreamTelegram)
        case 6: //git
            navigate(ExternalUrls.aboutBlockstreamGitHub)
        case 7: //you
            navigate(ExternalUrls.aboutBlockstreamYouTube)
        default:
            break
        }
    }

    func openFeedback() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogSendFeedbackViewController") as? DialogSendFeedbackViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    @IBAction func btnFeedback(_ sender: Any) {
        openFeedback()
    }

    @IBAction func btnHelp(_ sender: Any) {
        navigate(ExternalUrls.aboutHelpCenter)
    }

    @IBAction func btnTerms(_ sender: Any) {
        navigate(ExternalUrls.aboutTermsOfService)
    }

    @IBAction func btnPrivacy(_ sender: Any) {
        navigate(ExternalUrls.aboutPrivacyPolicy)
    }

}

extension AboutViewController: DialogSendFeedbackViewControllerDelegate {

    func didCancel() {
        //
    }

    func didSend(rating: Int, email: String?, comment: String) {
        AnalyticsManager.shared.recordFeedback(rating: rating, email: email, comment: comment)
        DropAlert().info(message: NSLocalizedString("id_thank_you_for_your_feedback", comment: ""))
    }
}
