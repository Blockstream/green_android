import UIKit

class WalletHDCell: UITableViewCell {

    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblTitle: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()

    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

    }

    func configure(_ item: WalletListItem) {
        self.lblTitle.text = item.title
        self.icon.image = item.icon
    }
}
