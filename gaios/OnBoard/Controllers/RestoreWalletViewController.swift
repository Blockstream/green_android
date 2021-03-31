import UIKit

class RestoreWalletViewController: UIViewController {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    @IBOutlet weak var cardRestore: UIView!
    @IBOutlet weak var lblRestoreTitle: UILabel!
    @IBOutlet weak var lblRestoreHint: UILabel!

    @IBOutlet weak var cardMigrate: UIView!
    @IBOutlet weak var lblMigrateTitle: UILabel!
    @IBOutlet weak var lblMigrateHint: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()

        setContent()
        setStyle()
        setActions()
    }

    func setContent() {
        title = ""
        lblTitle.text = "Restore a Wallet"
        lblHint.text = "Import a wallet created with a Blockstream Green app."
        lblRestoreTitle.text = "Restore Green Wallet"
        lblRestoreHint.text = "Bitcoin is the world's leading P2P cryptocurrency network. Select to send and receive bitcoin."
        lblMigrateTitle.text = "Migrate Another Wallet"
        lblMigrateHint.text = "Import a wallet created with other apps. This option only works with singlesig wallets using BIP39 mnemonics, and following the BIP44, BIP49, or BIP84 derivations."
    }

    func setStyle() {
        cardRestore.layer.cornerRadius = 5.0
        cardMigrate.layer.cornerRadius = 5.0

        cardMigrate.alpha = 0.5
    }

    func setActions() {
        let tapGesture1 = UITapGestureRecognizer(target: self, action: #selector(didPressCardRestore))
        cardRestore.addGestureRecognizer(tapGesture1)
        let tapGesture2 = UITapGestureRecognizer(target: self, action: #selector(didPressCardMigrate))
        cardMigrate.addGestureRecognizer(tapGesture2)
    }

    @objc func didPressCardRestore() {
        next()
    }

    @objc func didPressCardMigrate() {
//        next()
    }

    func next() {
        let storyboard = UIStoryboard(name: "OnBoard", bundle: nil)
        let vc = storyboard.instantiateViewController(withIdentifier: "ChooseNetworkViewController")
        navigationController?.pushViewController(vc, animated: true)
    }
}
