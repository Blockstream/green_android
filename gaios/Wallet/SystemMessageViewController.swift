import Foundation
import UIKit
import PromiseKit

class SystemMessageViewController: UIViewController {

    @IBOutlet weak var textView: UITextView!
    @IBOutlet weak var acceptLabel: UILabel!
    @IBOutlet weak var acceptCheck: DesignableButton!
    @IBOutlet weak var cancelBtn: UIButton!
    @IBOutlet weak var confirmBtn: UIButton!
    var msg: SystemMessage!

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_system_message", comment: "")
        textView.text = msg.text
        acceptLabel.text = NSLocalizedString("id_i_confirm_i_have_read_and", comment: "")
        cancelBtn.setTitle(NSLocalizedString("id_later", comment: ""), for: .normal)
        confirmBtn.setTitle(NSLocalizedString("id_accept", comment: ""), for: .normal)
        confirmBtn.isEnabled = false
        reload()

        AnalyticsManager.shared.recordView(.systemMessage, sgmt: AnalyticsManager.shared.sessSgmt(AccountsManager.shared.current))
    }

    func reload() {
        let accept = confirmBtn.isEnabled
        acceptCheck.backgroundColor = accept ? UIColor.customMatrixGreen() : UIColor.clear
        acceptCheck.setImage(accept ? UIImage(named: "check") : nil, for: UIControl.State.normal)
        acceptCheck.tintColor = UIColor.white
        acceptCheck.layer.borderWidth = 1.0
        acceptCheck.layer.borderColor =  UIColor.customTitaniumLight().cgColor
        acceptCheck.layer.cornerRadius = 2.0
        if accept {
            confirmBtn.setStyle(.primary)
        } else {
            confirmBtn.setStyle(.primaryDisabled)
        }
    }

    @IBAction func acceptCheckClick(_ sender: Any) {
        confirmBtn.isEnabled = !confirmBtn.isEnabled
        reload()
    }

    @IBAction func cancelBtn(_ sender: Any) {
        navigationController?.popViewController(animated: true)
    }

    @IBAction func confirmBtn(_ sender: Any) {
        let bgq = DispatchQueue.global(qos: .background)
        let session = WalletManager.current?.sessions.filter { $0.key == msg.network }.values.first
        Guarantee().compactMap { session }
            .then(on: bgq) { $0.ackSystemMessage(message: self.msg.text) }
            .done { _ in self.navigationController?.popViewController(animated: true) }
            .catch { _ in self.showError("Error removing system message") }
    }
}
