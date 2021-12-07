import UIKit

class ManualRestoreViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var cardSingleSig: UIView!
    @IBOutlet weak var lblSingleSigTitle: UILabel!
    @IBOutlet weak var lblSingleSigHint: UILabel!

    @IBOutlet weak var cardMultiSig: UIView!
    @IBOutlet weak var lblMultiSigTitle: UILabel!
    @IBOutlet weak var lblMultiSigHint: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()
    }

    func setContent() {
        title = ""
        lblTitle.text = "Manual Restore"
        lblHint.text = "Choose the security policy you want to restore this  wallet on."
        lblSingleSigTitle.text = NSLocalizedString("id_singlesig", comment: "")
        lblSingleSigHint.text = NSLocalizedString("id_restore_a_singlesig_wallet", comment: "")
        lblMultiSigTitle.text = NSLocalizedString("id_multisig_shield", comment: "")
        lblMultiSigHint.text = NSLocalizedString("id_import_a_multisig_shield_wallet", comment: "")
    }

    func setStyle() {
        cardSingleSig.layer.cornerRadius = 5.0
        cardMultiSig.layer.cornerRadius = 5.0
    }

    func setActions() {
        let tapGesture1 = UITapGestureRecognizer(target: self, action: #selector(didPressCardSingleSig))
        cardSingleSig.addGestureRecognizer(tapGesture1)
        let tapGesture2 = UITapGestureRecognizer(target: self, action: #selector(didPressCardMultisig))
        cardMultiSig.addGestureRecognizer(tapGesture2)
    }

    @objc func didPressCardSingleSig() {
        next(restoreSingleSig: true)
    }

    @objc func didPressCardMultisig() {
        next(restoreSingleSig: false)
    }

    func next(restoreSingleSig: Bool) {
        OnBoardManager.shared.params?.singleSig = restoreSingleSig
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "WalletNameViewController")
        self.navigationController?.pushViewController(vc, animated: true)
    }
}
