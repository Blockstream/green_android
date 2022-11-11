import UIKit

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
        // Initialization code
        bg.cornerRadius = 5.0
        btnDisclose.isUserInteractionEnabled = false
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
    }

    class var identifier: String { return String(describing: self) }

    override func prepareForReuse() {
        super.prepareForReuse()
        imgLight.isHidden = true
    }

    func configure(model: AccountSelectCellModel) {
        let name = model.account.localizedName()
        let type = model.account.type.typeStringId.localized.uppercased()
        let isSS = getGdkNetwork(model.account.network ?? "mainnet").electrum ? true : false
        let security = (isSS ? "Singlesig" : "Multisig").uppercased()

        lblName.text = name
        lblType.text = security + " / " + type
    }
}
