import UIKit

class LTInfoCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint1: UILabel!
    @IBOutlet weak var lblHint2: UILabel!

    static var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblTitle.setStyle(.sectionTitle)
        lblHint1.setStyle(.txt)
        lblHint2.setStyle(.txtCard)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: LTInfoCellModel) {
        lblTitle.text = model.title
        lblHint1.text = model.hint1
        lblHint2.text = model.hint2
    }
}
