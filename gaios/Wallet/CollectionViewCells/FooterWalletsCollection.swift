import Foundation
import UIKit

class FooterWalletsCollection: UICollectionViewCell {

    @IBOutlet weak var networkImage: UIImageView!
    @IBOutlet weak var message: UILabel!
    @IBOutlet weak var addButton: UIButton!

    override func layoutSubviews() {
        super.layoutSubviews()
        let border = CAShapeLayer()
        let width = self.contentView.frame.width - CGFloat(38)
        let height = self.contentView.frame.height - CGFloat(14)
        border.frame = CGRect.init(x: 10, y: 6, width: width, height: height)
        border.strokeColor = UIColor.customTitaniumMedium().cgColor
        border.fillColor = UIColor.clear.cgColor
        border.lineDashPattern = [3, 3]
        border.lineWidth = 1
        border.lineCap = kCALineCapRound
        border.path = UIBezierPath(roundedRect: border.frame, cornerRadius: 8).cgPath
        let bgView = UIView.init(frame: self.contentView.bounds)
        bgView.layer.insertSublayer(border, at: 0)
        self.backgroundView = bgView
        message.text = NSLocalizedString("id_add_new_account", comment: "")
    }
}
