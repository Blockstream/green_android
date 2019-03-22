import Foundation
import UIKit

class WalletCardView: UICollectionViewCell {

    @IBOutlet weak var balance: UILabel!
    @IBOutlet weak var balanceFiat: UILabel!
    @IBOutlet weak var walletName: UILabel!
    @IBOutlet weak var networkImage: UIImageView!
    @IBOutlet weak var unit: UILabel!
    var bgView: UIView?
    var layerGradient: CAGradientLayer?

    override func awakeFromNib() {
        super.awakeFromNib()
        bgView = UIView.init(frame: self.contentView.bounds)
        layerGradient = bgView?.makeGradientCard()
        bgView?.layer.insertSublayer(layerGradient!, at: 0)
        backgroundView = bgView
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        layerGradient?.frame = self.bounds
        layerGradient?.setNeedsDisplay()
    }
}
