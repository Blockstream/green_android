import Foundation
import UIKit

class WalletFullCardView: UIView {

    @IBOutlet weak var shadowView: UIView!
    @IBOutlet weak var walletName: UILabel!
    @IBOutlet weak var balance: UILabel!
    @IBOutlet weak var balanceFiat: UILabel!
    @IBOutlet weak var sendView: UIView!
    @IBOutlet weak var receiveView: UIView!
    @IBOutlet weak var actionsView: UIStackView!
    @IBOutlet weak var sendLabel: UILabel!
    @IBOutlet weak var receiveLabel: UILabel!
    @IBOutlet weak var stackButton: UIButton!
    @IBOutlet weak var networkImage: UIImageView!
    @IBOutlet weak var sendImage: UIImageView!
    @IBOutlet weak var receiveImage: UIImageView!
    @IBOutlet weak var backgroundView: UIView!
    @IBOutlet weak var unit: UILabel!
    @IBOutlet weak var assetsLabel: UILabel!
    @IBOutlet weak var assetsView: UIView!
    @IBOutlet weak var assetsHeight: NSLayoutConstraint!

    override func awakeFromNib() {
        super.awakeFromNib()
        let isLiquid = getGdkNetwork(getNetwork()).liquid
        sendLabel.text = NSLocalizedString("id_send", comment: "").capitalized
        receiveLabel.text = NSLocalizedString("id_receive", comment: "").capitalized
        sendImage.tintColor = isLiquid ? .white : UIColor.customMatrixGreen()
        receiveImage.tintColor = isLiquid ? .white : UIColor.customMatrixGreen()
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        let gradient = CAGradientLayer()
        let isLiquid = getGdkNetwork(getNetwork()).liquid
        gradient.colors = isLiquid ? [UIColor.cardBlueDark().cgColor, UIColor.cardBlueMedium().cgColor, UIColor.cardBlueLight().cgColor] : [UIColor.cardDark().cgColor, UIColor.cardMedium().cgColor, UIColor.cardLight().cgColor]
        gradient.locations = [0.0, 0.5, 1.0]
        gradient.startPoint = CGPoint(x: 0.0, y: 1.0)
        gradient.endPoint = CGPoint(x: 1.0, y: 1.0)
        gradient.frame = backgroundView.bounds
        backgroundView.layer.insertSublayer(gradient, at: 0)
        backgroundView.layer.masksToBounds = true

        sendView.layer.maskedCorners = [.layerMinXMaxYCorner]
        receiveView.layer.maskedCorners = [.layerMaxXMaxYCorner]

        if isLiquid {
            sendView.backgroundColor = UIColor.blueLight()
            receiveView.backgroundColor = UIColor.blueLight()
        } else {
            assetsHeight.constant = 0
            assetsView.layoutIfNeeded()
        }
    }
}
