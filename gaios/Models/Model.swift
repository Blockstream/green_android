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
        case assetId = "asset_id"
    }

    let address: String
    let satoshi: UInt64
    let assetId: String?

    init(address: String, satoshi: UInt64, assetId: String? = nil) {
        self.address = address
        self.satoshi = satoshi
        self.assetId = assetId
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
                let assetId = value["asset_id"] as? String
                return Addressee(address: address!, satoshi: satoshi ?? 0, assetId: assetId)
            }
        }
        set {
            let addressees = newValue.map { addr -> [String: Any] in
                var out = [String: Any]()
                out["address"] = addr.address
                out["satoshi"] = addr.satoshi
                out["asset_id"] = addr.assetId
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
            let btc = getGdkNetwork(getNetwork()).getFeeAsset()
            return dict?[btc] as? UInt64 ?? 0
        }
    }

    var amounts: [String: UInt64] {
        get {
            return get("satoshi") as [String: UInt64]? ?? [:]
        }
    }

    static func sort<T>(_ dict: [String: T]) -> [(key: String, value: T)] {
        let btc = getGdkNetwork(getNetwork()).getFeeAsset()
        var sorted = dict.filter { $0.key != btc }.sorted(by: {$0.0 < $1.0 })
        if dict.contains(where: { $0.key == btc }) {
            sorted.insert((key: btc, value: dict[btc]!), at: 0)
        }
        return Array(sorted)
    }

    /// Asset we are trying to send or receive, other than bitcoins for fees
    var defaultAsset: String {
        return Transaction.sort(amounts).filter { $0.key != getGdkNetwork(getNetwork()).getFeeAsset() }.first?.key ?? getGdkNetwork(getNetwork()).getFeeAsset()
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

    var transactionOutputs: [[String: Any]]? {
        get { return get("outputs") }
    }

    var transactionInputs: [[String: Any]]? {
        get { return get("inputs") }
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

    func hasBlindingData(data: [String: Any]) -> Bool {
        return data["asset_id"] != nil && data["satoshi"] != nil && data["assetblinder"] != nil && data["amountblinder"] != nil
    }

    func txoBlindingData(data: [String: Any], isUnspent: Bool) -> [String: Any] {
        var blindingData = [String: Any]()
        let index = isUnspent ? "vout" : "vin"
        blindingData[index] = data["pt_idx"]
        blindingData["asset_id"] = data["asset_id"]
        blindingData["assetblinder"] = data["assetblinder"]
        blindingData["satoshi"] = data["satoshi"]
        blindingData["amountblinder"] = data["amountblinder"]
        return blindingData
    }

    func txoBlindingString(data: [String: Any]) -> String? {
        if !hasBlindingData(data: data) {
            return nil
        }
        let satoshi = data["satoshi"] as? UInt64 ?? 0
        let assetId = data["asset_id"] as? String ?? ""
        let amountBlinder = data["amountblinder"] as? String ?? ""
        let assetBlinder = data["assetblinder"] as? String ?? ""
        return String(format: "%d,%@,%@,%@", satoshi, assetId, amountBlinder, assetBlinder)
    }

    func blindingData() -> [String: Any]? {
        var txBlindingData = [String: Any]()
        txBlindingData["version"] = 0
        txBlindingData["txid"] = hash
        txBlindingData["type"] = type
        txBlindingData["inputs"] = transactionInputs?.filter { (data: [String: Any]) -> Bool in
            return hasBlindingData(data: data)
        }.map { (data: [String: Any]) -> [String: Any] in
            return txoBlindingData(data: data, isUnspent: false)
        }
        txBlindingData["outputs"] = transactionOutputs?.filter { (data: [String: Any]) -> Bool in
            return hasBlindingData(data: data)
        }.map { (data: [String: Any]) -> [String: Any] in
            return txoBlindingData(data: data, isUnspent: true)
        }
        return txBlindingData
    }

    func blindingUrlString() -> String {
        var blindingUrlString = [String]()
        transactionInputs?.forEach { input in
            if let b = txoBlindingString(data: input) {
                blindingUrlString.append(b)
            }
        }
        transactionOutputs?.forEach { output in
            if let b = txoBlindingString(data: output) {
                blindingUrlString.append(b)
            }
        }
        return blindingUrlString.isEmpty ? "" : "#blinded=" + blindingUrlString.joined(separator: ",")
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
