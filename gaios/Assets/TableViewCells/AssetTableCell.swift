import Foundation
import UIKit

class AssetTableCell: UITableViewCell {

    @IBOutlet weak var headerLabel: UILabel!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var domainLabel: UILabel!
    @IBOutlet weak var amountTickerLabel: UILabel!

    private func configure() {
        selectionStyle = .none
    }

    func configure(for transaction: Transaction) {
        configure()
        let asset = transaction.assets[transaction.defaultAsset]
        headerLabel.text = NSLocalizedString("id_asset", comment: "")
        domainLabel.isHidden = true
        amountTickerLabel.isHidden = true
        let isBtc = transaction.defaultAsset == "btc"
        nameLabel.text = isBtc ? "L-BTC" : asset?.ticker?.isEmpty ?? true ? transaction.defaultAsset : asset?.ticker
    }

    func configure(tag: String, asset: AssetInfo?, satoshi: UInt64, negative: Bool = false) {
        let assetInfo = asset ?? AssetInfo(assetId: tag, name: tag, precision: 0, ticker: "")
        let details = ["satoshi": satoshi, "asset_info": assetInfo.encode()!] as [String: Any]
        let (amount, _) = Balance.convert(details: details)!.get(tag: tag)
        configure(tag: tag, asset: assetInfo, amount: amount, negative: negative)
    }

    func configure(tag: String, asset: AssetInfo, amount: String, negative: Bool) {
        configure()
        headerLabel.isHidden = true
        let isBtc = tag == "btc"
        let ticker = isBtc ? "L-BTC" : asset.ticker ?? ""
        nameLabel.text = isBtc ? "L-BTC" : asset.name
        amountTickerLabel.text = "\(negative ? "-": "")\(amount) \(ticker)"
        domainLabel.text = asset.entity?.domain ?? ""
        domainLabel.isHidden = asset.entity?.domain.isEmpty ?? true
    }
}
