import UIKit

class NetworkSelectionTableCell: UITableViewCell {

    @IBOutlet weak var logoImageView: UIImageView!
    @IBOutlet weak var logoWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var networkNameLabel: UILabel!
    @IBOutlet weak var cardView: UIView!
    @IBOutlet weak var indicatorImageView: UIImageView!

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func prepareForReuse() {
        logoImageView.image = nil
        logoWidthConstraint.constant = 24
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
        let logoName = network.liquid ? "btc_liquid_title" : network.icon
        logoImageView.image = UIImage(named: logoName!)
        logoWidthConstraint.constant = network.liquid ? 52 : 24
        networkNameLabel.text = network.name
        selectionStyle = .none
    }
}
