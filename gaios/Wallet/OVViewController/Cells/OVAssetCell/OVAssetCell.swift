import UIKit

class OVAssetCell: BaseCell {

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

    var viewModel: OVAssetCellModel? {
        didSet {
            self.lblAsset.text = viewModel?.asset?.name ?? viewModel?.asset?.assetId
            self.lblBalance1.text = viewModel?.value ?? ""
            self.lblBalance2.text = viewModel?.fiat ?? ""
            self.imgView?.image = viewModel?.icon
        }
    }
}
