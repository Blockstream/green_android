import UIKit

class AssetSelectCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var imgView: UIImageView!
    @IBOutlet weak var lblAsset: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
    }

    func configure(model: AssetSelectCellModel) {
        self.lblAsset.text = model.asset?.name ?? model.asset?.assetId
        self.imgView?.image = model.icon
    }
}
