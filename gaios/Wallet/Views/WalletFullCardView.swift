import Foundation
import UIKit

class WalletFullCardView: UIView {

    @IBOutlet weak var shadowView: UIView!
    @IBOutlet weak var walletName: UILabel!
    @IBOutlet weak var legacyLbl: UILabel!
    @IBOutlet weak var balance: UILabel!
    @IBOutlet weak var balanceFiat: UILabel!
    @IBOutlet weak var sendView: UIView!
    @IBOutlet weak var sweepView: UIView!
    @IBOutlet weak var receiveView: UIView!
    @IBOutlet weak var actionsView: UIStackView!
    @IBOutlet weak var sendLabel: UILabel!
    @IBOutlet weak var sweepLabel: UILabel!
    @IBOutlet weak var receiveLabel: UILabel!
    @IBOutlet weak var stackButton: UIButton!
    @IBOutlet weak var sendImage: UIImageView!
    @IBOutlet weak var sweepImage: UIImageView!
    @IBOutlet weak var receiveImage: UIImageView!
    @IBOutlet weak var backgroundView: UIView!
    @IBOutlet weak var unit: UILabel!
    @IBOutlet weak var assetsLabel: UILabel!
    @IBOutlet weak var assetsView: UIView!
    @IBOutlet weak var assetsHeight: NSLayoutConstraint!

    var account = AccountsManager.shared.current
    var isLiquid: Bool { account?.gdkNetwork?.liquid ?? false}
    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""

    }

    override func awakeFromNib() {
        super.awakeFromNib()
        sendLabel.text = NSLocalizedString("id_send", comment: "").capitalized
        sweepLabel.text = NSLocalizedString("id_sweep", comment: "").capitalized
        receiveLabel.text = NSLocalizedString("id_receive", comment: "").capitalized
        sendImage.tintColor = isLiquid ? .white : UIColor.customMatrixGreen()
        sweepImage.tintColor = isLiquid ? .white : UIColor.customMatrixGreen()
        receiveImage.tintColor = isLiquid ? .white : UIColor.customMatrixGreen()
        assetsView.layer.cornerRadius = 6.0
        sendView.layer.cornerRadius = 6.0
        sweepView.layer.cornerRadius = 6.0
        receiveView.layer.cornerRadius = 6.0
        actionsView.layer.cornerRadius = 6.0
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        let gradient = CAGradientLayer()
        gradient.colors = isLiquid ? [UIColor.cardBlueDark().cgColor, UIColor.cardBlueMedium().cgColor, UIColor.cardBlueLight().cgColor] : [UIColor.cardDark().cgColor, UIColor.cardMedium().cgColor, UIColor.cardLight().cgColor]
        gradient.locations = [0.0, 0.5, 1.0]
        gradient.startPoint = CGPoint(x: 0.0, y: 1.0)
        gradient.endPoint = CGPoint(x: 1.0, y: 1.0)
        gradient.frame = backgroundView.bounds
        backgroundView.layer.insertSublayer(gradient, at: 0)
        backgroundView.layer.masksToBounds = true
        backgroundView.layer.cornerRadius = 6.0
        sendView.layer.maskedCorners = [.layerMinXMaxYCorner]
        receiveView.layer.maskedCorners = [.layerMaxXMaxYCorner]

        if isLiquid {
            sendView.backgroundColor = UIColor.blueLight()
            receiveView.backgroundColor = UIColor.blueLight()
        } else {
            assetsHeight.constant = 0
            assetsView.layoutIfNeeded()
        }
    }

    func setup(with wallet: WalletItem) {
        if let converted = Balance.convert(details: ["satoshi": wallet.btc]) {
            let (amount, denom) = converted.get(tag: btc)
            let (fiat, currency) = converted.get(tag: "fiat")
            balance.text = amount
            unit.text = denom
            balanceFiat.text = "≈ \(fiat ?? "N.A.") \(currency)"
        }
        walletName.text = wallet.localizedName()
        assetsLabel.text = String(format: NSLocalizedString(wallet.satoshi.count == 1 ? "id_d_asset_in_this_account" : "id_d_assets_in_this_account", comment: ""), wallet.satoshi.count)
        if getGAService().getTwoFactorReset()?.isResetActive ?? false {
            actionsView.isHidden = true
        } else if getGAService().isWatchOnly {
            sendView.isHidden = true
            sweepView.isHidden = false
        }

        let accountType: AccountType? = AccountType(rawValue: wallet.type)
        legacyLbl.text = accountType?.name ?? ""
    }
}
