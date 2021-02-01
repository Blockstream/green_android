import Foundation
import UIKit
import PromiseKit

class PgpViewController: KeyboardViewController {

    @IBOutlet var content: PgpView!

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_pgp_key", comment: "")
        content.subtitle.text = NSLocalizedString("id_enter_a_pgp_public_key_to_have", comment: "")
        content.textarea.text = Settings.shared?.pgp ?? ""
        content.button.setTitle(NSLocalizedString("id_save", comment: ""), for: .normal)
        content.button.addTarget(self, action: #selector(save), for: .touchUpInside)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        content.button.setGradient(true)
    }

    @objc func save(_ sender: UIButton) {
        guard let settings = Settings.shared else { return }
        let bgq = DispatchQueue.global(qos: .background)
        let session = getGAService().getSession()
        let value = settings.pgp
        settings.pgp = content.textarea.text
        firstly {
            self.startAnimating()
            return Guarantee()
        }.compactMap(on: bgq) {
            try JSONSerialization.jsonObject(with: JSONEncoder().encode(settings), options: .allowFragments) as? [String: Any]
        }.compactMap(on: bgq) { details in
            try session.changeSettings(details: details)
        }.then(on: bgq) { call in
            call.resolve()
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

@IBDesignable
class PgpView: UIView {

    @IBOutlet weak var subtitle: UILabel!
    @IBOutlet weak var textarea: UITextView!
    @IBOutlet weak var button: UIButton!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }
}
