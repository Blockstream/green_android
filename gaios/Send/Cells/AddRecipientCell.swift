import UIKit

class AddRecipientCell: UITableViewCell {

    @IBOutlet weak var btnAddRecipient: UIButton!
    var action: VoidToVoid?

    override func awakeFromNib() {
        super.awakeFromNib()

        btnAddRecipient.setStyle(.outlinedGray)
        btnAddRecipient.setTitleColor(UIColor.customMatrixGreen(), for: .normal)
        btnAddRecipient.setTitle("Add Recipient", for: .normal)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
    }

    func configure(action: VoidToVoid?) {
        self.action = action
    }

    @IBAction func btnAddRecipient(_ sender: Any) {
        print("btnAddRecipient")
        action?()
    }
}
