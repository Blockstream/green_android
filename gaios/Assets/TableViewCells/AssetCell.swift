import Foundation
import UIKit

class AssetCell: UITableViewCell {

    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var domainLabel: UILabel!
    @IBOutlet weak var amountTickerLabel: UILabel!
    @IBOutlet weak var assetIconImageView: UIImageView!
    @IBOutlet weak var bgView: UIView!

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    override func prepareForReuse() {
        nameLabel.text = ""
        domainLabel.text = ""
        amountTickerLabel.text = ""
        assetIconImageView.image = nil
    }

    func configure(tag: String, info: AssetInfo?, icon: UIImage?, satoshi: UInt64, negative: Bool = false, isTransaction: Bool = false, sendAll: Bool = false) {
        let isBtc = tag == btc
        let details = ["satoshi": satoshi, "asset_info": info!.encode()!] as [String: Any]
        if let balance = Balance.convert(details: details) {
            let (amount, denom) = balance.get(tag: tag)
            let ticker = isBtc ? denom : info?.ticker ?? ""
            let amountTxt = sendAll ? NSLocalizedString("id_all", comment: "") : amount
            amountTickerLabel.text = "\(negative ? "-": "")\(amountTxt ?? "") \(ticker)"
        }
        selectionStyle = .none
        nameLabel.text = info?.name ?? tag
        domainLabel.text = info?.entity?.domain ?? ""
        domainLabel.isHidden = info?.entity?.domain.isEmpty ?? true
        assetIconImageView.image = icon
        self.backgroundColor = UIColor.customTitaniumDark()
        bgView.layer.cornerRadius = 6.0
    }
}
