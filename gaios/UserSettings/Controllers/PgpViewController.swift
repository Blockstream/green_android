import Foundation
import UIKit
import PromiseKit

class PgpViewController: KeyboardViewController {

    @IBOutlet weak var subtitle: UILabel!
    @IBOutlet weak var textarea: UITextView!
    @IBOutlet weak var btnSave: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_pgp_key", comment: "")
        subtitle.text = NSLocalizedString("id_enter_a_pgp_public_key_to_have", comment: "")
        textarea.text = WalletManager.current?.currentSession?.settings?.pgp ?? ""
        btnSave.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)
        btnSave.addTarget(self, action: #selector(save), for: .touchUpInside)
        setStyle()
    }

    func setStyle() {
        btnSave.setStyle(.primary)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        textarea.becomeFirstResponder()
    }

    @objc func save(_ sender: UIButton) {
        guard let session = WalletManager.current?.currentSession,
              let settings = session.settings else { return }
        let bgq = DispatchQueue.global(qos: .background)
        let value = settings.pgp
        settings.pgp = textarea.text
        firstly {
            self.startAnimating()
            return Guarantee()
        }.then(on: bgq) {
            session.changeSettings(settings: settings)
        }.ensure {
            self.stopAnimating()
        }.done {_ in
            self.navigationController?.popViewController(animated: true)
        }.catch {_ in
            settings.pgp = value
            let alert = UIAlertController(title: NSLocalizedString("id_pgp_key", comment: ""), message: NSLocalizedString("id_invalid_pgp_key", comment: ""), preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { (_: UIAlertAction) in
            })
            self.present(alert, animated: true, completion: nil)
        }
    }
}
