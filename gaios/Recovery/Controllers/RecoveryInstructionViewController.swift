import Foundation
import UIKit

class RecoveryInstructionViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var lblCard1Title: UILabel!
    @IBOutlet weak var lblCard2Title: UILabel!
    @IBOutlet weak var lblCard3Title: UILabel!
    @IBOutlet weak var lblTos: UILabel!
    @IBOutlet weak var btnNext: UIButton!

    var subAccountCreateMnemonicLength: MnemonicLengthOption?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()

        configureTosLabel()

        view.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryInstructionsScreen.view
        btnNext.accessibilityIdentifier = AccessibilityIdentifiers.RecoveryInstructionsScreen.continueBtn

        AMan.S.recordView(.recoveryIntro, sgmt: AMan.S.onBoardSgmt(onBoardParams: OnBoardManager.shared.params, flow: AMan.OnBoardFlow.strCreate))
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_back_up_recovery_phrase", comment: "")
        lblHint.text = NSLocalizedString("id_the_recovery_phrase_can_be_used", comment: "")
        lblCard1Title.text = NSLocalizedString("id_write_down_your_recovery_phrase", comment: "")
        lblCard2Title.text = NSLocalizedString("id_dont_store_your_recovery_phrase", comment: "")
        lblCard3Title.text = NSLocalizedString("id_dont_take_screenshots_of_your", comment: "")
        btnNext.setTitle(NSLocalizedString("id_continue", comment: ""), for: .normal)
    }

    func setStyle() {
        btnNext.setStyle(.primary)
    }

    func configureTosLabel() {
        let labelString = String(format: NSLocalizedString("id_by_proceeding_to_the_next_steps", comment: ""), NSLocalizedString("id_terms_of_service", comment: ""))

        guard let tosRange = labelString.range(of: NSLocalizedString("id_terms_of_service", comment: "")) else { return }
        let linkAttributes: [NSAttributedString.Key: Any] = [
            .foregroundColor: UIColor.customMatrixGreen(),
            .underlineColor: UIColor.customMatrixGreen(),
            .underlineStyle: NSUnderlineStyle.single.rawValue,
            .font: UIFont.systemFont(ofSize: 16)
        ]
        let attributedLabelString = NSMutableAttributedString(string: labelString)
        attributedLabelString.setAttributes(linkAttributes, range: NSRange(tosRange, in: labelString))
        lblTos.attributedText = attributedLabelString
        lblTos.isUserInteractionEnabled = true
        lblTos.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(labelTapped)))
    }

    @objc func labelTapped(_ recognizer: UITapGestureRecognizer) {
        if let url = URL(string: "https://blockstream.com/green/terms/") {
            UIApplication.shared.open(url, options: [:], completionHandler: nil)
        }
    }

    @IBAction func btnNext(_ sender: Any) {
        let storyboard = UIStoryboard(name: "Recovery", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryCreateViewController") as? RecoveryCreateViewController {
            vc.subAccountCreateMnemonicLength = subAccountCreateMnemonicLength
            navigationController?.pushViewController(vc, animated: true)
        }
    }

}
