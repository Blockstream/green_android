import UIKit

class WalletAssetCell: BaseCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var imgView: UIImageView!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblBalance1: UILabel!
    @IBOutlet weak var lblBalance2: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
    }

    func configure(model: WalletAssetCellModel) {
        self.lblAsset.text = model.asset?.name ?? model.asset?.assetId
        self.lblBalance1.text = model.value ?? ""
        self.lblBalance2.text = model.fiat ?? " - "
        self.imgView?.image = model.icon
    }
}
