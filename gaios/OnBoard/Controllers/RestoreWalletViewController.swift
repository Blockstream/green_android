import UIKit

class RestoreWalletViewController: UIViewController {

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

        view.accessibilityIdentifier = AccessibilityIdentifiers.RestoreWalletScreen.view
        cardMultiSig.accessibilityIdentifier = AccessibilityIdentifiers.RestoreWalletScreen.cardMultiSig
        cardSingleSig.accessibilityIdentifier = AccessibilityIdentifiers.RestoreWalletScreen.cardSingleSig
    }

    func setContent() {
        title = ""
        lblTitle.text = NSLocalizedString("id_restore_a_wallet", comment: "")
        lblHint.text = NSLocalizedString("id_import_a_wallet_created_on", comment: "")
        lblSingleSigTitle.text = NSLocalizedString("id_singlesig", comment: "")
        lblSingleSigHint.text = NSLocalizedString("Restore a Singlesig wallet created on Blockstream Green, or import a wallet created with other apps. This option only works with singlesig wallets using BIP39 mnemonics, and following the BIP44, BIP49, or BIP84 derivations.", comment: "")
        lblMultiSigTitle.text = NSLocalizedString("id_multisig_shield", comment: "")
        lblMultiSigHint.text = NSLocalizedString("Import a Multisig Shield wallet created on Blockstream Green.", comment: "")
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
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        if let vc = storyboard.instantiateViewController(withIdentifier: "ChooseNetworkViewController") as? ChooseNetworkViewController {
            vc.restoreSingleSig = restoreSingleSig
            navigationController?.pushViewController(vc, animated: true)
        }

    }
}
