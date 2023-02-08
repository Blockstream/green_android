import Foundation
import UIKit

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

    var balance: String {
        let satoshi = account.satoshi?[assetId ?? ""] ?? 0
        if let balance = Balance.fromSatoshi(satoshi, asset: asset) {
            let (amount, denom) = isFiat ? balance.toFiat() : balance.toValue()
            return "\(amount) \(denom)"
        }
        return ""
    }

    var ticker: String {
        let satoshi = account.satoshi?[assetId ?? ""] ?? 0
        if let balance = Balance.fromSatoshi(satoshi, asset: asset) {
            let (_, ticker) = isFiat ? balance.toFiat() : balance.toValue()
            return "\(ticker)"
        }
        return ""
    }

    mutating func fromSatoshi(_ satoshi: Int64) {
        if isFiat {
            amount = Balance.fromSatoshi(satoshi, asset: asset)?.toFiat().0
        } else {
            amount = Balance.fromSatoshi(satoshi, asset: asset)?.toValue().0
        }
    }

    func satoshi() -> Int64? {
        var amountText = (amount ?? "").isEmpty ? "0" : amount ?? "0"
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else { return nil }
        let balance: Balance? = {
            if isFiat {
                return Balance.fromFiat(amountText)
            } else if isBtc {
                return Balance.fromDenomination(amountText)
            } else {
                return Balance.fromValue(amountText, asset: asset)
            }
        }()
        return balance?.satoshi
    }
}
