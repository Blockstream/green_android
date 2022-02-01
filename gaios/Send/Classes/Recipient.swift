import Foundation

class Recipient: Codable {
    var address: String?
    var amount: String?
    var assetId: String?
    var isFiat: Bool = false
    var txError = ""
    var fee: UInt64?
    var feeRate: UInt64?
    var satoshi: UInt64?
    var amounts: [String: UInt64]?
    var isSendAll: Bool = false

    private var asset: AssetInfo? {
        guard let assetId = self.assetId else {
            return nil
        }
        return Registry.shared.infos[assetId] ?? AssetInfo(assetId: assetId, name: assetId, precision: 0, ticker: "")
    }

    private var btc: String {
        return AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
    }

    func getSatoshi() -> UInt64? {
        guard let assetId = self.assetId, let asset = self.asset else {
            return nil
        }
        var amountText = amount ?? ""
        amountText = amountText.isEmpty ? "0" : amountText
        amountText = amountText.unlocaleFormattedString(8)
        guard let number = Double(amountText), number > 0 else { return nil }
        let isBtc = assetId == btc
        let denominationBtc = SessionsManager.current.settings!.denomination.rawValue
        let key = isFiat ? "fiat" : (isBtc ? denominationBtc : assetId)
        let details: [String: Any]
        if isBtc {
            details = [key: amountText]
        } else {
            details = [key: amountText, "asset_info": asset.encode()!]
        }
        return Balance.convert(details: details)?.satoshi
    }
}
