import UIKit

class OnBoardInfoCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        bg.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    class var identifier: String { return String(describing: self) }

    override func prepareForReuse() {
        super.prepareForReuse()
    }

    func configure(model: OnBoardInfoCellModel) {
        icon.image = model.icon
        lblTitle.text = model.title
        lblHint.text = model.hint
    }
}
