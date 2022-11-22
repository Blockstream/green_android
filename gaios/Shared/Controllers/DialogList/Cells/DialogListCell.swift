import UIKit

class DialogListCell: UITableViewCell {

    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblTitle: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(_ model: DialogListCellModel) {
        icon.isHidden = true
        if let img = model.icon {
            icon.image = img
            icon.isHidden = false
        }
        lblTitle.text = model.title
    }
}
