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
    @IBOutlet weak var iconsView: UIView!
    @IBOutlet weak var iconsStack: UIStackView!
    @IBOutlet weak var iconsStackWidth: NSLayoutConstraint!

    private var sIdx: Int = 0
    private var cIdx: Int = 0
    private var isLast: Bool = false
    private var onSelect: (() -> Void)?
    private let iconW: CGFloat = 24

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
                   isLast: Bool,
                   onSelect: (() -> Void)?) {
        self.cIdx = cIdx
        self.sIdx = sIdx
        self.isLast = isLast
        self.onSelect = onSelect

        lblType.text = model.lblType
        lblName.text = model.name
        lblFiat.text = "19.80 USD"
        lblAmount.text = "0.00000100 BTC"
        imgSS.isHidden = !model.isSS
        imgMS.isHidden = model.isSS
        btnWarn.isHidden = true
        btcImg.isHidden = model.isLiquid
        [bg, effectView, btnWarn].forEach {
            model.isTest ?
            ($0?.backgroundColor = model.isLiquid ? UIColor.gAccountTestLightBlue() : UIColor.gAccountTestGray())
            :
            ($0?.backgroundColor = model.isLiquid ? UIColor.gAccountLightBlue() : UIColor.gAccountOrange())
        }
        btnDisclose.isHidden = onSelect == nil

        let session = WalletManager.current?.activeSessions[model.account.network ?? "mainnet"]
        for v in iconsStack.subviews {
            v.removeFromSuperview()
        }

        var assets = [(key: String, value: Int64)]()
        assets = Transaction.sort(model.account.satoshi ?? [:])

        // need rework here, icons list is not correct
        var icons: [UIImage] = []
        for asset in assets {
            let tag = asset.key
            if let icon = session?.registry?.image(for: tag) {
                if icons.count > 0 {
                    if icon != icons.last {
                        icons.append(icon)
                    }
                } else {
                    icons.append(icon)
                }
            }
        }

        iconsStackWidth.constant = CGFloat(icons.count) * iconW - CGFloat(icons.count - 1) * 10.0
        setImages(icons)
        iconsView.isHidden = false //!showAccounts || !gdkNetwork.liquid
    }

    func setImages(_ images: [UIImage]) {
        for img in images {
            let imageView = UIImageView()
            imageView.image = img
            imageView.heightAnchor.constraint(equalToConstant: iconW).isActive = true
            imageView.widthAnchor.constraint(equalToConstant: iconW).isActive = true
            imageView.borderWidth = 1.0
            imageView.borderColor = UIColor.gBlackBg()
            imageView.cornerRadius = iconW / 2.0
            iconsStack.addArrangedSubview(imageView)
        }
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

    @IBAction func btnSelect(_ sender: Any) {
        onSelect?()
    }
}
