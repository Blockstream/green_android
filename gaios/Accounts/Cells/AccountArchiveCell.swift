import UIKit
import gdk

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
    private var cColor: UIColor = .clear
    
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
    
    func configure(account: WalletItem, action: VoidToVoid? = nil) {
        
        let isLiquid = account.gdkNetwork.liquid
        let isTest = !account.gdkNetwork.mainnet
        
        isTest ? (cColor = isLiquid ? UIColor.gAccountTestLightBlue() : UIColor.gAccountTestGray()) :
        (cColor = isLiquid ? UIColor.gAccountLightBlue() : UIColor.gAccountOrange())
        
        bg.backgroundColor = cColor
        self.lblAccountTitle.text = account.localizedName
        
        let assetId = account.gdkNetwork.getFeeAsset()
        if let converted = Balance.fromSatoshi(account.satoshi?[assetId] ?? 0, assetId: assetId) {
            let (amount, denom) = converted.toDenom()
            lblBalance.text = "\(denom) \(amount)"
            lblBalance.isHidden = false
        }
        self.lblAccountHint.text = account.type.longText.uppercased()
        self.action = action
        self.actionBtn.isHidden = false
        for v in iconsStack.subviews {
            v.removeFromSuperview()
        }
        let assets = AssetAmountList(account.satoshi ?? [:])
        iconsView.isHidden = !isLiquid
        var icons: [UIImage] = []
        for asset in assets.ids {
            let icon = assets.image(for: asset)
            if icons.count > 0 {
                if icon != icons.last {
                    icons.append(icon)
                }
            } else {
                icons.append(icon)
            }
        }
        setImages(icons)
        iconsStackWidth.constant = CGFloat(icons.count) * iconW
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
