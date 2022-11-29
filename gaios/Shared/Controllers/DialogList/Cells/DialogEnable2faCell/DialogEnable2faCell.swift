import UIKit

class DialogEnable2faCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var btnAction: UIButton!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(_ model: DialogCellModel) {
        guard let model = model as? DialogEnable2faCellModel else { return }
        lblTitle.text = model.title
        lblHint.text = model.hint
        btnAction.setTitle(model.actionTitle, for: .normal)
        btnAction.setStyle(.primary)
    }
}
