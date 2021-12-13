import UIKit

class ChooseSecurityViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var cardSimple: UIView!
    @IBOutlet weak var lblSimpleTitle: UILabel!
    @IBOutlet weak var lblSimpleHint: UILabel!

    @IBOutlet weak var cardAdvanced: UIView!
    @IBOutlet weak var lblAdvancedTitle: UILabel!
    @IBOutlet weak var lblAdvancedHint: UILabel!
    @IBOutlet weak var viewMnemonicSize: UIView!
    @IBOutlet weak var lblMnemonicSize: UILabel!
    @IBOutlet weak var segmentMnemonicSize: UISegmentedControl!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()

        view.accessibilityIdentifier = AccessibilityIdentifiers.ChooseSecurityScreen.view
        cardAdvanced.accessibilityIdentifier = AccessibilityIdentifiers.ChooseSecurityScreen.multiSigCard
        cardSimple.accessibilityIdentifier = AccessibilityIdentifiers.ChooseSecurityScreen.singleSigCard
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_choose_security_policy", comment: "")
        lblHint.text = NSLocalizedString("id_once_selected_this_spending", comment: "")
        lblSimpleTitle.text = NSLocalizedString("id_singlesig", comment: "")
        lblSimpleHint.text = NSLocalizedString("id_your_funds_are_secured_by_a", comment: "")
        lblAdvancedTitle.text = NSLocalizedString("id_multisig_shield", comment: "")
        lblAdvancedHint.text = NSLocalizedString("id_your_funds_are_secured_by", comment: "")
        viewMnemonicSize.isHidden = LandingViewController.flowType != .add
    }

    func setStyle() {
        cardSimple.layer.cornerRadius = 5.0
        cardAdvanced.layer.cornerRadius = 5.0
        viewMnemonicSize.layer.cornerRadius = 5.0
        viewMnemonicSize.borderWidth = 1.0
        viewMnemonicSize.borderColor = UIColor.customGrayLight()
        if #available(iOS 13.0, *) {
            segmentMnemonicSize.backgroundColor = UIColor.clear
            segmentMnemonicSize.layer.borderColor = UIColor.customMatrixGreen().cgColor
            segmentMnemonicSize.selectedSegmentTintColor = UIColor.customMatrixGreen()
             let titleTextAttributes = [NSAttributedString.Key.foregroundColor: UIColor.customMatrixGreen()]
            segmentMnemonicSize.setTitleTextAttributes(titleTextAttributes, for: .normal)
             let titleTextAttributes1 = [NSAttributedString.Key.foregroundColor: UIColor.white]
            segmentMnemonicSize.setTitleTextAttributes(titleTextAttributes1, for: .selected)
         } else {
             segmentMnemonicSize.tintColor = UIColor.customMatrixGreen()
             segmentMnemonicSize.layer.borderWidth = 1
             segmentMnemonicSize.layer.borderColor = UIColor.customMatrixGreen().cgColor
             let titleTextAttributes = [NSAttributedString.Key.foregroundColor: UIColor.customMatrixGreen()]
            segmentMnemonicSize.setTitleTextAttributes(titleTextAttributes, for: .normal)
             let titleTextAttributes1 = [NSAttributedString.Key.foregroundColor: UIColor.white]
            segmentMnemonicSize.setTitleTextAttributes(titleTextAttributes1, for: .selected)
       }
    }

    func setActions() {
        let tapGesture1 = UITapGestureRecognizer(target: self, action: #selector(didPressCardSimple))
        cardSimple.addGestureRecognizer(tapGesture1)
        let tapGesture2 = UITapGestureRecognizer(target: self, action: #selector(didPressCardAdvanced))
        cardAdvanced.addGestureRecognizer(tapGesture2)
    }

    @objc func didPressCardSimple() {
        OnBoardManager.shared.params?.singleSig = true
        next()
    }

    @objc func didPressCardAdvanced() {
        OnBoardManager.shared.params?.singleSig = false
        next()
    }

    func next() {
        if segmentMnemonicSize.selectedSegmentIndex == 1 {
            OnBoardManager.shared.params?.mnemonicSize = MnemonicSize._24.rawValue
        } else {
            OnBoardManager.shared.params?.mnemonicSize = MnemonicSize._12.rawValue
        }
        let storyboard = UIStoryboard(name: "Recovery", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryInstructionViewController")
        navigationController?.pushViewController(vc, animated: true)
    }
}
