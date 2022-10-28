import UIKit

class AccountCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var detailView: UIView!
    @IBOutlet weak var effectView: UIView!
    @IBOutlet weak var innerEffectView: UIView!
    @IBOutlet weak var btcImg: UIImageView!
    @IBOutlet weak var btnDisclose: UIButton!
    @IBOutlet weak var btnWarn: UIButton!

    @IBOutlet weak var imgMS: UIImageView!
    @IBOutlet weak var imgSS: UIImageView!
    @IBOutlet weak var lblType: UILabel!
    @IBOutlet weak var lblName: UILabel!
    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var lblAmount: UILabel!

    private var sIdx: Int = 0
    private var cIdx: Int = 0
    private var isLast: Bool = false

    static var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.cornerRadius = 5.0
        innerEffectView.layer.cornerRadius = 5.0
        innerEffectView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        btnDisclose.borderWidth = 1.0
        btnDisclose.borderColor = .white
        btnDisclose.cornerRadius = 3.0
        btnWarn.borderWidth = 1.0
        btnWarn.borderColor = .black
        btnWarn.cornerRadius = 16.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        select(selected)
    }

    func configure(model: AccountCellModel,
                   cIdx: Int,
                   sIdx: Int,
                   isLast: Bool) {
        self.cIdx = cIdx
        self.sIdx = sIdx
        self.isLast = isLast

        lblType.text = model.lblType
        lblName.text = model.name
        lblFiat.text = "19.80 USD"
        lblAmount.text = "0.00000100 BTC"
        imgSS.isHidden = !model.isSS
        imgMS.isHidden = model.isSS
        btnWarn.isHidden = true
        btcImg.isHidden = model.isLiquid
        [bg, effectView, btnWarn].forEach { $0?.backgroundColor = model.isLiquid ? UIColor.gAccountLightBlue() : UIColor.gAccountOrange() }
    }

    func updateUI(_ value: Bool) {
        self.detailView.alpha = value ? 1.0 : 0.0
        if self.isLast {
            self.effectView.alpha = 0.0
        } else {
            self.effectView.alpha = value ? 0.0 : 1.0
        }
    }

    func select(_ value: Bool) {
        UIView.animate(withDuration: 0.3, animations: {
            self.updateUI(value)
        })
    }
}
