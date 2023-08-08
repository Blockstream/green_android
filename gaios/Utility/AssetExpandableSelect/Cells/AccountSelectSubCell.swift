import UIKit
import gdk

class AccountSelectSubCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var btnDisclose: UIButton!
    @IBOutlet weak var imgMS: UIImageView!
    @IBOutlet weak var imgSS: UIImageView!
    @IBOutlet weak var imgLight: UIImageView!
    @IBOutlet weak var lblType: UILabel!
    @IBOutlet weak var lblName: UILabel!

    @IBOutlet weak var borderBottom: UIView!
    @IBOutlet weak var borderLeft: UIView!
    @IBOutlet weak var borderRight: UIView!

    override func awakeFromNib() {
        super.awakeFromNib()
        btnDisclose.isUserInteractionEnabled = false
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
    }

    class var identifier: String { return String(describing: self) }

    override func prepareForReuse() {
        super.prepareForReuse()
    }

    func configure(model: AccountSelectSubCellModel, isLast: Bool) {
        lblName.text = model.account.localizedName
        lblType.text = model.account.type.longText.uppercased()
        imgSS.isHidden = !model.account.type.singlesig
        imgMS.isHidden = !model.account.type.multisig
        imgLight.isHidden = !model.account.type.lightning
        borderLeft.isHidden = false
        borderRight.isHidden = false
        borderBottom.isHidden = true // isLast == false
    }
}
