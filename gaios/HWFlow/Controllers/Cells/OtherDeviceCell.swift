import UIKit

class OtherDeviceCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var btnDisclose: UIButton!
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
    }

    func configure() {
        lblType.text = "Serial F2910-1100-5120"
        lblName.text = "Ledger Wallet from Basak"
        lblHint.text = "Fast transactions on the Lightning Network, powered by Greenlight."
        setStyle()
    }

    func setStyle() {
        lblType.font = UIFont.systemFont(ofSize: 10.0, weight: .regular)
        lblName.font = UIFont.systemFont(ofSize: 18.0, weight: .bold)
        lblHint.font = UIFont.systemFont(ofSize: 12.0, weight: .regular)
        lblType.textColor = .white.withAlphaComponent(0.6)
        lblName.textColor = .white
        lblHint.textColor = .white.withAlphaComponent(0.6)
    }
}
