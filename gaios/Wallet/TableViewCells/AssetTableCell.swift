import Foundation
import UIKit

class AssetTableCell: UITableViewCell {
    @IBOutlet weak var title: UILabel!
    @IBOutlet weak var value: UILabel!

    func setup(tag: String, asset: AssetInfo?, satoshi: UInt64 ) {
        let isBtc = tag == "btc"
        let ticker = isBtc ? "L-BTC" : asset?.ticker ?? ""
        let assetInfo = asset ?? AssetInfo(assetId: tag, name: "", precision: 0, ticker: "")
        let details = ["satoshi": satoshi, "asset_info": assetInfo.encode()!] as [String: Any]
        let res = try? getSession().convertAmount(input: details)
        let amount = res![tag] as? String ?? ""
        value.text = "\(amount) \(ticker)"
        title.text = isBtc ? "L-BTC" : asset?.name ?? tag
    }
}
