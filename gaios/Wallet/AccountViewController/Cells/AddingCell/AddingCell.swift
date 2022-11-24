import UIKit

class AddingCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblTitle: UILabel!

    var onTap: (() -> Void)?

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
    }

    func configure(model: AddingCellModel,
                   onTap: (() -> Void)?) {
        self.lblTitle.attributedText = model.title
        self.onTap = onTap
    }

    @IBAction func onTap(_ sender: Any) {
        onTap?()
    }
}
