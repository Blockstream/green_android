import Foundation
import PromiseKit

struct Transactions {
    let list: [Transaction]

    init(list: [Transaction]) {
        self.list = list
    }
}

enum TransactionError: Error {
    case invalid(localizedDescription: String)
}

struct Addressee: Codable {

    enum CodingKeys: String, CodingKey {
        case address
        case satoshi
        case assetTag = "asset_tag"
    }

    let address: String
    let satoshi: UInt64
    let assetTag: String?

    init(address: String, satoshi: UInt64, assetTag: String? = nil) {
        self.address = address
        self.satoshi = satoshi
        self.assetTag = assetTag
    }
}

struct Transaction {
    var details: [String: Any]

    private func get<T>(_ key: String) -> T? {
        return details[key] as? T
    }

    init(_ details: [String: Any]) {
        self.details = details
    }

    var addressees: [Addressee] {
        get {
            let out: [[String: Any]] = get("addressees") ?? []
            return out.map { value in
                let address = value["address"] as? String
                let satoshi = value["satoshi"] as? UInt64
                let assetTag = value["asset_tag"] as? String
                return Addressee(address: address!, satoshi: satoshi!, assetTag: assetTag)
            }
        }
        set {
            let addressees = newValue.map { addr -> [String: Any] in
                var out = [String: Any]()
                out["address"] = addr.address
                out["satoshi"] = addr.satoshi
                out["asset_tag"] = addr.assetTag
                return out
            }
            details["addressees"] = addressees
        }
    }

    var addresseesReadOnly: Bool {
        get { return get("addressees_read_only") ?? false }
    }

    var blockHeight: UInt32 {
        get { return get("block_height") ?? 0 }
    }

    var canRBF: Bool {
        get { return get("can_rbf") ?? false }
    }

    var createdAt: String {
        get { return get("created_at") ?? String() }
    }

    var error: String {
        get { return get("error") ?? String() }
        set { details["error"] = newValue }
    }

    var fee: UInt64 {
        get { return get("fee") ?? 0 }
    }

    var feeRate: UInt64 {
        get { return get("fee_rate" ) ?? 0 }
        set { details["fee_rate"] = newValue }
    }

    var hash: String {
        get { return get("txhash") ?? String() }
    }

    var isSweep: Bool {
        get { return get("is_sweep") ?? false }
    }

    var memo: String {
        get { return get("memo") ?? String() }
        set { details["memo"] = newValue }
    }

    var satoshi: UInt64 {
        get {
            let dict = get("satoshi") as [String: Any]?
            return dict?["btc"] as? UInt64 ?? 0
        }
    }

    var amounts: [String: UInt64] {
        get {
            return get("satoshi") as [String: UInt64]? ?? [:]
        }
    }

    static func sort<T>(_ dict: [String: T]) -> [(key: String, value: T)] {
        var sorted = dict.filter { $0.key != "btc" }.sorted(by: {$0.0 < $1.0 })
        if dict.contains(where: { $0.key == "btc" }) {
            sorted.insert((key: "btc", value: dict["btc"]!), at: 0)
        }
        return Array(sorted)
    }

    var defaultAsset: String {
        return Transaction.sort(amounts).filter { $0.key != "btc" }.first?.key ?? "btc"
    }

    var sendAll: Bool {
        get { return get("send_all") ?? false }
        set { details["send_all"] = newValue }
    }

    var size: UInt64 {
        get { return get("transaction_vsize") ?? 0 }
    }

    var type: String {
        get { return get("type") ?? String() }
    }

    func address() -> String? {
        let out: [String] = get("addressees") ?? []
        guard !out.isEmpty else {
            return nil
        }
        return out[0]
    }

    func date(dateStyle: DateFormatter.Style, timeStyle: DateFormatter.Style) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        dateFormatter.locale = Locale(identifier: "en_US_POSIX") // set locale to reliable US_POSIX
        guard let date = dateFormatter.date(from: createdAt) else {
            return createdAt
        }
        return DateFormatter.localizedString(from: date, dateStyle: dateStyle, timeStyle: timeStyle)
    }
}

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
    let fiat: String
    let fiatCurrency: String
    let fiatRate: String
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

    func get(tag: String) -> (String, String) {
        if "fiat" == tag {
            return (fiat.localeFormattedString(2), fiatCurrency)
        }
        if "btc" == tag {
            let denomination = getGAService().getSettings()?.denomination ?? .BTC
            let res = try? JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any]
            let value = res![denomination.rawValue] as? String
            return (value!.localeFormattedString(denomination.digits), denomination.string)
        }
        if let asset = asset?[tag] {
            return (asset.localeFormattedString(Int(assetInfo?.precision ?? 8)), assetInfo?.ticker ?? "")
        }
        return ("", "")
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
    }

    private let name: String
    let pointer: UInt32
    var receiveAddress: String?
    let receivingId: String
    let type: String
    var satoshi: [String: UInt64]
    var btc: UInt64 { get { return satoshi["btc"]! }}

    func localizedName() -> String {
        return pointer == 0 ? NSLocalizedString("id_main_account", comment: "") : name
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
