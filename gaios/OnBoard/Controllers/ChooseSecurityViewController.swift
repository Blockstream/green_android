import UIKit

enum SecurityOption: String {
    case single = "SingleSig"
    case multi = "MultiSig"
}

class ChooseSecurityViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var cardSimple: UIView!
    @IBOutlet weak var lblSimpleTitle: UILabel!
    @IBOutlet weak var lblSimpleHint: UILabel!

    @IBOutlet weak var cardAdvanced: UIView!
    @IBOutlet weak var lblAdvancedTitle: UILabel!
    @IBOutlet weak var lblAdvancedHint: UILabel!

    var securityOption: SecurityOption?

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()

        view.accessibilityIdentifier = AccessibilityIdentifiers.ChooseSecurityScreen.view
        cardAdvanced.accessibilityIdentifier = AccessibilityIdentifiers.ChooseSecurityScreen.multiSigCard
        cardSimple.accessibilityIdentifier = AccessibilityIdentifiers.ChooseSecurityScreen.singleSigCard

        switch LandingViewController.flowType {
        case .add:
            AnalyticsManager.shared.recordView(.onBoardChooseSecurity, sgmt: AnalyticsManager.shared.chooseSecuritySgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strCreate))
        case .restore:
            // not used now
            AnalyticsManager.shared.recordView(.onBoardChooseSecurity, sgmt: AnalyticsManager.shared.chooseSecuritySgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strRestore))
        case .watchonly:
            AnalyticsManager.shared.recordView(.onBoardWatchOnlyChooseSecurity, sgmt: AnalyticsManager.shared.chooseSecuritySgmt(onBoardParams: OnBoardManager.shared.params, flow: AnalyticsManager.OnBoardFlow.strCreate))
        }
    }

    func setContent() {
        lblTitle.text = NSLocalizedString("id_choose_security_policy", comment: "")
        lblHint.text = NSLocalizedString("id_once_selected_this_spending", comment: "")
        lblSimpleTitle.text = NSLocalizedString("id_singlesig", comment: "")
        lblSimpleHint.text = NSLocalizedString("id_your_funds_are_secured_by_a", comment: "")
        lblAdvancedTitle.text = NSLocalizedString("id_multisig_shield", comment: "")
        lblAdvancedHint.text = NSLocalizedString("id_your_funds_are_secured_by", comment: "")
    }

    func setStyle() {
        cardSimple.layer.cornerRadius = 5.0
        cardAdvanced.layer.cornerRadius = 5.0
    }

    func setActions() {
        let tapGesture1 = UITapGestureRecognizer(target: self, action: #selector(didPressCardSimple))
        cardSimple.addGestureRecognizer(tapGesture1)
        let tapGesture2 = UITapGestureRecognizer(target: self, action: #selector(didPressCardAdvanced))
        cardAdvanced.addGestureRecognizer(tapGesture2)
    }

    @objc func didPressCardSimple() {
        switch LandingViewController.flowType {
        case .watchonly:
            DropAlert().warning(message: NSLocalizedString("id_this_feature_is_coming_soon", comment: ""), delay: 3)
        default:
            securityOption = .single
            selectLength()
        }
    }

    @objc func didPressCardAdvanced() {
        switch LandingViewController.flowType {
        case .watchonly:
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)

            if let vc = storyboard.instantiateViewController(withIdentifier: "ChooseNetworkViewController") as? ChooseNetworkViewController {
                vc.watchOnlySecurityOption = .multi
                navigationController?.pushViewController(vc, animated: true)
            }
        default:
            securityOption = .multi
            selectLength()
        }
    }

    func selectLength() {
        let storyboard = UIStoryboard(name: "Shared", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "DialogMnemonicLengthViewController") as? DialogMnemonicLengthViewController {
            vc.modalPresentationStyle = .overFullScreen
            vc.delegate = self
            present(vc, animated: false, completion: nil)
        }
    }

    func next(_ lenght: MnemonicLengthOption) {
        switch securityOption {
        case .single:
            OnBoardManager.shared.params?.singleSig = true
        case .multi:
            OnBoardManager.shared.params?.singleSig = false
        default:
            break
        }
        OnBoardManager.shared.params?.mnemonicSize = lenght.rawValue
        let storyboard = UIStoryboard(name: "Recovery", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryInstructionViewController")
        navigationController?.pushViewController(vc, animated: true)
    }
}

extension ChooseSecurityViewController: DialogMnemonicLengthViewControllerDelegate {
    func didSelect(_ option: MnemonicLengthOption) {
        next(option)
    }
}
