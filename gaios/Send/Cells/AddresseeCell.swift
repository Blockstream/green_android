import UIKit

class AddresseeCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!

    @IBOutlet weak var lblRecipientTitle: UILabel!
    @IBOutlet weak var lblRecipientAddress: UILabel!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var lblDenomination: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblFiat: UILabel!

    var isFiat = false

    override func awakeFromNib() {
        super.awakeFromNib()
        setStyle()
        setContent()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
    }

    func configure(cellModel: AddresseeCellModel) {
        lblRecipientTitle.text = NSLocalizedString("id_recipient", comment: "")
        lblRecipientAddress.text = cellModel.addreessee.address
        lblAmount.text = cellModel.amount
        lblDenomination.text = cellModel.ticker
        lblFiat.isHidden = !cellModel.showFiat
        lblFiat.text = cellModel.fiat
        icon.image = WalletManager.current?.registry.image(for: cellModel.assetId)
    }

    func setStyle() {
        bg.cornerRadius = 8.0
    }

    func setContent() {
    }
}
