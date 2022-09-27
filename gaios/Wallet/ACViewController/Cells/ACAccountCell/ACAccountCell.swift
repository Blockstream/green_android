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

    var action: (() -> Void)?
    var showAll: Bool?
    var viewModel: ACAccountCellModel?

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
        actionBtn.isHidden = true
    }

    func configure(viewModel: ACAccountCellModel, showAll: Bool, action: (() -> Void)?) {
        self.viewModel = viewModel
        self.showAll = showAll
        self.action = action

        self.lblAccountTitle.text = viewModel.name
        self.lblType.text = viewModel.type.localized
        self.lblSecurity.text = viewModel.security
        self.lblBalance.text = viewModel.value
        self.lblFiat.text = viewModel.fiat

        actionBtn.isHidden = action == nil || !showAll
        bg.backgroundColor = UIColor.customMatrixGreen()
        bgShadow.backgroundColor = showAll ? .clear : .customMatrixGreen()
    }

    @IBAction func actionBtn(_ sender: Any) {
        self.action?()
    }
}
