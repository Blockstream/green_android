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

    private var isEyeOff = false
    private var model: BalanceCellModel?
    private var onAssets: (() -> Void)?
    private let iconW: CGFloat = 18
    private var cachedBalance = [(String, Int64)]()

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        lblBalanceTitle.text = "Total Balance"
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    func configure(model: BalanceCellModel,
                   cachedBalance: [(String, Int64)],
                   onAssets: (() -> Void)?) {
        self.model = model
        setContent()
        let uLineAttr = [NSAttributedString.Key.underlineStyle: NSUnderlineStyle.thick.rawValue]
        let str = NSAttributedString(string: "\(model.numAssets) assets in total", attributes: uLineAttr)
        btnAssets.setAttributedTitle(str, for: .normal)
        self.onAssets = onAssets

        let icons = cachedBalance.compactMap { WalletManager.current?.registry.image(for: $0.0) }

        for v in iconsStack.subviews {
            v.removeFromSuperview()
        }

        iconsStackWidth.constant = CGFloat(icons.count) * iconW - CGFloat(icons.count - 1) * 5.0
        setImages(icons)
        iconsView.isHidden = false //!showAccounts || !gdkNetwork.liquid
    }

    func setContent() {
        if isEyeOff {
            lblBalanceValue.text = "---"
            lblBalanceFiat.text = "---"
            btnEye.setImage(UIImage(named: "ic_hide"), for: .normal)
        } else {
            lblBalanceValue.text = model?.value ?? ""
            lblBalanceFiat.text = model?.valueFiat ?? ""
            btnEye.setImage(UIImage(named: "ic_eye_flat"), for: .normal)
        }
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

    @IBAction func btnEye(_ sender: Any) {
        isEyeOff = !isEyeOff
        setContent()
    }

    @IBAction func btnAssets(_ sender: Any) {
        onAssets?()
    }
}
