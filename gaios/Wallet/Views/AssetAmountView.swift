import Foundation
import UIKit

class AssetAmountView: UIView {

    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblDenom: UILabel!
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var spvVerifyIcon: UIImageView!

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
    }

    func clean() {
        lblAmount.text = ""
        lblDenom.text = ""
        icon.image = UIImage()
        spvVerifyIcon.image = UIImage()
    }

    func setup(tx: Transaction, satoshi: Int64, assetId: String) {
        let registry = WalletManager.current?.registry
        let asset = registry?.info(for: assetId)
        if let balance = Balance.fromSatoshi(satoshi, asset: asset) {
            let (value, denom) = balance.toValue()
            lblAmount.text = String(format: "%@%@", satoshi > 0 ? "+" : "", value)
            lblDenom.text = "\(denom)"
        }
        icon.image = registry?.image(for: assetId)
        lblAmount.textColor = satoshi > 0 ? .customMatrixGreen() : .white
        setSpvVerifyIcon(tx: tx)
    }

    func setSpvVerifyIcon(tx: Transaction) {
        switch tx.spvVerified {
        case "disabled", "verified", nil:
            spvVerifyIcon.isHidden = true
        case "in_progress":
            spvVerifyIcon.isHidden = false
            spvVerifyIcon.image = UIImage(named: "ic_spv_progress")
            spvVerifyIcon.tintColor = .white
        case "not_verified":
            spvVerifyIcon.isHidden = false
            spvVerifyIcon.image = UIImage(named: "ic_spv_warning")
            spvVerifyIcon.tintColor = .red
        default:
            spvVerifyIcon.isHidden = false
            spvVerifyIcon.image = UIImage(named: "ic_spv_warning")
            spvVerifyIcon.tintColor = .yellow
        }
    }
}
