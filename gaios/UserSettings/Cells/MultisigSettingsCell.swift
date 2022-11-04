import UIKit

class MultisigSettingsCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var lblHint: UILabel!
    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var disclosure: UIImageView!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblTitle.textColor = .white
        lblHint.textColor = UIColor.customGrayLight()
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    var viewModel: MultisigSettingsCellModel? {
        didSet {
            lblTitle.text = viewModel?.title
            lblHint.text = viewModel?.subtitle
            disclosure.isHidden = !(viewModel?.disclosure ?? false)
            disclosure.image = viewModel?.disclosureImage
        }
    }
}
