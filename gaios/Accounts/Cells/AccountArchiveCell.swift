import UIKit

class AccountArchiveCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
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

    func configure(account: WalletItem, action: VoidToVoid? = nil, color: UIColor, isLiquid: Bool) {

        bg.backgroundColor = color
        self.lblAccountTitle.text = account.localizedName()

        if let converted = Balance.convert(details: ["satoshi": account.btc]) {
            let (amount, denom) = converted.get(tag: btc)
            lblBalance.text = "\(denom) \(amount!)"
            lblBalance.isHidden = false
        }

        let accountType: AccountType? = AccountType(rawValue: account.type)
        self.lblAccountHint.text = accountType?.name ?? ""
        self.action = action
        self.actionBtn.isHidden = false

        var assets = [(key: String, value: UInt64)]()
        assets = Transaction.sort(account.satoshi ?? [:])
        assets = sortAssets(assets: assets)

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
        iconsView.isHidden = !isLiquid
    }

    func sortAssets(assets: [(key: String, value: UInt64)]) -> [(key: String, value: UInt64)] {
        var tAssets: [SortingAsset] = []
        assets.forEach { asset in
            let tAss = SortingAsset(tag: asset.key, info: SessionsManager.current!.registry!.infos[asset.key], hasImage: SessionsManager.current!.registry!.hasImage(for: asset.key), value: asset.value)
            tAssets.append(tAss)
        }
        var oAssets = [(key: String, value: UInt64)]()
        tAssets.sort(by: {!$0.hasImage && !$1.hasImage ? $0.info?.ticker != nil && !($1.info?.ticker != nil) : $0.hasImage && !$1.hasImage})

        tAssets.forEach { asset in
            oAssets.append((key:asset.tag, value: asset.value))
        }
        return oAssets
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
