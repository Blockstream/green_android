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
        let wm = WalletManager.current
        let networks: [NetworkSecurityCase] = wm?.testnet ?? false ? [.testnetMS, .testnetLiquidMS] : [.liquidMS, .testnetLiquidMS]
        for network in networks {
            let session = wm?.activeSessions.filter { $0.key == network.network }.first?.value
            if let pgp = session?.settings?.pgp {
                return pgp
            }
        }
        return nil
    }

    func setPgp(pgp: String) -> Promise<Void> {
        let wm = WalletManager.current
        let sessions = wm?.activeSessions.values.filter { !$0.gdkNetwork.electrum && $0.logged }
        guard let sessions = sessions, !sessions.isEmpty else {
            return Promise().asVoid()
        }
        let promises = sessions.compactMap { changeSettings(session: $0, pgp: pgp) }
        return when(fulfilled: promises).asVoid()
    }

    func changeSettings(session: SessionManager, pgp: String) -> Promise<Void> {
        guard let settings = session.settings else {
            return Promise().asVoid()
        }
        settings.pgp = pgp
        return session.changeSettings(settings: settings).asVoid()
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
