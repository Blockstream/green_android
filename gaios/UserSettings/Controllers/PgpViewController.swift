import Foundation
import UIKit
import PromiseKit
import gdk

class PgpViewController: KeyboardViewController {

    @IBOutlet weak var subtitle: UILabel!
    @IBOutlet weak var textarea: UITextView!
    @IBOutlet weak var btnSave: UIButton!

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_pgp_key", comment: "")
        subtitle.text = NSLocalizedString("id_enter_a_pgp_public_key_to_have", comment: "")
        btnSave.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)
        btnSave.addTarget(self, action: #selector(save), for: .touchUpInside)
        setStyle()
        textarea.text = getPgp() ?? ""
    }

    func setStyle() {
        btnSave.setStyle(.primary)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        textarea.becomeFirstResponder()
    }

    func getPgp() -> String? {
        return WalletManager.current?.activeSessions.values
            .filter { !$0.gdkNetwork.electrum }
            .map { $0.settings?.pgp ?? "" }
            .filter { !$0.isEmpty }
            .first
    }

    func setPgp(pgp: String) -> Promise<Void> {
         let sessions = WalletManager.current?.activeSessions.values
            .filter { !$0.gdkNetwork.electrum }
         return Promise<Void>.chain(sessions ?? [], 1) { self.changeSettings(session: $0, pgp: pgp).asVoid() }.asVoid()
    }

    func changeSettings(session: SessionManager, pgp: String) -> Promise<Void> {
        guard let settings = session.settings else { return Promise().asVoid() }
        let bgq = DispatchQueue.global(qos: .background)
        settings.pgp = pgp
        return Guarantee().then(on: bgq) { session.changeSettings(settings: settings) }.asVoid()
    }

    @objc func save(_ sender: UIButton) {
        guard let txt = self.textarea.text, !txt.isEmpty else { return }
        let bgq = DispatchQueue.global(qos: .background)
        firstly { self.startAnimating(); return Guarantee() }
        .then(on: bgq) { self.setPgp(pgp: txt) }
        .ensure { self.stopAnimating() }
        .done {_ in self.navigationController?.popViewController(animated: true) }
        .catch {_ in self.showError(NSLocalizedString("id_invalid_pgp_key", comment: "")) }
    }
}
