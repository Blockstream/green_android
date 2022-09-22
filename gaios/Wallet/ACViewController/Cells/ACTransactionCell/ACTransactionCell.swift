import UIKit

class ACTransactionCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var imgView: UIImageView!
    @IBOutlet weak var lblStatus: UILabel!
    @IBOutlet weak var lblDate: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblWallet: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.borderWidth = 2.0
        bg.borderColor = .white.withAlphaComponent(0.2)
        bg.cornerRadius = 5.0

        let view = UIView(frame: CGRect(x: 21,
                                        y: frame.size.height - 17,
                                        width: frame.size.width / 3.0,
                                            height: 3))
        view.backgroundColor = UIColor.customMatrixGreen()
        view.layer.shadowOffset = CGSize(width: 0, height: 0)
        view.layer.shadowColor = UIColor.customMatrixGreen().cgColor
        view.layer.shadowRadius = 8
        view.layer.shadowOpacity = 0.8
        addSubview(view)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    var viewModel: ACTransactionCellModel? {
        didSet {
            self.lblStatus.text = viewModel?.status
            self.lblDate.text = viewModel?.date
            self.lblAmount.text = viewModel?.value
            self.lblWallet.text = viewModel?.subaccountName
        }
    }
}
