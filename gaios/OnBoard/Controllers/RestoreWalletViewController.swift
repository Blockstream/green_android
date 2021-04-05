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
        lblTitle.text = NSLocalizedString("id_restore_a_wallet", comment: "")
        lblHint.text = NSLocalizedString("id_import_a_wallet_created_on", comment: "")
        lblRestoreTitle.text = NSLocalizedString("id_restore_green_wallet", comment: "")
        lblRestoreHint.text = NSLocalizedString("id_bitcoin_is_the_worlds_leading", comment: "")
        lblMigrateTitle.text = NSLocalizedString("id_migrate_another_wallet", comment: "")
        lblMigrateHint.text = NSLocalizedString("id_import_a_wallet_created_with", comment: "")
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
