import Foundation
import greenaddress

public struct AuthTxInput: Codable {
    enum CodingKeys: String, CodingKey {
        case addressType = "address_type"
        case prevoutScript = "prevout_script"
        case userPath = "user_path"
        case aeHostCommitment = "ae_host_commitment"
        case aeHostEntropy = "ae_host_entropy"
        case assetblinder
        case amountblinder
        case commitment
        case satoshi
        case txhash
        case ptIdx = "pt_idx"
        case sequence
    }
    let addressType: String
    let prevoutScript: String
    let userPath: [UInt32]
    let commitment: String?
    let aeHostCommitment: String?
    let aeHostEntropy: String?
    let satoshi: UInt64
    let txhash: String
    let assetblinder: String?
    let amountblinder: String?
    let ptIdx: UInt?
    let sequence: UInt?

    var ptIdxHex: [UInt8] { (ptIdx ?? 0).uint32LE() }
    var txhashHex: [UInt8] { hexToData(txhash).reversed() }
    var assetblinderHex: [UInt8] { hexToData(assetblinder ?? "").reversed() }
    var amountblinderHex: [UInt8] { hexToData(amountblinder ?? "").reversed() }
}

public struct AuthTxOutput: Codable {
    enum CodingKeys: String, CodingKey {
        case isChange = "is_change"
        case addressType = "address_type"
        case subtype
        case userPath = "user_path"
        case recoveryXpub = "recovery_xpub"
        case satoshi
        case asset_id
        case script
        case blindingKey = "blinding_key"
    }
    let isChange: Bool?
    let addressType: String?
    let subtype: UInt32?
    let userPath: [UInt32]?
    let recoveryXpub: String?
    let satoshi: UInt64?
    let asset_id: String?
    let script: String?
    let blindingKey: String?
}

public struct AuthTx: Codable {
    enum CodingKeys: String, CodingKey {
        case transaction
        case transactionVersion = "transaction_version"
        case transactionLocktime = "transaction_locktime"
    }
    let transaction: String
    let transactionVersion: UInt?
    let transactionLocktime: UInt?
    var hex: Data { hexToData(transaction) }
}

public struct AuthSignTransaction: Codable {
    enum CodingKeys: String, CodingKey {
        case transaction
        case signingInputs = "signing_inputs"
        case txOutputs = "transaction_outputs"
        case signingTxs = "signing_transactions"
        case useAeProtocol = "use_ae_protocol"
    }
    let transaction: AuthTx
    let signingInputs: [AuthTxInput]
    let txOutputs: [AuthTxOutput]
    let signingTxs: [String: String]?
    let useAeProtocol: Bool?
}

public struct AuthSignTransactionResponse: Codable {
    enum CodingKeys: String, CodingKey {
        case signatures
        case signerCommitments = "signer_commitments"
        case assetCommitments = "asset_commitments"
        case valueCommitments = "value_commitments"
        case assetblinders
        case amountblinders
    }
    let signatures: [String]
    let signerCommitments: [String?]?
    let assetCommitments: [String?]?
    let valueCommitments: [String?]?
    let assetblinders: [String?]?
    let amountblinders: [String?]?
    public init(signatures: [String], signerCommitments: [String?]? = nil, assetCommitments: [String?]? = nil, valueCommitments: [String?]? = nil, assetblinders: [String?]? = nil, amountblinders: [String?]? = nil) {
        self.signatures = signatures
        self.signerCommitments = signerCommitments
        self.assetCommitments = assetCommitments
        self.valueCommitments = valueCommitments
        self.assetblinders = assetblinders
        self.amountblinders = amountblinders
    }
}
