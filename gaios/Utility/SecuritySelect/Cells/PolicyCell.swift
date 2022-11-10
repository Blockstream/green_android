import UIKit

class PolicyCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var btnDisclose: UIButton!
    @IBOutlet weak var imgMS: UIImageView!
    @IBOutlet weak var imgSS: UIImageView!
    @IBOutlet weak var imgLight: UIImageView!
    @IBOutlet weak var lblType: UILabel!
    @IBOutlet weak var lblName: UILabel!
    @IBOutlet weak var lblHint: UILabel!

    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        bg.cornerRadius = 5.0
        btnDisclose.isUserInteractionEnabled = false
        btnDisclose.backgroundColor = UIColor.gGreenMatrix()
        btnDisclose.cornerRadius = 4.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    class var identifier: String { return String(describing: self) }

    override func prepareForReuse() {
        super.prepareForReuse()
        imgLight.isHidden = true
    }

    func configure(model: PolicyCellModel) {
        imgSS.isHidden = !model.isSS
        imgMS.isHidden = model.isSS

        lblType.text = model.type
        lblName.text = model.name
        lblHint.text = model.hint
        if model.isLight == true {
            imgLight.isHidden = false
            imgSS.isHidden = true
            imgMS.isHidden = true
        }
    }
}
