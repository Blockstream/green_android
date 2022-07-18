import Foundation

class Recipient: Codable {
    var address: String?
    var amount: String?
    var assetId: String?
    var isFiat: Bool = false
    var txError = ""

    var isSendAll: Bool = false

    private var asset: AssetInfo? {
        guard let assetId = self.assetId else {
            return nil
        }
        return SessionsManager.current?.registry?.info(for: assetId)
    }

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    func getSatoshi() -> UInt64? {
        if self.assetId == nil {
            return nil
        }
        var amountText = amount ?? ""
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else {
            return nil
        }
        if isFiat {
            return Balance.fromFiat(amountText)?.satoshi
        } else {
            return Balance.fromDenomination(amountText)?.satoshi
        }
    }
}
