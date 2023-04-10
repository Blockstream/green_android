import Foundation
import UIKit
import gdk

struct RecipientCellModel {

    var address: String?
    var amount: String?
    var assetId: String?
    var isFiat: Bool = false
    var txError = ""
    var isSendAll: Bool = false
    var account: WalletItem
    var inputType: InputType = .transaction

    var isBtc: Bool {
        [AssetInfo.btcId, AssetInfo.lbtcId, AssetInfo.testId, AssetInfo.ltestId].contains(assetId ?? "")
    }

    var asset: AssetInfo? {
        WalletManager.current?.registry.info(for: assetId ?? "")
    }

    var assetImage: UIImage? {
        WalletManager.current?.registry.image(for: assetId ?? "")
    }

    func isBipAddress() -> Bool {
        return account.session?.validBip21Uri(uri: address ?? "") ?? false
    }

    var id: String { assetId ?? account.gdkNetwork.getFeeAsset() }

    var balance: String {
        let satoshi = account.satoshi?[id] ?? 0
        if let (amount, denom) = convert(satoshi) {
            return "\(amount) \(denom)"
        }
        return ""
    }

    var ticker: String {
        let satoshi = account.satoshi?[id] ?? 0
        return convert(satoshi)?.1 ?? ""
    }

    mutating func fromSatoshi(_ satoshi: Int64) {
        amount = convert(satoshi)?.0 ?? ""
    }

    func convert(_ satoshi: Int64) -> (String, String)? {
        let balance = Balance.fromSatoshi(satoshi, assetId: id)
        if isFiat {
            return balance?.toFiat()
        } else if isBtc {
            return balance?.toDenom()
        } else {
            return balance?.toValue()
        }
    }

    func convert(_ amount: String) -> Balance? {
        if isFiat {
            return Balance.fromFiat(amount)
        } else if isBtc {
            return Balance.fromDenomination(amount, assetId: id)
        } else {
            return Balance.fromValue(amount, assetId: id)
        }
    }

    func satoshi() -> Int64? {
        var amountText = (amount ?? "").isEmpty ? "" : amount ?? ""
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else { return nil }
        return convert(amountText)?.satoshi
    }
}
