import Foundation

class Recipient: Codable {
    var address: String?
    var amount: String?
    var assetId: String?
    var isFiat: Bool = false
    var txError = ""

    var isSendAll: Bool = false

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    func getSatoshi() -> Int64? {
        var amountText = amount ?? ""
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else {
            return nil
        }
        if isFiat {
            return Balance.fromFiat(amountText)?.satoshi
        } else if assetId == nil || assetId == btc {
            return Balance.fromDenomination(amountText)?.satoshi
        } else {
            let asset = WalletManager.current?.registry.info(for: assetId ?? btc)
            return Balance.fromValue(amountText, asset: asset)?.satoshi
        }
    }
}
