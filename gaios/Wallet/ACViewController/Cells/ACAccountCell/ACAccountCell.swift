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

    var action: (() -> Void)?

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
    }

    func configure(showAll: Bool, action: (() -> Void)?) {
        prepareForReuse()

        self.action = action
        bg.backgroundColor = UIColor.customMatrixGreen()
        if showAll { bgShadow.backgroundColor = .clear } else { bgShadow.backgroundColor = UIColor.customMatrixGreen() }
        self.lblAccountTitle.text = "BTC Account"
        self.lblType.text = "Legacy"
        self.lblSecurity.text = "Singlesig"
        self.lblBalance.text = "BTC 0,000000000"
        self.lblFiat.text = "~0,00 USD"

        self.actionBtn.isHidden = action == nil
    }

    @IBAction func actionBtn(_ sender: Any) {
        self.action?()
    }
}
