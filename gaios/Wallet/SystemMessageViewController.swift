import Foundation
import UIKit
import PromiseKit

class SystemMessageViewController: UIViewController {

    @IBOutlet var content: SystemMessageView!
    var systemMessage: Event!
    private var text: String {
        get { return systemMessage.value["text"] as! String }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = NSLocalizedString("id_system_message", comment: "")
        content.textView.text = text
        content.acceptLabel.text = NSLocalizedString("id_i_confirm_i_have_read_and", comment: "")
        content.laterButton.setTitle(NSLocalizedString("id_later", comment: ""), for: .normal)
        content.confirmButton.setTitle(NSLocalizedString("id_accept", comment: ""), for: .normal)
        content.confirmButton.isEnabled = false
        updateButtons()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        content.confirmButton.updateGradientLayerFrame()
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
                try getSession().ackSystemMessage(message: self.text)
            }.then(on: bgq) { twoFactorCall in
                twoFactorCall.resolve(self)
            }.done { _ in
                getGAService().reloadSystemMessage()
                self.navigationController?.popViewController(animated: true)
            }.catch { _ in
                print("Error on remove system message")
            }
        }
    }

    @IBAction func acceptCheckClick(_ sender: Any) {
        content.confirmButton.isEnabled = !content.confirmButton.isEnabled
        updateButtons()
    }

    func updateButtons() {
        let accept = content.confirmButton.isEnabled
        content.acceptCheck.backgroundColor = accept ? UIColor.customMatrixGreen() : UIColor.clear
        content.acceptCheck.layer.borderColor =  UIColor.customTitaniumLight().cgColor
        content.acceptCheck.setImage(accept ? UIImage(named: "check") : nil, for: UIControlState.normal)
        content.acceptCheck.tintColor = UIColor.white
        content.confirmButton.setGradient(accept)
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
}
