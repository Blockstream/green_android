import Foundation
import UIKit
import PromiseKit

class SystemMessageViewController: UIViewController {

    @IBOutlet var content: SystemMessageView!
    var systemMessage: Event!
    private var text: String {
        get { return systemMessage.value["text"] as? String ?? "" }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_system_message", comment: "")
        content.textView.text = text
        content.acceptLabel.text = NSLocalizedString("id_i_confirm_i_have_read_and", comment: "")
        content.laterButton.setTitle(NSLocalizedString("id_later", comment: ""), for: .normal)
        content.confirmButton.setTitle(NSLocalizedString("id_accept", comment: ""), for: .normal)
        content.confirmButton.isEnabled = false
        content.reload()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        content.laterButton.addTarget(self, action: #selector(click(_:)), for: .touchUpInside)
        content.confirmButton.addTarget(self, action: #selector(click(_:)), for: .touchUpInside)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        content.laterButton.removeTarget(self, action: #selector(click(_:)), for: .touchUpInside)
        content.confirmButton.removeTarget(self, action: #selector(click(_:)), for: .touchUpInside)
    }

    @objc func click(_ sender: UIButton?) {
        if sender == content.laterButton {
            navigationController?.popViewController(animated: true)
        } else if sender == content.confirmButton {
            let bgq = DispatchQueue.global(qos: .background)
            Guarantee().map(on: bgq) {
                try SessionManager.shared.ackSystemMessage(message: self.text)
            }.then(on: bgq) { twoFactorCall in
                twoFactorCall.resolve()
            }.done { _ in
                SessionManager.shared.notificationManager.reloadSystemMessage()
                self.navigationController?.popViewController(animated: true)
            }.catch { _ in
                print("Error on remove system message")
            }
        }
    }
}

@IBDesignable
class SystemMessageView: UIView {
    @IBOutlet weak var textView: UITextView!
    @IBOutlet weak var acceptLabel: UILabel!
    @IBOutlet weak var acceptCheck: DesignableButton!
    @IBOutlet weak var laterButton: UIButton!
    @IBOutlet weak var confirmButton: UIButton!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setup()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        confirmButton.updateGradientLayerFrame()
    }

    @IBAction func acceptCheckClick(_ sender: Any) {
        confirmButton.isEnabled = !confirmButton.isEnabled
        reload()
    }

    func reload() {
        let accept = confirmButton.isEnabled
        acceptCheck.backgroundColor = accept ? UIColor.customMatrixGreen() : UIColor.clear
        acceptCheck.setImage(accept ? UIImage(named: "check") : nil, for: UIControl.State.normal)
        acceptCheck.tintColor = UIColor.white
        confirmButton.setGradient(accept)
        acceptCheck.layer.borderWidth = 1.0
        acceptCheck.layer.borderColor =  UIColor.customTitaniumLight().cgColor
        acceptCheck.layer.cornerRadius = 2.0
    }
}
