import Foundation
import PromiseKit

struct Transactions {
    let list: [Transaction]
    let nextPageId: UInt32
    let pageId: UInt32

    init(list: [Transaction], nextPageId: UInt32, pageId: UInt32) {
        self.list = list
        self.nextPageId = nextPageId
        self.pageId = pageId
    }
}

enum TransactionError: Error {
    case invalid(localizedDescription: String)
}

struct Addressee: Codable {
    let address: String
    let satoshi: UInt64

    init(address: String, satoshi: UInt64) {
        self.address = address
        self.satoshi = satoshi
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
                return Addressee(address: value["address"] as! String, satoshi: (value["satoshi"] as? UInt64) ?? 0)
            }
        }
        set {
            let addressees = newValue.map { addr -> [String: Any] in
                var out = [String: Any]()
                out["address"] = addr.address
                out["satoshi"] = addr.satoshi
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
        get { return get("satoshi") ?? 0 }
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

    func amount() -> String {
        let satoshi = String.toBtc(satoshi: self.satoshi)
        if type == "outgoing" || type == "redeposit" {
            return "-" + satoshi
        } else {
            return satoshi
        }
    }

    func address() -> String? {
        let out: [String] = get("addressees") ?? []
        guard !out.isEmpty else {
            return nil
        }
        return out[0]
    }

    func date() -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .short
        let date = Date.dateFromString(dateString: createdAt)
        return Date.dayMonthYear(date: date)
    }

}

class WalletItem: Codable {

    enum CodingKeys: String, CodingKey {
        case bits
        case btc
        case fiat
        case fiatCurrency = "fiat_currency"
        case fiatRate = "fiat_rate"
        case mbtc
        case name
        case pointer
        case receiveAddress
        case receivingId = "receiving_id"
        case satoshi
        case type
        case ubtc
    }

    let bits: String
    let btc: String
    let fiat: String
    let fiatCurrency: String
    let fiatRate: String
    let mbtc: String
    private let name: String
    let pointer: UInt32
    var receiveAddress: String?
    let receivingId: String
    var satoshi: UInt64
    let type: String
    let ubtc: String

    func localizedName() -> String {
        return pointer == 0 ? NSLocalizedString("id_main_account", comment: "") : name
    }

    func generateNewAddress() -> String? {
        return try? getSession().getReceiveAddress(subaccount: self.pointer)
    }

    func getAddress() -> String {
        if let address = receiveAddress {
            return address
        }
        receiveAddress = generateNewAddress()
        return receiveAddress ?? String()
    }

    func getBalance() -> Promise<UInt64> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().compactMap(on: bgq) {
            try getSession().getBalance(details: ["subaccount": self.pointer, "num_confs": 0])
        }.compactMap(on: bgq) { balance in
            return balance["satoshi"] as? UInt64
        }.compactMap { satoshi in
            self.satoshi = satoshi
            return satoshi
        }
    }
}

class Wallets: Codable {
    let array: [WalletItem]
}

func getTransactions(_ pointer: UInt32, pageId: Int = 0) -> Promise<Transactions> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) {_ in
        try getSession().getTransactions(details: ["subaccount": pointer, "page_id": pageId])
    }.compactMap(on: bgq) { data in
        let list = (data["list"] as! [[String: Any]]).map { tx -> Transaction in
            return Transaction(tx)
        }
        let nextPageId = data["next_page_id"] as! UInt32
        let pageId = data["page_id"] as! UInt32
        return Transactions(list: list, nextPageId: nextPageId, pageId: pageId)
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
    }.map(on: bgq) { data in
        return Transaction(data)
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
    guard let conversion = try? getSession().convertAmount(input: details) else {
        return nil
    }
    return conversion
}

func getFeeEstimates() -> [UInt64]? {
    guard let estimates = try? getSession().getFeeEstimates() else { return nil }
    return estimates == nil ? nil : estimates!["fees"] as? [UInt64]
}

func getUserNetworkSettings() -> [String: Any]? {
    return UserDefaults.standard.value(forKey: "network_settings") as? [String: Any]
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
    }.compactMap(on: bgq) { data in
        let jsonData = try JSONSerialization.data(withJSONObject: data)
        return try JSONDecoder().decode(WalletItem.self, from: jsonData)
    }
}

func getSubaccounts() -> Promise<[WalletItem]> {
    let bgq = DispatchQueue.global(qos: .background)
    return Guarantee().compactMap(on: bgq) {
        try getSession().getSubaccounts()
    }.compactMap(on: bgq) { data in
        let jsonData = try JSONSerialization.data(withJSONObject: data)
        let wallets: Wallets = try JSONDecoder().decode(Wallets.self, from: jsonData)
        return wallets.array
    }
}

func changeAddresses(_ accounts: [UInt32]) -> Promise<[WalletItem]> {
    let bgq = DispatchQueue.global(qos: .background)
    return getSubaccounts().compactMap(on: bgq) { wallets in
        let updates = wallets.filter { accounts.contains($0.pointer) }
        return updates.map { $0.receiveAddress = $0.generateNewAddress(); return $0 }
    }
}
