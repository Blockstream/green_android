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
    @IBOutlet weak var blurArea: UIView!

    private var model: BalanceCellModel?
    private var onAssets: (() -> Void)?
    private var onHide: ((Bool) -> Void)?
    private let iconW: CGFloat = 18
    private var hideBalance = false

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblBalanceTitle.text = "Total Balance"
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    lazy var blurredView: UIView = {
        let containerView = UIView()
        let blurEffect = UIBlurEffect(style: .dark)
        let customBlurEffectView = CustomVisualEffectView(effect: blurEffect, intensity: 0.4)
        customBlurEffectView.frame = blurArea.bounds

        let dimmedView = UIView()
        dimmedView.backgroundColor = .black.withAlphaComponent(0.15)
        dimmedView.frame = blurArea.bounds
        containerView.addSubview(customBlurEffectView)
        containerView.addSubview(dimmedView)
        return containerView
    }()

    func configure(model: BalanceCellModel,
                   hideBalance: Bool,
                   onHide: ((Bool) -> Void)?,
                   onAssets: (() -> Void)?) {
        self.hideBalance = hideBalance
        self.model = model
        lblBalanceValue.text = model.value
        lblBalanceFiat.text = model.valueFiat

        let uLineAttr = [NSAttributedString.Key.underlineStyle: NSUnderlineStyle.thick.rawValue]
        let str = NSAttributedString(string: "\(model.numAssets) assets in total", attributes: uLineAttr)
        btnAssets.setAttributedTitle(str, for: .normal)
        self.onAssets = onAssets
        self.onHide = onHide
        let sorted = model.cachedBalance.sorted()
        var icons: [UIImage] = []
        for asset in sorted {
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

        iconsStackWidth.constant = CGFloat(icons.count) * iconW - CGFloat(icons.count - 1) * 5.0
        setImages(icons)
        iconsView.isHidden = false //!showAccounts || !gdkNetwork.liquid
        refreshBlur()
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

    func refreshBlur() {
        if hideBalance {
            blurArea.subviews.forEach {$0.removeFromSuperview()}
            blurArea.addSubview(blurredView)
            btnEye.setImage(UIImage(named: "ic_hide"), for: .normal)
        } else {
            blurArea.subviews.forEach {$0.removeFromSuperview()}
            btnEye.setImage(UIImage(named: "ic_eye_flat"), for: .normal)
        }
    }

    @IBAction func btnEye(_ sender: Any) {
        hideBalance = !hideBalance
        onHide?(hideBalance)
        refreshBlur()
    }

    @IBAction func btnAssets(_ sender: Any) {
        onAssets?()
    }
}
