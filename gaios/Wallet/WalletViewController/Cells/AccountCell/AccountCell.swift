import UIKit

class AccountCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var detailView: UIView!
    @IBOutlet weak var effectView: UIView!
    @IBOutlet weak var innerEffectView: UIView!
    @IBOutlet weak var btcImg: UIImageView!
    @IBOutlet weak var btnSelect: UIButton!
    @IBOutlet weak var btnCopy: UIButton!
    @IBOutlet weak var btnShield: UIButton!
    @IBOutlet weak var imgMS: UIImageView!
    @IBOutlet weak var imgSS: UIImageView!
    @IBOutlet weak var lblType: UILabel!
    @IBOutlet weak var lblName: UILabel!
    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var iconsView: UIView!
    @IBOutlet weak var iconsStack: UIStackView!
    @IBOutlet weak var iconsStackWidth: NSLayoutConstraint!

    @IBOutlet weak var icC1: UIView!
    @IBOutlet weak var icC2: UIView!
    @IBOutlet weak var icC3: UIView!
    @IBOutlet weak var icC4: UIView!
    @IBOutlet weak var icC5: UIView!
    @IBOutlet weak var icC6: UIView!
    @IBOutlet weak var icC7: UIView!
    @IBOutlet weak var icV1: UIImageView!
    @IBOutlet weak var icV2: UIImageView!
    @IBOutlet weak var icV3: UIImageView!
    @IBOutlet weak var icV4: UIImageView!
    @IBOutlet weak var icV5: UIImageView!
    @IBOutlet weak var icV6: UIImageView!
    @IBOutlet weak var icV7: UIImageView!

    @IBOutlet weak var trailing2_1: NSLayoutConstraint!
    @IBOutlet weak var trailing3_2: NSLayoutConstraint!
    @IBOutlet weak var trailing4_3: NSLayoutConstraint!
    @IBOutlet weak var titlesTrailing: NSLayoutConstraint!

    private var sIdx: Int = 0
    private var cIdx: Int = 0
    private var hideBalance: Bool = false
    private var isLast: Bool = false
    private var onSelect: (() -> Void)?
    private var onCopy: (() -> Void)?
    private var onShield: ((Int) -> Void)?
    private let iconW: CGFloat = 24
    private var cColor: UIColor = .clear

    var views: [UIView] {
        return [icC1, icC2, icC3, icC4, icC5, icC6, icC7]
    }

    var imgs: [UIView] {
        return [icV1, icV2, icV3, icV4, icV5, icV6, icV7]
    }

    static var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.cornerRadius = 5.0
        innerEffectView.layer.cornerRadius = 5.0
        innerEffectView.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        btnShield.borderWidth = 1.0
        btnShield.borderColor = .black
        btnShield.cornerRadius = 16.0
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

    func stringForAttachment() -> NSAttributedString {
        if #available(iOS 13.0, *) {
            let attachment = NSTextAttachment()
            let image = UIImage(systemName: "asterisk")?.withTintColor(.white)
            attachment.image = image
            let fullString = NSMutableAttributedString(string: "")
            fullString.append(NSAttributedString(attachment: attachment))
            return fullString
        } else {
            return NSAttributedString()
        }
    }

    func configure(model: AccountCellModel,
                   cIdx: Int,
                   sIdx: Int,
                   hideBalance: Bool,
                   isLast: Bool,
                   onSelect: (() -> Void)?,
                   onCopy: (() -> Void)?,
                   onShield: ((Int) -> Void)?) {
        self.cIdx = cIdx
        self.sIdx = sIdx
        self.hideBalance = hideBalance
        self.isLast = isLast
        self.onSelect = onSelect
        self.onCopy = onCopy
        self.onShield = onShield

        lblType.text = model.lblType
        lblName.text = NSLocalizedString(model.name, comment: "")

        if hideBalance {
            lblFiat.attributedText = Common.obfuscate(color: .white, size: 12, length: 5)
            lblAmount.attributedText = Common.obfuscate(color: .white, size: 16, length: 5)
        } else {
            lblFiat.text = model.fiatStr
            lblAmount.text = model.balanceStr
        }
        imgSS.isHidden = !model.isSS
        imgMS.isHidden = model.isSS
        let session = model.account.session
        let watchOnly = AccountsManager.shared.current?.isWatchonly ?? false
        let enabled2FA = session?.twoFactorConfig?.anyEnabled ?? false
        btnShield.isHidden = onSelect == nil || model.isSS || enabled2FA || watchOnly
        btcImg.isHidden = model.isLiquid
        model.isTest ? (cColor = model.isLiquid ? UIColor.gAccountTestLightBlue() : UIColor.gAccountTestGray()) :
        (cColor = model.isLiquid ? UIColor.gAccountLightBlue() : UIColor.gAccountOrange())
        [bg, effectView, btnShield].forEach {
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

//        iconsStackWidth.constant = CGFloat(icons.count) * iconW - CGFloat(icons.count - 1) * 10.0
//        setImages(icons)
//        iconsView.isHidden = false //!showAccounts || !gdkNetwork.liquid

        views.forEach {
            $0.borderWidth = 1.0
            $0.borderColor = UIColor.white
            $0.cornerRadius = $0.frame.size.width / 2.0
            $0.backgroundColor = cColor.darker(by: 10)
        }
        imgs.forEach {
            $0.cornerRadius = $0.frame.size.width / 2.0
            $0.clipsToBounds = true
        }
        views.forEach { $0.isHidden = true }
        let padding: CGFloat = icons.count > 4 ? -20 : -10
        [trailing2_1, trailing3_2, trailing4_3].forEach {
            $0?.constant = padding
        }

        titlesTrailing.constant = 0.0
        let width = icC1.frame.width

        switch icons.count {
        case 0:
            break
        case 1:
            icC1.isHidden = false
            icV1.image = icons[0]
            titlesTrailing.constant = -width * 1.0 - 10.0
        case 2:
            icC1.isHidden = false
            icV1.image = icons[1]
            icC2.isHidden = false
            icV2.image = icons[0]
            titlesTrailing.constant = -width * 2.0 - 10.0 + 10.0
        case 3:
            icC1.isHidden = false
            icV1.image = icons[2]
            icC2.isHidden = false
            icV2.image = icons[1]
            icC3.isHidden = false
            icV3.image = icons[0]
            titlesTrailing.constant = -width * 3.0 - 10.0 + 20.0
        case 4:
            icC1.isHidden = false
            icV1.image = icons[3]
            icC2.isHidden = false
            icV2.image = icons[2]
            icC3.isHidden = false
            icV3.image = icons[1]
            icC4.isHidden = false
            icV4.image = icons[0]
            titlesTrailing.constant = -width * 4.0 - 10.0 + 30.0
        default:
            icV1.image = UIImage()
            icV2.image = UIImage()
            icV3.image = UIImage()
            icC4.isHidden = false
            icV4.image = icons[3]
            icC5.isHidden = false
            icV5.image = icons[2]
            icC6.isHidden = false
            icV6.image = icons[1]
            icC7.isHidden = false
            icV7.image = icons[0]
            titlesTrailing.constant = -width * 4.0 - 10.0 + 30.0 - 30.0
        }

        if icons.count > 4 {
            views.forEach { $0.isHidden = false }
        }
    }

//    func setImages(_ images: [UIImage]) {
//        for img in images {
//            let imageView = UIImageView()
//            imageView.image = img
//            imageView.heightAnchor.constraint(equalToConstant: iconW).isActive = true
//            imageView.widthAnchor.constraint(equalToConstant: iconW).isActive = true
//            imageView.backgroundColor = cColor
//            imageView.borderWidth = 1.0
//            imageView.borderColor = UIColor.gBlackBg()
//            imageView.cornerRadius = iconW / 2.0
//            imageView.clipsToBounds = true
//            iconsStack.addArrangedSubview(imageView)
//        }
//    }

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

    @IBAction func btnShield(_ sender: Any) {
        onShield?(cIdx)
    }
}
