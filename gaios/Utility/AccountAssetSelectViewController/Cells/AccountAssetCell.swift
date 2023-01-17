import UIKit

class AccountAssetCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!

    @IBOutlet weak var imgView: UIImageView!
    @IBOutlet weak var imgMS: UIImageView!
    @IBOutlet weak var imgSS: UIImageView!

    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblAmount: UILabel!

    @IBOutlet weak var lblAccount: UILabel!
    @IBOutlet weak var lblFiat: UILabel!
    @IBOutlet weak var lblType: UILabel!

    class var identifier: String { return String(describing: self) }

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.cornerRadius = 5.0
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        lblAsset.text = ""
        lblAccount.text = ""
        lblType.text = ""
        imgView.image = UIImage()
        lblAmount.text = ""
        lblFiat.text = ""
    }

    func configure(model: AccountAssetCellModel) {
        let name = model.asset.name ?? model.asset.assetId
        self.lblAsset.text = name
        self.lblAccount.text = model.account.localizedName()
        self.lblType.text = model.account.type.typeStringId.localized.uppercased()

        imgView.image = WalletManager.current?.registry.image(for: model.asset.assetId)
        let isSS = model.account.gdkNetwork.electrum ? true : false
        imgSS.isHidden = !isSS
        imgMS.isHidden = isSS
        // isLiquid = account.gdkNetwork.liquid
        // isTest = !account.gdkNetwork.mainnet

        let satoshi = model.balance.first?.value ?? 0
        if let balance = Balance.fromSatoshi(satoshi, asset: model.asset)?.toValue() {
            lblAmount.text = "\(balance.0) \(balance.1)"
        }
        if let balance = Balance.fromSatoshi(satoshi, asset: model.asset)?.toFiat() {
            lblFiat.text = "\(balance.0) \(balance.1)"
        }
    }
}
