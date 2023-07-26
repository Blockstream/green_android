import Foundation
import UIKit
import gdk

struct AddresseeCellModel {

    var tx: Transaction
    var index: Int
    var isSendAll: Bool { addreessee.isGreedy ?? false }
    var addreessee: Addressee { tx.addressees[index] }
    var assetId: String { addreessee.assetId ?? tx.subaccountItem?.gdkNetwork.getFeeAsset() ?? AssetInfo.btcId }
    var showFiat: Bool { [AssetInfo.btcId, AssetInfo.lbtcId, AssetInfo.ltestId].contains(assetId) }

    var satoshi: Int64 {
        var value = addreessee.satoshi
        if isSendAll{
            value = tx.amounts.filter({$0.key == assetId}).first?.value ?? 0
        }
        return value ?? 0
    }

    var amount: String {
        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId) {
            let (amount, _) = balance.toValue()
            return amount
        }
        return ""
    }

    var ticker: String {
        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId) {
            let (_, ticker) = balance.toValue()
            return ticker
        }
        return ""
    }

    var fiat: String {
        if let balance = Balance.fromSatoshi(satoshi, assetId: assetId) {
            let (fiat, fiatCurrency) = balance.toFiat()
            return "≈ \(fiat) \(fiatCurrency)"
        }
        return ""
    }

    var icon: UIImage? {
        if tx.subaccountItem?.gdkNetwork.lightning ?? false {
            return UIImage(named: "ic_lightning_btc")
        } else {
            let registry = WalletManager.current?.registry
            return registry?.image(for: assetId)
        }
    }
}
