import Foundation
import UIKit

class AssetTableCell: UITableViewCell {

    @IBOutlet weak var headerLabel: UILabel!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var domainLabel: UILabel!
    @IBOutlet weak var amountTickerLabel: UILabel!
    @IBOutlet weak var assetIconImageView: UIImageView!
    @IBOutlet weak var bgView: UIView!
    
    override func prepareForReuse() {
        headerLabel.text = ""
        headerLabel.isHidden = false
        nameLabel.text = ""
        domainLabel.text = ""
        amountTickerLabel.text = ""
        assetIconImageView.image = nil
    }

    func configure(tag: String, info: AssetInfo?, icon: UIImage?, satoshi: UInt64, negative: Bool = false, isTransaction: Bool = false, sendAll: Bool = false) {
        let isBtc = tag == "btc"
        let asset = info ?? AssetInfo(assetId: tag, name: tag, precision: 0, ticker: "")
        let details = ["satoshi": satoshi, "asset_info": asset.encode()!] as [String: Any]
        if let balance = Balance.convert(details: details) {
            let (amount, denom) = balance.get(tag: tag)
            let ticker = isBtc ? denom : asset.ticker ?? ""
            let amountTxt = sendAll ? NSLocalizedString("id_all", comment: "") : amount
            amountTickerLabel.text = "\(negative ? "-": "")\(amountTxt ?? "") \(ticker)"
        }
        selectionStyle = .none
        headerLabel.isHidden = !isTransaction
        nameLabel.text = isBtc ? "Liquid Bitcoin" : info?.name ?? tag
        domainLabel.text = info?.entity?.domain ?? ""
        domainLabel.isHidden = info?.entity?.domain.isEmpty ?? true
        assetIconImageView.image = icon
        self.backgroundColor = UIColor.customTitaniumDark()
        bgView.layer.cornerRadius = 6.0
    }
}
