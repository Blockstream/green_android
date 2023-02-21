import UIKit

protocol AddRecipientCellDelegate: AnyObject {
    func action()
}

class AddRecipientCell: UITableViewCell {

    @IBOutlet weak var btnAddRecipient: UIButton!
    weak var delegate: AddRecipientCellDelegate?

    override func awakeFromNib() {
        super.awakeFromNib()

        btnAddRecipient.setStyle(.outlinedGray)
        btnAddRecipient.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
        btnAddRecipient.setTitle("id_add_recipient".localized, for: .normal)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() { }

    func configure() { }

    @IBAction func btnAddRecipient(_ sender: Any) {
        delegate?.action()
    }
}
