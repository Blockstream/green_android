import UIKit

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
        // Initialization code
        // Initialization code
        btnDisclose.isUserInteractionEnabled = false
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
    }

    class var identifier: String { return String(describing: self) }

    override func prepareForReuse() {
        super.prepareForReuse()
        imgLight.isHidden = true
    }

    func configure(model: AccountSelectSubCellModel, isLast: Bool) {
        let name = model.account.localizedName()
        let type = model.account.type.typeStringId.localized.uppercased()
        let isSS = getGdkNetwork(model.account.network ?? "mainnet").electrum ? true : false
        let security = (isSS ? "Singlesig" : "Multisig").uppercased()

        lblName.text = name
        lblType.text = security + " / " + type
        borderLeft.isHidden = false
        borderRight.isHidden = false
        borderBottom.isHidden = isLast == false
    }
}
