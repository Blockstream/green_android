import UIKit

class NetworkSelectionTableCell: UITableViewCell {

    @IBOutlet weak var logoImageView: UIImageView!
    @IBOutlet weak var networkNameLabel: UILabel!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var indicatorImageView: UIImageView!

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func prepareForReuse() {
        logoImageView.image = nil
        networkNameLabel.text = ""
    }

    func configure(with network: GdkNetwork, selected: Bool) {
        cardView.shadowOpacity = 1
        cardView.shadowColor = .black
        cardView.shadowOffset = CGSize(width: 0, height: 2.0)
        cardView.shadowRadius = 4
        cardView.borderWidth = 1
        cardView.borderColor = selected ? UIColor.customMatrixGreen() : UIColor.customModalMedium()
        let indicatorImageName = selected ? "network_selected" : "network_deselected"
        indicatorImageView.image = UIImage(named: indicatorImageName)
        if let iconName = network.icon {
            logoImageView.image = UIImage(named: iconName)
        }
        networkNameLabel.text = network.name
        selectionStyle = .none
    }
}
