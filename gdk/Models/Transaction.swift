import Foundation
import BreezSDK
import lightning

public enum TransactionError: Error {
    case invalid(localizedDescription: String)
}

public enum TxType: Codable {
    case transaction
    case sweep
    case bumpFee
    case bolt11
    case lnurl
}

public struct Addressee: Codable {
    enum CodingKeys: String, CodingKey {
        case address
        case satoshi
        case isGreedy = "is_greedy"
        case assetId = "asset_id"
        case hasLockedAmount = "has_locked_amount"
        case minAmount = "min_amount"
        case maxAmount = "max_amount"
        case domain
        case metadata
        case type
    }
    public var address: String
    public var satoshi: Int64?
    public var isGreedy: Bool?
    public var assetId: String?
    public let hasLockedAmount: Bool?
    public let minAmount: UInt64?
    public let maxAmount: UInt64?
    public let domain: String?
    public let metadata: [[String]]?
    public let type: TxType?

    public static func from(address: String, satoshi: Int64?, assetId: String?, isGreedy: Bool = false) -> Addressee {
        return Addressee(address: address,
                         satoshi: satoshi,
                         isGreedy: isGreedy,
                         assetId: assetId,
                         hasLockedAmount: nil,
                         minAmount: nil,
                         maxAmount: nil,
                         domain: nil,
                         metadata: nil,
                         type: .transaction)
    }

    public static func fromLnInvoice(_ invoice: LnInvoice, fallbackAmount: UInt64) -> Addressee {
        return Addressee(address: invoice.bolt11,
                         satoshi: -Int64(invoice.amountSatoshi ?? fallbackAmount),
                         assetId: nil,
                         hasLockedAmount: invoice.amountMsat != nil,
                         minAmount: nil,
                         maxAmount: nil,
                         domain: nil,
                         metadata: nil,
                         type: .bolt11)
    }

    public static func fromRequestData(_ requestData: LnUrlPayRequestData, input: String, satoshi: UInt64) -> Addressee {
        return Addressee(
            address: input,
            satoshi: -Int64((requestData.sendableSatoshi(userSatoshi: satoshi) ?? 0)),
            assetId: nil,
            hasLockedAmount: requestData.isAmountLocked,
            minAmount: requestData.minSendableSatoshi,
            maxAmount: requestData.maxSendableSatoshi,
            domain: requestData.domain,
            metadata: requestData.metadata,
            type: .lnurl)
    }
}

public struct TransactionOutput: Codable {
    enum CodingKeys: String, CodingKey {
        case address
        case domain
        case assetId = "asset_id"
        case isChange = "is_change"
        case satoshi
    }
    public let address: String?
    public let domain: String?
    public let assetId: String?
    public let isChange: Bool?
    public let satoshi: Int64
    public static func fromLnInvoice(_ invoice: LnInvoice, fallbackAmount: Int64?) -> TransactionOutput {
        return TransactionOutput(
            address: invoice.bolt11,
            domain: nil,
            assetId: nil,
            isChange: false,
            satoshi: -Int64((invoice.amountSatoshi ?? UInt64(fallbackAmount ?? 0)))
        )
    }
    public static func fromLnUrlPay(_ requestData: LnUrlPayRequestData, input: String, satoshi: Int64?) -> TransactionOutput {
        return TransactionOutput(
            address: input,
            domain: requestData.domain,
            assetId: nil,
            isChange: false,
            satoshi: -Int64(requestData.sendableSatoshi(userSatoshi: UInt64(satoshi ?? 0)) ?? 0)
        )
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
        get { (get("addressees") ?? []).compactMap { Addressee.from($0) as? Addressee }}
        set { details["addressees"] = newValue.map { $0.toDict() }}
    }

    public var transaction: String? {
        get { return get("transaction") }
    }

    public var blockHeight: UInt32 {
        get { return get("block_height") ?? 0 }
        set { details["block_height"] = newValue }
    }

