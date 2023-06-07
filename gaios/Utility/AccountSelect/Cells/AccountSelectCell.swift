import UIKit
import gdk

class AccountSelectCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var btnDisclose: UIButton!
    @IBOutlet weak var imgMS: UIImageView!
    @IBOutlet weak var imgSS: UIImageView!
    @IBOutlet weak var imgLight: UIImageView!
    @IBOutlet weak var lblType: UILabel!
    @IBOutlet weak var lblName: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
        btnDisclose.isUserInteractionEnabled = false
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
    }

    class var identifier: String { return String(describing: self) }

    override func prepareForReuse() {
        super.prepareForReuse()
    }

    func configure(model: AccountSelectCellModel) {
        imgSS.isHidden = !model.account.type.singlesig
        imgMS.isHidden = !model.account.type.multisig
        imgLight.isHidden = !model.account.type.lightning
        lblName.text = model.account.localizedName
        lblType.text = model.account.type.subtitle.uppercased()
    }
}
