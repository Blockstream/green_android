import UIKit

class WalletEmptyCell: UITableViewCell {

    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var bgContainer: UIView!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

    }

    func configure(_ item: WalletListItem) {
        self.lblTitle.text = item.title
        lblTitle.textColor = UIColor.customGrayLight()
        self.icon.image = item.icon.maskWithColor(color: UIColor.customGrayLight())
        bgContainer.layer.borderWidth = 1.0
        bgContainer.layer.borderColor = UIColor.customGrayLight().cgColor
        bgContainer.cornerRadius = 8.0
    }
}
