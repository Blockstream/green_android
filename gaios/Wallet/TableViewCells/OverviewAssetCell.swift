//
//  OverviewAssetCell.swift
//  gaios
//
//  Created by Mauro Olivo on 09/09/21.
//  Copyright © 2021 Blockstream Corporation. All rights reserved.
//

import UIKit

class OverviewAssetCell: UITableViewCell {

    @IBOutlet weak var bg: UIView!
    @IBOutlet weak var lblDenom: UILabel!
    @IBOutlet weak var lblAsset: UILabel!
    @IBOutlet weak var lblAmount: UILabel!
    @IBOutlet weak var lblAmount2: UILabel!
    @IBOutlet weak var icon: UIImageView!

    override func awakeFromNib() {
        super.awakeFromNib()
        bg.layer.cornerRadius = 5.0
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    override func prepareForReuse() {
        lblDenom.text = ""
        lblAsset.text = ""
        lblAmount.text = ""
        lblAmount2.text = ""
        icon.image = nil
    }

    func configure(tag: String, info: AssetInfo?, icon: UIImage?, satoshi: UInt64, negative: Bool = false, isTransaction: Bool = false, sendAll: Bool = false) {
        let isBtc = tag == btc
        let asset = info ?? AssetInfo(assetId: tag, name: tag, precision: 0, ticker: "")
        let details = ["satoshi": satoshi, "asset_info": asset.encode()!] as [String: Any]
        if let balance = Balance.convert(details: details) {
            let (amount, denom) = balance.get(tag: tag)
            let ticker = isBtc ? denom : asset.ticker ?? ""
            let amountTxt = sendAll ? NSLocalizedString("id_all", comment: "") : amount
            lblAmount.text = "\(negative ? "-": "")\(amountTxt ?? "")"
            lblDenom.text = "\(ticker)"
            lblAmount2.text = ""
            if isBtc || tag == getGdkNetwork("liquid").policyAsset {
                let (fiat, fiatCurrency) = balance.get(tag: "fiat")
                lblAmount2.text = "≈ \(fiat ?? "N.A.") \(fiatCurrency)"
            }
        }
        selectionStyle = .none
        lblAsset.text = isBtc ? "Liquid Bitcoin" : info?.name ?? tag
        self.icon.image = icon
    }
}
