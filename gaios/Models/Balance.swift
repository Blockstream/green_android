import Foundation

struct Balance: Codable {

    enum CodingKeys: String, CodingKey {
        case bits
        case btc
        case fiat
        case fiatCurrency = "fiat_currency"
        case fiatRate = "fiat_rate"
        case mbtc
        case satoshi
        case ubtc
        case sats
        case assetInfo = "asset_info"
        case asset
    }

    let bits: String
    let btc: String
    let fiat: String?
    let fiatCurrency: String
    let fiatRate: String?
    let mbtc: String
    let satoshi: Int64
    let ubtc: String
    let sats: String
    let assetInfo: AssetInfo?
    var asset: [String: String]?

    static var session: SessionManager? { WalletManager.current?.prominentSession }
    static var lbtc: String { getGdkNetwork("liquid").getFeeAsset() }
    static var ltest: String { getGdkNetwork("testnet-liquid").getFeeAsset() }

    static func from(details: [String: Any]) -> Balance? {
        if var res = try? session?.convertAmount(input: details) {
            res["asset_info"] = details["asset_info"]
            if let data = try? JSONSerialization.data(withJSONObject: res, options: []),
               var balance = try? JSONDecoder().decode(Balance.self, from: data) {
                if let assetInfo = balance.assetInfo,
                   let value = res[assetInfo.assetId] as? String {
                    balance.asset = [assetInfo.assetId: value]
                }
                return balance
            }
        }
        return nil
    }

    static func fromFiat(_ fiat: String) -> Balance? {
        let details: [String: Any] = ["fiat": fiat]
        return Balance.from(details: details)
    }

    static func fromDenomination(_ value: String) -> Balance? {
        let denomination = session?.settings?.denomination.rawValue ?? "btc"
        let details: [String: Any] = [denomination: value]
        return Balance.from(details: details)
    }

    static func fromValue(_ value: String, asset: AssetInfo? = nil) -> Balance? {
        let assetId = asset?.assetId ?? "btc"
        var details: [String: Any] = [assetId: value]
        if let asset = asset, !["btc", lbtc, ltest].contains(asset.assetId) {
            details["asset_info"] = asset.encode()
        }
        return Balance.from(details: details)
    }

    static func fromSatoshi(_ satoshi: Any, asset: AssetInfo? = nil) -> Balance? {
        var details: [String: Any] = ["satoshi": satoshi]
        if let asset = asset, !["btc", lbtc, ltest].contains(asset.assetId) {
            details["asset_info"] = asset.encode()
        }
        return Balance.from(details: details)
    }

    static func fromSatoshi(_ satoshi: UInt64, asset: AssetInfo? = nil) -> Balance? {
        return Balance.fromSatoshi(Int64(satoshi), asset: asset)
    }

    func toFiat() -> (String, String) {
        let mainnet = AccountsManager.shared.current?.gdkNetwork?.mainnet
        if let asset = assetInfo, !["btc", Balance.lbtc, Balance.ltest].contains(asset.assetId) {
            return ("", "")
        } else {
            return (fiat?.localeFormattedString(2) ?? "n/a", mainnet ?? true ? fiatCurrency : "FIAT")
        }
    }

    func toDenom() -> (String, String) {
        let denomination = Balance.session?.settings?.denomination ?? .BTC
        let res = try? JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any]
        let value = res![denomination.rawValue] as? String
        return (value?.localeFormattedString(denomination.digits) ?? "n/a", denomination.string)
    }

    func toAssetValue() -> (String, String) {
        return (asset?.first?.value.localeFormattedString(Int(assetInfo?.precision ?? 8)) ?? "n/a", assetInfo?.ticker ?? "n/a")
    }

    func toValue() -> (String, String) {
        if let asset = assetInfo, !["btc", Balance.lbtc, Balance.ltest].contains(asset.assetId) {
            return toAssetValue()
        } else {
            return toDenom()
        }
    }
}
