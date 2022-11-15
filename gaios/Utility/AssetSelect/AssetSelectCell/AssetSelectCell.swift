import UIKit

class AssetSelectCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var assetSubview: UIView!
    @IBOutlet weak var imgView: UIImageView!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var ampSubview: UIView!
    @IBOutlet weak var lblAmp: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        ampSubview.cornerRadius = 5.0
        assetSubview.cornerRadius = 5.0
    }

    func configure(model: AssetSelectCellModel) {
        let name = model.asset?.name ?? model.asset?.assetId
        self.lblAsset.text = name
        self.imgView?.image = model.icon

        ampSubview.isHidden = true
        assetSubview.borderWidth = 0.0

        if let ampWarn = model.ampWarn {
            lblAmp.text = ampWarn
            ampSubview.isHidden = false
            assetSubview.borderWidth = 2.0
            assetSubview.borderColor = UIColor.gGreenMatrix()
        }
    }
}