    public var privateKey: String? {
        get { return get("private_key") }
        set { details["private_key"] = newValue }
    }

    public var canRBF: Bool {
        get { return get("can_rbf") ?? false }
        set { details["can_rbf"] = newValue }
    }

    public var createdAtTs: Int64 {
        get { return get("created_at_ts") ?? 0 }
        set { details["created_at_ts"] = newValue }
    }

    public var error: String {
        get { return get("error") ?? String() }
        set { details["error"] = newValue }
    }

    public var fee: UInt64 {
        get { return get("fee") ?? 0 }
        set { details["fee"] = newValue }
    }

    public var feeRate: UInt64 {
        get { return get("fee_rate" ) ?? 0 }
        set { details["fee_rate"] = newValue }
    }

    public var hash: String {
        get { return get("txhash") ?? String() }
        set { details["txhash"] = newValue }
    }

    public var isSweep: Bool {
        get { privateKey != nil }
    }

    public var memo: String {
        get { return get("memo") ?? String() }
        set { details["memo"] = newValue }
    }

    public var isLiquid: Bool {
        amounts["btc"] == nil
    }

    public var sessionSubaccount: UInt32 {
        get { get("subaccount") as UInt32? ?? 0 }
        set { details["subaccount"] = newValue }
    }

    public var amounts: [String: Int64] {
        get { get("satoshi") as [String: Int64]? ?? [:] }
        set { details["satoshi"] = newValue }
    }

    public var size: UInt64 {
        get { return get("transaction_vsize") ?? 0 }
        set { details["transaction_vsize"] = newValue }
    }

    public var type: TransactionType {
        get { TransactionType(rawValue: get("type") ?? "") ?? .outgoing }
        set { details["type"] = newValue.rawValue }
    }

    public var previousTransaction: [String: Any]? {
        get { get("previous_transaction") }
        set { details["previous_transaction"] = newValue }
    }

    // tx outputs in create transaction
    public var transactionOutputs: [TransactionOutput]? {
        get {
            let params: [[String: Any]]? = get("transaction_outputs")
            return params?.compactMap { TransactionOutput.from($0) as? TransactionOutput }
        }
        set { details["transaction_outputs"] = newValue?.map { $0.toDict() } }
    }

    // tx utxos
    public var utxos: [String: Any]? {
        get { return get("utxos") }
        set { details["utxos"] = newValue }
    }

    // tx outputs in get transaction
    public var outputs: [[String: Any]]? {
        get { return get("outputs") }
        set { details["outputs"] = newValue }
    }

    // tx inputs in get transaction
    public var inputs: [[String: Any]]? {
        get { return get("inputs") }
        set { details["inputs"] = newValue }
    }

    public var spvVerified: String? {
        get { return get("spv_verified") }
        set { details["spv_verified"] = newValue }
    }

    public var message: String? {
        get { return get("message") }
        set { details["message"] = newValue }
    }

    public var plaintext: [String: String]? {
        get { return get("plaintext") }
        set { details["plaintext"] = newValue }
    }

    public var url: [String: String]? {
        get { return get("url") }
        set { details["url"] = newValue }
    }

    public var isPendingCloseChannel: Bool? {
        get { return get("isPendingCloseChannel") }
        set { details["isPendingCloseChannel"] = newValue }
    }

    public var isLightningSwap: Bool? {
        get { return get("isLightningSwap") }
        set { details["isLightningSwap"] = newValue }
    }

    public var isInProgressSwap: Bool? {
        get { return get("isInProgressSwap") }
        set { details["isInProgressSwap"] = newValue }
    }

    public var isRefundableSwap: Bool? {
        get { return get("isRefundableSwap") }
        set { details["isRefundableSwap"] = newValue }
    }

    public var txType: TxType {
        if privateKey != nil {
            return .sweep
        } else if previousTransaction != nil {
            return .bumpFee
        } else {
            return addressees.first?.type ?? .transaction
        }
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
