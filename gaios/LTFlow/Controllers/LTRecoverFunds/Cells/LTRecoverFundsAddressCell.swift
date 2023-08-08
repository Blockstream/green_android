import UIKit

protocol LTRecoverFundsAddressCellDelegate: AnyObject {
    func didChange(address: String)
    func qrcodeScanner()
}

class LTRecoverFundsAddressCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var editField: UITextField!
    @IBOutlet weak var errorLabel: UILabel!
    @IBOutlet weak var pasteButton: UIButton!
    @IBOutlet weak var qrcodeButton: UIButton!
    @IBOutlet weak var cancelButton: UIButton!
    
    weak var delegate: LTRecoverFundsAddressCellDelegate?

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(address: String) {
        editField.text = address
        errorLabel.text = ""
    }

    @objc func triggerTextChange() {
        if let text = editField.text {
            delegate?.didChange(address: text)
        }
        cancelButton.isHidden = !(editField.text?.count ?? 0 > 0)
        pasteButton.isHidden = (editField.text?.count ?? 0 > 0)
    }

    @IBAction func pasteTap(_ sender: Any) {
        if let text = UIPasteboard.general.string {
            editField.text = text
            triggerTextChange()
            delegate?.didChange(address: text)
        }
    }

    @IBAction func cancelTap(_ sender: Any) {
        editField.text = ""
        triggerTextChange()
    }

    @IBAction func qrcodeTap(_ sender: Any) {
        delegate?.qrcodeScanner()
    }

    @IBAction func addressDidChange(_ sender: Any) {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(self.triggerTextChange), object: nil)
        perform(#selector(self.triggerTextChange), with: nil, afterDelay: 0.5)
    }
}
