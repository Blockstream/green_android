import Foundation
import PromiseKit

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
    let satoshi: UInt64
    let ubtc: String
    let sats: String
    let assetInfo: AssetInfo?
    var asset: [String: String]?

    static func convert(details: [String: Any]) -> Balance? {
        guard var res = try? SessionsManager.current.convertAmount(input: details) else { return nil}
        res["asset_info"] = details["asset_info"]
        var balance = try? JSONDecoder().decode(Balance.self, from: JSONSerialization.data(withJSONObject: res, options: []))
        if let assetInfo = balance?.assetInfo {
            let value = res[assetInfo.assetId] as? String
            balance?.asset = [assetInfo.assetId: value!]
        }
        return balance
    }

    func get(tag: String) -> (String?, String) {
        let feeAsset = AccountsManager.shared.current?.gdkNetwork?.getFeeAsset()
        let mainnet = AccountsManager.shared.current?.gdkNetwork?.mainnet ?? true
        if "fiat" == tag {
            return (fiat?.localeFormattedString(2), mainnet ? fiatCurrency : "FIAT")
        }
        if feeAsset == tag {
            let denomination = SessionsManager.current.settings?.denomination ?? .BTC
            let res = try? JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any]
            let value = res![denomination.rawValue] as? String
            return (value!.localeFormattedString(denomination.digits), denomination.string)
        }
        if let asset = asset?[tag] {
            return (asset.localeFormattedString(Int(assetInfo?.precision ?? 8)), assetInfo?.ticker ?? "")
        }
        return (nil, "")
    }
}

func getTransactionDetails(txhash: String) -> Promise<[String: Any]> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) {
        try SessionsManager.current.getTransactionDetails(txhash: txhash)
    }
}

func createTransaction(details: [String: Any]) -> Promise<Transaction> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) {
        try SessionsManager.current.createTransaction(details: details)
    }.then(on: bgq) { call in
        call.resolve()
    }.map(on: bgq) { data in
        let result = data["result"] as? [String: Any]
        return Transaction(result ?? [:])
    }
}

func signTransaction(details: [String: Any]) -> Promise<TwoFactorCall> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) {
        try SessionsManager.current.signTransaction(details: details)
    }
}

func createTransaction(transaction: Transaction) -> Promise<Transaction> {
    return createTransaction(details: transaction.details)
}

func signTransaction(transaction: Transaction) -> Promise<TwoFactorCall> {
    return signTransaction(details: transaction.details)
}

func convertAmount(details: [String: Any]) -> [String: Any]? {
    return try? SessionsManager.current.convertAmount(input: details)
}

func getFeeEstimates() -> [UInt64]? {
    let estimates = try? SessionsManager.current.getFeeEstimates()
    return estimates == nil ? nil : estimates!["fees"] as? [UInt64]
}

func getUserNetworkSettings() -> [String: Any] {
    if let settings = UserDefaults.standard.value(forKey: "network_settings") as? [String: Any] {
        return settings
    }
    return [:]
}

func removeKeychainData() {
    let network = getNetwork()
    _ = AuthenticationTypeHandler.removeAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
    _ = AuthenticationTypeHandler.removeAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: network)
}

func removeBioKeychainData() {
    let network = getNetwork()
    _ = AuthenticationTypeHandler.removeAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
}

func removePinKeychainData() {
    let network = getNetwork()
    _ = AuthenticationTypeHandler.removeAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: network)
}

func isPinEnabled(network: String) -> Bool {
    let bioData = AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyBiometric, forNetwork: network)
    let pinData = AuthenticationTypeHandler.findAuth(method: AuthenticationTypeHandler.AuthKeyPIN, forNetwork: network)
    return pinData || bioData
}

func onFirstInitialization(network: String) {
    // Generate a keypair to encrypt user data
    let initKey = network + "FirstInitialization"
    if !UserDefaults.standard.bool(forKey: initKey) {
        removeKeychainData()
        UserDefaults.standard.set(true, forKey: initKey)
    }
}
