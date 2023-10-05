import Foundation
import UIKit

protocol AddressEditCellDelegate {
    func qrcodeScanner()
    func paste()
    func addressDidChange(text: String)
}

struct AddressEditCellModel {
    let text: String?
    let error: String?
    let editable: Bool
}

class AddressEditCell: UITableViewCell {
    
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var editField: UITextField!
    @IBOutlet weak var errorLabel: UILabel!
    @IBOutlet weak var pasteButton: UIButton!
    @IBOutlet weak var qrcodeButton: UIButton!
    @IBOutlet weak var cancelButton: UIButton!
    
    private var delegate: AddressEditCellDelegate?
    
    override func awakeFromNib() {
        super.awakeFromNib()
        setStyle()
        setContent()

        let cStyle = editField.defaultTextAttributes[.paragraphStyle, default: NSParagraphStyle()] as? NSParagraphStyle
        let style = cStyle?.mutableCopy() as? NSMutableParagraphStyle
        style?.lineBreakMode = .byTruncatingMiddle
        editField.defaultTextAttributes[.paragraphStyle] = style
    }
    
    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }
    
    override func prepareForReuse() {
    }
    
    func setStyle() {
        bg.cornerRadius = 5.0
    }
    
    func setContent() {
    }
    
    func configure(cellModel: AddressEditCellModel, delegate: AddressEditCellDelegate) {
        editField.text = cellModel.text ?? ""
        errorLabel.text = cellModel.error?.localized ?? ""
        editField.isEnabled = cellModel.editable
        editField.isUserInteractionEnabled = cellModel.editable
        pasteButton.isEnabled = cellModel.editable
        qrcodeButton.isEnabled = cellModel.editable
        cancelButton.isEnabled = cellModel.editable
        cancelButton.isHidden = !(editField.text?.count ?? 0 > 0)
        pasteButton.isHidden = (editField.text?.count ?? 0 > 0)
        self.delegate = delegate
        editField.addDoneButtonToKeyboard(myAction: #selector(self.editField.resignFirstResponder))
    }

    @objc func triggerTextChange() {
        if let text = editField.text {
            delegate?.addressDidChange(text: text)
        }
        cancelButton.isHidden = !(editField.text?.count ?? 0 > 0)
        pasteButton.isHidden = (editField.text?.count ?? 0 > 0)
    }

    @IBAction func pasteTap(_ sender: Any) {
        delegate?.paste()
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
