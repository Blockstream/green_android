import UIKit

class RecoveryPhraseViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var cardQR: UIView!
    @IBOutlet weak var lblQRTitle: UILabel!
    @IBOutlet weak var lblQRHint: UILabel!

    @IBOutlet weak var cardPhrase: UIView!
    @IBOutlet weak var lblPhraseTitle: UILabel!
    @IBOutlet weak var lblPhraseHint: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()

        view.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryPhraseScreen.view
        cardPhrase.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryPhraseScreen.phraseCard

        switch LandingViewController.flowType {
        case .add:
            // not used now
            AnalyticsManager.shared.recordView(.onBoardChooseRecovery, sgmt: AnalyticsManager.shared.chooseRecoverySgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strCreate))
        case .restore:
            AnalyticsManager.shared.recordView(.onBoardChooseRecovery, sgmt: AnalyticsManager.shared.chooseRecoverySgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strRestore))
        }
    }

    func setContent() {
        title = ""
        lblTitle.text = NSLocalizedString("id_enter_your_recovery_phrase", comment: "")
        lblHint.text = NSLocalizedString("id_we_will_do_our_best_to_find_a", comment: "")
        lblQRTitle.text = NSLocalizedString("id_qr_code", comment: "")
        lblQRHint.text = NSLocalizedString("id_as_easy_as_snapping_a_photo", comment: "")
        lblPhraseTitle.text = NSLocalizedString("id_recovery_phrase", comment: "")
        lblPhraseHint.text = NSLocalizedString("id_got_it_written_down_great_you", comment: "")
    }

    func setStyle() {
        cardQR.layer.cornerRadius = 5.0
        cardPhrase.layer.cornerRadius = 5.0
    }

    func setActions() {
        let tapGesture1 = UITapGestureRecognizer(target: self, action: #selector(didPressCardQR))
        cardQR.addGestureRecognizer(tapGesture1)
        let tapGesture2 = UITapGestureRecognizer(target: self, action: #selector(didPressCardPhrase))
        cardPhrase.addGestureRecognizer(tapGesture2)
    }

    @objc func didPressCardQR() {
        next(.qr)
    }

    @objc func didPressCardPhrase() {
        next(.phrase)
    }

    func next(_ recoveryType: RecoveryType) {

        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "MnemonicViewController") as? MnemonicViewController {
            vc.recoveryType = recoveryType
            navigationController?.pushViewController(vc, animated: true)
        }
    }
}
