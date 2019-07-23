import UIKit

class WalletMiniCollection: UICollectionViewCell {

    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var bgView: UIView!

    var gradientView: UIView?
    var layerGradient: CAGradientLayer?

    override func awakeFromNib() {
        super.awakeFromNib()
        gradientView = UIView.init(frame: self.contentView.bounds)
        layerGradient = gradientView?.makeGradientCard()
        gradientView?.layer.insertSublayer(layerGradient!, at: 0)
        bgView.addSubview(gradientView!)
        bgView.sendSubviewToBack(gradientView!)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        layerGradient?.frame = self.bounds
        layerGradient?.setNeedsDisplay()
    }

}
