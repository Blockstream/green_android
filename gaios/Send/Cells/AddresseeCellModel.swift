import Foundation
import UIKit
import gdk

struct AddresseeCellModel {

    var tx: Transaction
    var index: Int
    var isSendAll: Bool { tx.sendAll }
    var addreessee: Addressee { tx.addressees[index] }
    var assetId: String { addreessee.assetId ?? tx.subaccountItem?.gdkNetwork.getFeeAsset() ?? "btc" }
    var showFiat: Bool { [AssetInfo.btcId, AssetInfo.lbtcId, AssetInfo.ltestId].contains(assetId) }

    var satoshi: Int64 {
        var value = addreessee.satoshi
        //let asset = (account?.gdkNetwork.liquid ?? false) ? addreessee?.assetId ?? "" : "btc"
        if tx.subaccountItem?.gdkNetwork.multisig ?? false && tx.sendAll {
            value = tx.amounts.filter({$0.key == assetId}).first?.value ?? 0
        }
        return value
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
}
