import UIKit

class BalanceCell: UITableViewCell {

    @IBOutlet weak var lblBalanceTitle: UILabel!
    @IBOutlet weak var lblBalanceValue: UILabel!
    @IBOutlet weak var lblBalanceFiat: UILabel!
    @IBOutlet weak var btnAssets: UIButton!
    @IBOutlet weak var iconsView: UIView!
    @IBOutlet weak var iconsStack: UIStackView!
    @IBOutlet weak var iconsStackWidth: NSLayoutConstraint!
    @IBOutlet weak var btnEye: UIButton!
    @IBOutlet weak var assetsBox: UIView!

    private var model: BalanceCellModel?
    private var onAssets: (() -> Void)?
    private var onConvert: (() -> Void)?
    private var onHide: ((Bool) -> Void)?
    private let iconW: CGFloat = 20.0
    private var hideBalance = false

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblBalanceTitle.text = "id_total_balance".localized
        lblBalanceTitle.font = .systemFont(ofSize: 18.0, weight: .bold)
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: BalanceCellModel,
                   hideBalance: Bool,
                   onHide: ((Bool) -> Void)?,
                   onAssets: (() -> Void)?,
                   onConvert:(() -> Void)?) {
        self.hideBalance = hideBalance
        self.model = model
        lblBalanceValue.text = model.value
        lblBalanceFiat.text = model.valueChange

        let assets = model.cachedBalance.sorted().nonZero()
        assetsBox.isHidden = assets.count < 2

        let uLineAttr = [NSAttributedString.Key.underlineStyle: NSUnderlineStyle.thick.rawValue]
        let str = NSAttributedString(string: String(format: "id_d_assets_in_total".localized, assets.count), attributes: uLineAttr)
        btnAssets.setAttributedTitle(str, for: .normal)
        self.onAssets = onAssets
        self.onHide = onHide
        self.onConvert = onConvert
        var icons: [UIImage] = []
        for asset in assets {
            let tag = asset.0
            if let icon = WalletManager.current?.registry.image(for: tag) {
                if icons.count > 0 {
                    if icon != icons.last {
                        icons.append(icon)
                    }
                } else {
                    icons.append(icon)
                }
            }
        }

        for v in iconsStack.subviews {
            v.removeFromSuperview()
        }

        icons = Array(icons.prefix(10))
        iconsStackWidth.constant = CGFloat(icons.count) * iconW - CGFloat(icons.count - 1) * 5.0
        setImages(icons)
        iconsView.isHidden = false //!showAccounts || !gdkNetwork.liquid
        refreshVisibility()
    }

    func setImages(_ images: [UIImage]) {
        for img in images {
            let imageView = UIImageView()
            imageView.image = img
//            imageView.heightAnchor.constraint(equalToConstant: iconW).isActive = true
//            imageView.widthAnchor.constraint(equalToConstant: iconW).isActive = true

            iconsStack.addArrangedSubview(imageView)
        }
    }

    func refreshVisibility() {
        if hideBalance {
            lblBalanceValue.attributedText = Common.obfuscate(color: .white, size: 24, length: 5)
            lblBalanceFiat.attributedText = Common.obfuscate(color: .gray, size: 12, length: 5)
            btnEye.setImage(UIImage(named: "ic_eye_closed"), for: .normal)
        } else {
            lblBalanceValue.text = self.model?.value
            lblBalanceFiat.text = self.model?.valueChange
            btnEye.setImage(UIImage(named: "ic_eye_flat"), for: .normal)
        }
    }

    @IBAction func onBalanceTap(_ sender: Any) {
        AnalyticsManager.shared.convertBalance(account: AccountsManager.shared.current)
        onConvert?()
    }

    @IBAction func btnEye(_ sender: Any) {
        hideBalance = !hideBalance
        onHide?(hideBalance)
        refreshVisibility()
    }

    @IBAction func btnAssets(_ sender: Any) {
        onAssets?()
    }
}
