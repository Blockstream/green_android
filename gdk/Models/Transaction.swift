import Foundation

public enum TransactionError: Error {
    case invalid(localizedDescription: String)
}

public struct Addressee: Codable {

    enum CodingKeys: String, CodingKey {
        case address
        case satoshi
        case assetId = "asset_id"
    }

    public let address: String
    public let satoshi: Int64
    public let assetId: String?

    public init(address: String, satoshi: Int64, assetId: String? = nil) {
        self.address = address
        self.satoshi = satoshi
        self.assetId = assetId
    }
}

public enum TransactionType: String, Codable {
    case incoming
    case outgoing
    case redeposit
    case mixed
}

public struct Transaction: Comparable {
    public var details: [String: Any]
    public var subaccount: Int?

    private func get<T>(_ key: String) -> T? {
        return details[key] as? T
    }

    public init(_ details: [String: Any], subaccount: Int? = nil) {
        self.details = details
        self.subaccount = subaccount
    }

    public var addressees: [Addressee] {
        get {
            let out: [[String: Any]] = get("addressees") ?? []
            return out.map { value in
                let address = value["address"] as? String
                let satoshi = value["satoshi"] as? Int64
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

    public var addresseesList: [String] {
        get { get("addressees") ?? [] }
    }

    public var addresseesReadOnly: Bool {
        get { return get("addressees_read_only") ?? false }
    }

    public var transaction: String? {
        get { return get("transaction") }
    }

    public var blockHeight: UInt32 {
        get { return get("block_height") ?? 0 }
    }

    public var canRBF: Bool {
        get { return get("can_rbf") ?? false }
    }

    public var createdAtTs: UInt64 {
        get { return get("created_at_ts") ?? 0 }
    }

    public var error: String {
        get { return get("error") ?? String() }
        set { details["error"] = newValue }
    }

    public var fee: UInt64 {
        get { return get("fee") ?? 0 }
    }

    public var feeRate: UInt64 {
        get { return get("fee_rate" ) ?? 0 }
        set { details["fee_rate"] = newValue }
    }

    public var hash: String {
        get { return get("txhash") ?? String() }
    }

    public var isSweep: Bool {
        get { return get("is_sweep") ?? false }
    }

    public var memo: String {
        get { return get("memo") ?? String() }
        set { details["memo"] = newValue }
    }

    public var isLiquid: Bool {
        amounts["btc"] == nil
    }

    public var amounts: [String: Int64] {
        get {
            return get("satoshi") as [String: Int64]? ?? [:]
        }
    }

    public var sendAll: Bool {
        get { return get("send_all") ?? false }
        set { details["send_all"] = newValue }
    }

    public var size: UInt64 {
        get { return get("transaction_vsize") ?? 0 }
    }

    public var type: TransactionType {
        get { TransactionType(rawValue: get("type") ?? "") ?? .outgoing }
    }

    // tx outputs in create transaction
    public var transactionOutputs: [[String: Any]]? {
        get { return get("transaction_outputs") }
    }

    // tx outputs in get transaction
    public var outputs: [[String: Any]]? {
        get { return get("outputs") }
    }

    // tx inputs in get transaction
    public var inputs: [[String: Any]]? {
        get { return get("inputs") }
    }

    public var spvVerified: String? {
        get { return get("spv_verified") }
    }

    public var isBlinded: Bool {
        get { get("is_blinded") ?? false }
    }

    public func date(dateStyle: DateFormatter.Style, timeStyle: DateFormatter.Style) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(Double(createdAtTs / 1000000)))
        return DateFormatter.localizedString(from: date, dateStyle: dateStyle, timeStyle: timeStyle)
    }

    public func hasBlindingData(data: [String: Any]) -> Bool {
        let satoshi = data["satoshi"] as? Int64 ?? 0
        let assetId = data["asset_id"] as? String ?? ""
        let amountBlinder = data["amountblinder"] as? String ?? ""
        let assetBlinder = data["assetblinder"] as? String ?? ""
        return assetId != "" && satoshi != 0 && amountBlinder != "" && assetBlinder != ""
    }

    public func txoBlindingData(data: [String: Any], isUnspent: Bool) -> TxoBlindingData {
        return TxoBlindingData.init(vin: !isUnspent ? data["pt_idx"] as? Int64 : nil,
                                    vout: isUnspent ? data["pt_idx"] as? Int64 : nil,
                                    asset_id: data["asset_id"] as? String ?? "",
                                    assetblinder: data["assetblinder"] as? String ?? "",
                                    satoshi: data["satoshi"] as? Int64 ?? 0,
                                    amountblinder: data["amountblinder"] as? String ?? "")
    }

    public func txoBlindingString(data: [String: Any]) -> String? {
        if !hasBlindingData(data: data) {
            return nil
        }
        let satoshi = data["satoshi"] as? UInt64 ?? 0
        let assetId = data["asset_id"] as? String ?? ""
        let amountBlinder = data["amountblinder"] as? String ?? ""
        let assetBlinder = data["assetblinder"] as? String ?? ""
        return String(format: "%lu,%@,%@,%@", satoshi, assetId, amountBlinder, assetBlinder)
    }

    public func blindingData() -> BlindingData {
        let inputs = inputs?.filter { (data: [String: Any]) -> Bool in
            return hasBlindingData(data: data)
        }.map { (data: [String: Any]) in
            return txoBlindingData(data: data, isUnspent: false)
        }
        let outputs = outputs?.filter { (data: [String: Any]) -> Bool in
            return hasBlindingData(data: data)
        }.map { (data: [String: Any]) in
            return txoBlindingData(data: data, isUnspent: true)
        }
        return BlindingData(version: 0,
                            txid: hash,
                            type: type,
                            inputs: inputs ?? [],
                            outputs: outputs ?? [])
    }

    public func blindingUrlString() -> String {
        var blindingUrlString = [String]()
        inputs?.forEach { input in
            if let b = txoBlindingString(data: input) {
                blindingUrlString.append(b)
            }
        }
        outputs?.forEach { output in
            if let b = txoBlindingString(data: output) {
                blindingUrlString.append(b)
            }
        }
        return blindingUrlString.isEmpty ? "" : "#blinded=" + blindingUrlString.joined(separator: ",")
    }

    public static func == (lhs: Transaction, rhs: Transaction) -> Bool {
        (lhs.details as NSDictionary).isEqual(to: rhs.details)
    }

    public static func < (lhs: Transaction, rhs: Transaction) -> Bool {
        if lhs.createdAtTs == rhs.createdAtTs {
            if (lhs.type == .incoming && rhs.type == .outgoing) && (lhs.blockHeight == rhs.blockHeight) {
                return false
            }
            if (lhs.type == .outgoing && rhs.type == .incoming) && (lhs.blockHeight == rhs.blockHeight) {
                return true
            }
        }
        return lhs.createdAtTs < rhs.createdAtTs
    }
}

public struct TxoBlindingData: Codable {
    let vin: Int64?
    let vout: Int64?
    let asset_id: String
    let assetblinder: String
    let satoshi: Int64
    let amountblinder: String
}

public struct BlindingData: Codable {
    let version: Int
    let txid: String
    let type: TransactionType
    let inputs: [TxoBlindingData]
    let outputs: [TxoBlindingData]
}
