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
        guard var res = try? getSession().convertAmount(input: details) else { return nil}
        res["asset_info"] = details["asset_info"]
        var balance = try? JSONDecoder().decode(Balance.self, from: JSONSerialization.data(withJSONObject: res, options: []))
        if let assetInfo = balance?.assetInfo {
            let value = res[assetInfo.assetId] as? String
            balance?.asset = [assetInfo.assetId: value!]
        }
        return balance
    }

    func get(tag: String) -> (String?, String) {
        let feeAsset = AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() ?? ""
        if "fiat" == tag {
            return (fiat?.localeFormattedString(2), fiatCurrency)
        }
        if feeAsset == tag {
            let denomination = Settings.shared?.denomination ?? .BTC
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

class WalletItem: Codable {

    enum CodingKeys: String, CodingKey {
        case name
        case pointer
        case receiveAddress
        case receivingId = "receiving_id"
        case type
        case satoshi
        case recoveryChainCode = "recovery_chain_code"
        case recoveryPubKey = "recovery_pub_key"
    }

    private let name: String
    let pointer: UInt32
    var receiveAddress: String?
    let receivingId: String
    let type: String
    var satoshi: [String: UInt64]?
    var btc: UInt64 { get { return satoshi[getGdkNetwork(getNetwork()).getFeeAsset()] ?? 0 }}
    var recoveryChainCode: String?
    var recoveryPubKey: String?

    func localizedName() -> String {
        if !name.isEmpty {
            return name
        }
        if pointer == 0 {
            return NSLocalizedString("id_main_account", comment: "")
        }
        return NSLocalizedString("id_account", comment: "") + " \(pointer)"
    }

    func generateNewAddress() -> Promise<String> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().compactMap(on: bgq) {_ in
            try getSession().getReceiveAddress(details: ["subaccount": self.pointer])
        }.then(on: bgq) { call in
            call.resolve()
        }.compactMap(on: bgq) { data in
            let result = data["result"] as? [String: Any]
            return result?["address"] as? String ?? ""
        }
    }

    func getAddress() -> Promise<String> {
        if let address = receiveAddress {
            return Guarantee().compactMap { _ in
                return address
            }
        }
        return generateNewAddress().compactMap { address in
            self.receiveAddress = address
            return address
        }
    }

    func getBalance() -> Promise<[String: UInt64]> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().compactMap(on: bgq) {
            try getSession().getBalance(details: ["subaccount": self.pointer, "num_confs": 0])
        }.then(on: bgq) { call in
            call.resolve()
        }.compactMap { data in
            let satoshi = data["result"] as? [String: UInt64]
            self.satoshi = satoshi ?? [:]
            return satoshi
        }
    }
}

func getTransactions(_ pointer: UInt32, first: UInt32 = 0) -> Promise<Transactions> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) {_ in
        try getSession().getTransactions(details: ["subaccount": pointer, "first": first, "count": 15])
    }.then(on: bgq) { call in
        call.resolve()
    }.compactMap(on: bgq) { data in
        let result = data["result"] as? [String: Any]
        let dict = result?["transactions"] as? [[String: Any]]
        let list = dict?.map { Transaction($0) }
        return Transactions(list: list ?? [])
    }
}

func getTransactionDetails(txhash: String) -> Promise<[String: Any]> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) {
        try getSession().getTransactionDetails(txhash: txhash)
    }
}

func createTransaction(details: [String: Any]) -> Promise<Transaction> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) {
        try getSession().createTransaction(details: details)
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
        try getSession().signTransaction(details: details)
    }
}

func createTransaction(transaction: Transaction) -> Promise<Transaction> {
    return createTransaction(details: transaction.details)
}

func signTransaction(transaction: Transaction) -> Promise<TwoFactorCall> {
    return signTransaction(details: transaction.details)
}

func convertAmount(details: [String: Any]) -> [String: Any]? {
    return try? getSession().convertAmount(input: details)
}

func getFeeEstimates() -> [UInt64]? {
    let estimates = try? getSession().getFeeEstimates()
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

func getSubaccount(_ pointer: UInt32) -> Promise<WalletItem> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) {
        try getSession().getSubaccount(subaccount: pointer)
    }.then(on: bgq) { call in
        call.resolve()
    }.compactMap(on: bgq) { data in
        let result = data["result"] as? [String: Any]
        let jsonData = try JSONSerialization.data(withJSONObject: result ?? [:])
        return try JSONDecoder().decode(WalletItem.self, from: jsonData)
    }
}

func getSubaccounts() -> Promise<[WalletItem]> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) {
        try getSession().getSubaccounts()
    }.then(on: bgq) { call in
        call.resolve()
    }.compactMap(on: bgq) { data in
        let result = data["result"] as? [String: Any]
        let subaccounts = result?["subaccounts"] as? [[String: Any]]
        let jsonData = try JSONSerialization.data(withJSONObject: subaccounts ?? [:])
        let wallets = try JSONDecoder().decode([WalletItem].self, from: jsonData)
        return wallets
    }
}
