import UIKit

class AnyAssetCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var assetSubview: UIView!
    @IBOutlet weak var imgView: UIImageView!
    @IBOutlet weak var lblAny: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        assetSubview.cornerRadius = 5.0
    }

    func configure() {
        self.lblAny.text = "Receive any Liquid Asset"
    }
}
