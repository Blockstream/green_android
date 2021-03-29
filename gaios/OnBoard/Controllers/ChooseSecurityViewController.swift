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

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()
    }

    func setContent() {
        lblTitle.text = "Choose your Security"
        lblHint.text = "Once selected, this spending policy cannot be changed. For tips on what type of security is best for you, visit our Help Center."
        lblSimpleTitle.text = "Singlesig"
        lblSimpleHint.text = "Your funds are secured by a single key held on your device. Simpler to set up and operate than multisig. If in doubt, select this option."
        lblAdvancedTitle.text = "Multisig Shield"
        lblAdvancedHint.text = "Your funds are secured by multiple keys, with one key on your device and another on our servers. For enhanced security."
    }

    func setStyle() {
        cardSimple.layer.cornerRadius = 5.0
        cardAdvanced.layer.cornerRadius = 5.0

        cardSimple.alpha = 0.5
    }

    func setActions() {
        let tapGesture1 = UITapGestureRecognizer(target: self, action: #selector(didPressCardSimple))
        cardSimple.addGestureRecognizer(tapGesture1)
        let tapGesture2 = UITapGestureRecognizer(target: self, action: #selector(didPressCardAdvanced))
        cardAdvanced.addGestureRecognizer(tapGesture2)
    }

    @objc func didPressCardSimple() {
        // next()
    }

    @objc func didPressCardAdvanced() {
        next()
    }

    func next() {
        switch LandingViewController.flowType {
        case .add:
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "WalletNameViewController")
            navigationController?.pushViewController(vc, animated: true)
        case .restore:
            let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
            let vc = storyboard.instantiateViewController(withIdentifier: "RecoveryPhraseViewController")
            navigationController?.pushViewController(vc, animated: true)
        }

    }
}
