import UIKit

class OverviewAccountCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var bgShadow: UIView!
    @IBOutlet weak var bgHint: UIView!
    @IBOutlet weak var lblAccountTitle: UILabel!
    @IBOutlet weak var lblAccountHint: UILabel!
    @IBOutlet weak var lblBalance: UILabel!
    @IBOutlet weak var actionBtn: UIButton!
    @IBOutlet weak var iconsView: UIView!
    @IBOutlet weak var iconsStack: UIStackView!
    @IBOutlet weak var iconsStackWidth: NSLayoutConstraint!

    var action: VoidToVoid?
    let iconW: CGFloat = 18

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    override func awakeFromNib() {
        super.awakeFromNib()

        bg.layer.cornerRadius = 6.0
        bgShadow.cornerRadius = 6.0
        bgShadow.alpha = 0.5
        bgHint.layer.cornerRadius = 3.0
        iconsView.layer.cornerRadius = 3.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    override func prepareForReuse() {
        lblAccountTitle.text = ""
        lblAccountHint.text = ""
        lblBalance.text = ""
        lblBalance.isHidden = true
        iconsView.isHidden = true
    }

    func configure(account: WalletItem, action: VoidToVoid? = nil, color: UIColor, showAccounts: Bool, isLiquid: Bool) {
        prepareForReuse()
        bg.backgroundColor = color
        if showAccounts { bgShadow.backgroundColor = .clear } else { bgShadow.backgroundColor = color }
        self.lblAccountTitle.text = account.localizedName()

        if let converted = Balance.fromSatoshi(account.btc) {
            let (amount, denom) = converted.toValue()
            lblBalance.text = "\(denom) \(amount)"
            lblBalance.isHidden = !showAccounts
        }

        self.lblAccountHint.text = account.localizedHint()
        self.action = action
        self.actionBtn.isHidden = action == nil

        var assets = [(key: String, value: UInt64)]()
        assets = Transaction.sort(account.satoshi ?? [:])

        for v in iconsStack.subviews {
            v.removeFromSuperview()
        }

        var icons: [UIImage] = []
        for asset in assets {
            let tag = asset.key
            let icon = SessionsManager.current?.registry?.image(for: tag)
            if icon != nil {
                if icons.count > 0 {
                    if icon != icons.last {
                        icons.append(icon!)
                    }
                } else {
                    icons.append(icon!)
                }
            }
        }
        iconsStackWidth.constant = CGFloat(icons.count) * iconW
        setImages(icons)
        iconsView.isHidden = !showAccounts || !isLiquid
    }

    func setImages(_ images: [UIImage]) {
        for img in images {
            let imageView = UIImageView()
            imageView.image = img
            imageView.heightAnchor.constraint(equalToConstant: iconW).isActive = true
            imageView.widthAnchor.constraint(equalToConstant: iconW).isActive = true

            iconsStack.addArrangedSubview(imageView)
        }
    }

    @IBAction func actionBtn(_ sender: Any) {
        self.action?()
    }
}
