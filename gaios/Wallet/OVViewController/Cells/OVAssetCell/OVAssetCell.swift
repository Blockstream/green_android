import UIKit

class OVAssetCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var imgView: UIImageView!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblBalance1: UILabel!
    @IBOutlet weak var lblBalance2: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(icon: UIImage?,
                   lblAsset: String,
                   lblBalance1: String,
                   lblBalance2: String
    ) {
        self.lblAsset.text = lblAsset
        self.lblBalance1.text = lblBalance1
        self.lblBalance2.text = lblBalance2
    }
}
