import UIKit

class LTNoteCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblNote: UILabel!
    var onEditClick: (() -> Void)?

    static var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    @IBAction func onEdit(_ sender: Any) {
        onEditClick?()
    }

    func configure(model: LTNoteCellModel, onEditClick:  (() -> Void)?) {
        lblNote.text = model.note
        self.onEditClick = onEditClick
    }
}
