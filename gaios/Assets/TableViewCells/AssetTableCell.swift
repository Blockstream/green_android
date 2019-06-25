import Foundation
import UIKit

class AssetTableCell: UITableViewCell {
    @IBOutlet weak var title: UILabel!
    @IBOutlet weak var value: UILabel!

    func setup(tag: String, asset: AssetInfo?, satoshi: UInt64, negative: Bool = false) {
        let assetInfo = asset ?? AssetInfo(assetId: tag, name: tag, precision: 0, ticker: "")
        let details = ["satoshi": satoshi, "asset_info": assetInfo.encode()!] as [String: Any]
        let (amount, ticker) = Balance.convert(details: details).get(tag: tag)
        value.text = "\(negative ? "-": "")\(amount) \(ticker)"
        title.text = "btc" == tag ? "L-BTC" : assetInfo.name
    }
}
