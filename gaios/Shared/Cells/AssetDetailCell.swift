import UIKit

class AssetDetailCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    func configure(_ title: String, _ hint: String) {
        self.lblTitle.text = title
        self.lblHint.text = hint
    }

    func configureAmount(_ title: String, _ hint: String, _ hideBalance: Bool = false) {
        self.lblTitle.text = title
        self.lblHint.text = hint
        if hideBalance {
            self.lblHint.attributedText = Common.obfuscate(color: .white, size: 14, length: 5)
        }
    }
}
