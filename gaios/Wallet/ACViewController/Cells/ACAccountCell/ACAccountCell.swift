import UIKit

class ACAccountCell: BaseCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var bgShadow: UIView!
    @IBOutlet weak var lblAccountTitle: UILabel!

    @IBOutlet weak var bgType: UIView!
    @IBOutlet weak var bgSecurity: UIView!

    @IBOutlet weak var lblType: UILabel!
    @IBOutlet weak var lblSecurity: UILabel!

    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var lblBalance: UILabel!
    @IBOutlet weak var actionBtn: UIButton!

    class var identifier: String { return String(describing: self) }

    var action: (() -> Void)? {
        didSet {
            actionBtn.isHidden = action != nil && showAll
        }
    }

    var showAll: Bool = true {
        didSet {
            actionBtn.isHidden = action != nil && showAll
            bgShadow.backgroundColor = showAll ? .clear : .customMatrixGreen()
        }
    }

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.layer.cornerRadius = 6.0
        bgShadow.cornerRadius = 6.0
        bgShadow.alpha = 0.5
        bgType.layer.cornerRadius = 3.0
        bgSecurity.layer.cornerRadius = 3.0
    }

    override func prepareForReuse() {
        lblAccountTitle.text = ""
        lblType.text = ""
        lblSecurity.text = ""
        lblBalance.text = ""
        lblFiat.text = ""
//        lblBalance.isHidden = true
        actionBtn.isHidden = true
        bg.backgroundColor = UIColor.customMatrixGreen()
    }

    var viewModel: ACAccountCellModel? {
        didSet {
            prepareForReuse()
            self.lblAccountTitle.text = viewModel?.name
            self.lblType.text = viewModel?.type
            self.lblSecurity.text = viewModel?.security
            self.lblBalance.text = viewModel?.value
            self.lblFiat.text = viewModel?.fiat
        }
    }

    @IBAction func actionBtn(_ sender: Any) {
        self.action?()
    }
}
