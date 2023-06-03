import UIKit

class DialogDenominationCell: UITableViewCell {

    @IBOutlet weak var lblTitle: UILabel!
    @IBOutlet weak var icon: UIImageView!
    
    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblTitle.font = UIFont.systemFont(ofSize: 14.0, weight: .regular)
        lblTitle.textColor = .white
    }

    override func prepareForReuse() {
        lblTitle.text = ""
        lblTitle.textColor = .white
        icon.isHidden = true
    }

    func configure(title: String, isSelected: Bool) {
        lblTitle.text = title
        icon.isHidden = isSelected == false
        lblTitle.textColor = isSelected ? UIColor.gGreenMatrix() : .white
    }
}

