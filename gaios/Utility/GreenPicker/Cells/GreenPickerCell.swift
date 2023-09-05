import UIKit

class GreenPickerCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblTitle.setStyle(.txtBigger)
        lblHint.setStyle(.txtCard)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: GreenPickerItem,
                   isSelected: Bool) {
        lblTitle.text = model.title
        lblHint.text = model.hint
        lblTitle.textColor = isSelected ? UIColor.gGreenMatrix() : .white
    }
}
