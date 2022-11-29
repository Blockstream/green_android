import UIKit

class AccountCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var detailView: UIView!
    @IBOutlet weak var effectView: UIView!
    @IBOutlet weak var innerEffectView: UIView!
    @IBOutlet weak var btcImg: UIImageView!
    @IBOutlet weak var btnSelect: UIButton!
    @IBOutlet weak var btnCopy: UIButton!
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
    private var onCopy: (() -> Void)?
    private let iconW: CGFloat = 24
    private var cColor: UIColor = .clear

    static var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.cornerRadius = 5.0
        innerEffectView.layer.cornerRadius = 5.0
        innerEffectView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        btnWarn.borderWidth = 1.0
        btnWarn.borderColor = .black
        btnWarn.cornerRadius = 16.0
        [btnSelect, btnCopy].forEach {
            $0?.borderWidth = 1.0
            $0?.borderColor = .white
            $0?.cornerRadius = 3.0
        }
        btnCopy.setTitle("Copy ID", for: .normal)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        select(selected)
    }

    func configure(model: AccountCellModel,
                   cIdx: Int,
                   sIdx: Int,
                   isLast: Bool,
                   onSelect: (() -> Void)?,
                   onCopy: (() -> Void)?) {
        self.cIdx = cIdx
        self.sIdx = sIdx
        self.isLast = isLast
        self.onSelect = onSelect
        self.onCopy = onCopy

        lblType.text = model.lblType
        lblName.text = NSLocalizedString(model.name, comment: "")
        lblFiat.text = model.fiatStr
        lblAmount.text = model.balanceStr
        imgSS.isHidden = !model.isSS
        imgMS.isHidden = model.isSS
        let session = model.account.session
        let enabled2FA = session?.twoFactorConfig?.anyEnabled ?? false
        btnWarn.isHidden = model.isSS || enabled2FA
        btcImg.isHidden = model.isLiquid
        model.isTest ? (cColor = model.isLiquid ? UIColor.gAccountTestLightBlue() : UIColor.gAccountTestGray()) :
        (cColor = model.isLiquid ? UIColor.gAccountLightBlue() : UIColor.gAccountOrange())
        [bg, effectView, btnWarn].forEach {
            $0?.backgroundColor = cColor
        }
        btnSelect.isHidden = onSelect == nil
        btnCopy.isHidden = onCopy == nil || model.account.type != .amp // only for amp

        for v in iconsStack.subviews {
            v.removeFromSuperview()
        }

        let assets = AssetAmountList(model.account.satoshi ?? [:]).sorted()
        let registry = WalletManager.current?.registry
        var icons = [UIImage]()
        assets.compactMap { registry?.image(for: $0.0) }
            .forEach { if !icons.contains($0) { icons += [$0] } }

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
            imageView.backgroundColor = cColor
            imageView.borderWidth = 1.0
            imageView.borderColor = UIColor.gBlackBg()
            imageView.cornerRadius = iconW / 2.0
            imageView.clipsToBounds = true
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

    @IBAction func btnCopy(_ sender: Any) {
        onCopy?()
    }
}
