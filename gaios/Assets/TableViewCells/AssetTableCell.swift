import Foundation
import UIKit

class AssetTableCell: UITableViewCell {

    @IBOutlet weak var headerLabel: UILabel!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var domainLabel: UILabel!
    @IBOutlet weak var amountTickerLabel: UILabel!
    @IBOutlet weak var assetIconImageView: UIImageView!

    override func prepareForReuse() {
        headerLabel.text = ""
        headerLabel.isHidden = false
        nameLabel.text = ""
        domainLabel.text = ""
        amountTickerLabel.text = ""
        assetIconImageView.image = nil
    }

    func configure(tag: String, asset: AssetInfo?, satoshi: UInt64, negative: Bool = false, isTransaction: Bool = false) {
        let assetInfo = asset ?? AssetInfo(assetId: tag, name: tag, precision: 0, ticker: "")
        let details = ["satoshi": satoshi, "asset_info": assetInfo.encode()!] as [String: Any]
        let (amount, denom) = Balance.convert(details: details)!.get(tag: tag)
        configure(tag: tag, asset: assetInfo, amount: amount, denom: denom, negative: negative)
    }

    func configure(tag: String, asset: AssetInfo, amount: String, denom: String, negative: Bool, isTransaction: Bool = false) {
        selectionStyle = .none
        headerLabel.isHidden = !isTransaction
        let isBtc = tag == "btc"
        let ticker = isBtc ? denom : asset.ticker ?? ""
        nameLabel.text = isBtc ? "L-BTC" : asset.name
        amountTickerLabel.text = "\(negative ? "-": "")\(amount) \(ticker)"
        domainLabel.text = asset.entity?.domain ?? ""
        domainLabel.isHidden = asset.entity?.domain.isEmpty ?? true
        assetIconImageView.image = AssetIcon.loadAssetIcon(with: tag)
    }
}
