import Foundation
import greenaddress

public struct HWTxInput: Codable {
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
    let userPath: [Int]
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
    var txhashHex: [UInt8] { txhash.hexToData().reversed() }
    var assetblinderHex: [UInt8] { assetblinder?.hexToData().reversed() ?? [] }
    var amountblinderHex: [UInt8] { amountblinder?.hexToData().reversed() ?? [] }
}

public struct HWTxOutput: Codable {
    enum CodingKeys: String, CodingKey {
        case isChange = "is_change"
        case addressType = "address_type"
        case subtype
        case userPath = "user_path"
        case recoveryXpub = "recovery_xpub"
        case satoshi
        case asset_id
        case scriptpubkey
        case blindingKey = "blinding_key"
    }
    let isChange: Bool?
    let addressType: String?
    let subtype: UInt32?
    let userPath: [Int]?
    let recoveryXpub: String?
    let satoshi: UInt64?
    let asset_id: String?
    let scriptpubkey: String?
    let blindingKey: String?
}


public typealias HWTransaction = [String: Any]

public extension HWTransaction {
    private func take<T>(_ key: String) -> T? {
        return self[key] as? T
    }
    var transaction: String? { take("transaction")}
    var transactionVersion: UInt? { take("transaction_version")}
    var transactionLocktime: UInt? { take("transaction_locktime")}
}

public struct HWTransactionInputLedger {
    let prevOut: Data
    let script: Data
    let sequence: Data
    var size = 0
    
    init(_ data: Data) {
        let data = Data(data.bytes)
        prevOut = Data(data[0..<36])
        let (scriptSize, len) = VarintUtils.read(data[36...])
        var offset = 36 + len
        script = data[offset..<offset+scriptSize]
        offset += scriptSize
        sequence = data[offset..<offset+4]
        size = offset + 4
    }
    
    func serialize() -> Data {
        var buffer = Data()
        buffer.append(prevOut)
        buffer.append(VarintUtils.write(script.count))
        buffer.append(script)
        buffer.append(sequence)
        return buffer
    }
}
    
public struct HWTransactionOutputLedger {
    let amount: Data
    let script: Data
    var size = 0
    
    init(_ data: Data) {
        let data = Data(data.bytes)
        amount = data[0..<8]
        let (scriptSize, len) = VarintUtils.read(data[8...])
        size = 8 + len + scriptSize
        script = data[8+len..<size]
    }
    
    func serialize() -> Data {
        var buffer = Data()
        buffer.append(amount)
        buffer.append(VarintUtils.write(script.count))
        buffer.append(script)
        return buffer
    }
}

public struct HWTransactionLedger {
    var version: Data
    var segwit = false
    var locktime: Data
    var inputs = [HWTransactionInputLedger]()
    var outputs = [HWTransactionOutputLedger]()
    
    init(data: Data) {
        
        version = data[0..<4]
        // If num-inputs is zero, this should rather be the segwit flag
        var offset = 4
        var (numberItems, len) = VarintUtils.read(data[4...])
        offset += len
        if numberItems == 0 {
            var (flag, len) = VarintUtils.read(data[offset...])
            offset += len
            if flag != 1 {
                print("Invalid segwit flag value")
            }
            segwit = true
            // The actual number of inputs
            (numberItems, len) = VarintUtils.read(data[offset...])
            offset += len
        }
        // Inputs
        for _ in 0..<numberItems {
            let input = HWTransactionInputLedger(data[offset...])
            offset += input.size
            self.inputs.append(input)
        }
        // Outputs
        (numberItems, len) = VarintUtils.read(data[offset...])
        offset += len
        for _ in 0..<numberItems {
            let output = HWTransactionOutputLedger(data[offset...])
            offset += output.size
            self.outputs.append(output)
        }
        // if segwit, we need to skip over the withness data
        // we know the last 4 bytes are the locktime
        locktime = data[data.count-4..<data.count]
    }
    
}

public struct HWSignTxParams {
    enum CodingKeys: String, CodingKey {
        case transaction
        case signingInputs = "transaction_inputs"
        case txOutputs = "transaction_outputs"
        case signingTxs = "signing_transactions"
        case useAeProtocol = "use_ae_protocol"
    }
    let transaction: String?
    let signingInputs: [InputOutput]
    let txOutputs: [InputOutput]
    let signingTxs: [String: String]
    let useAeProtocol: Bool
    init(_ details: [String: Any]) {
        transaction = details["transaction"] as? String
        signingInputs = details["transaction_inputs"] as? [InputOutput] ?? []
        txOutputs = details["transaction_outputs"] as? [InputOutput] ?? []
        signingTxs = details["signing_transactions"] as? [String: String] ?? [:]
        useAeProtocol = details["use_ae_protocol"] as? Bool ?? false
    }
}

public struct HWSignTxResponse: Codable {
    enum CodingKeys: String, CodingKey {
        case signatures
        case signerCommitments = "signer_commitments"
    }
    let signatures: [String]
    let signerCommitments: [String?]?
    public init(signatures: [String], signerCommitments: [String?]? = nil) {
        self.signatures = signatures
        self.signerCommitments = signerCommitments
    }
}

public struct HWBlindingFactorsParams {
    enum CodingKeys: String, CodingKey {
        case usedUtxos = "used_utxos"
        case transactionOutputs = "transaction_outputs"
    }
    let usedUtxos: [InputOutput]
    let transactionOutputs: [InputOutput]
}

public struct HWBlindingFactorsResult: Codable {
    enum CodingKeys: String, CodingKey {
        case assetblinders
        case amountblinders
    }
    var assetblinders: [String]
    var amountblinders: [String]
    mutating func append(assetblinder: String, amountblinder: String) {
        assetblinders.append(assetblinder)
        amountblinders.append(amountblinder)
    }
}

public struct HWSignMessageParams: Codable {
    enum CodingKeys: String, CodingKey {
        case path
        case message
        case aeHostEntropy = "ae_host_entropy"
        case aeHostCommitment = "ae_host_commitment"
        case useAeProtocol = "use_ae_protocol"
        case createRecoverableSig = "create_recoverable_sig"
    }
    let path: [Int]
    let message: String
    let aeHostEntropy: String?
    let aeHostCommitment: String?
    let useAeProtocol: Bool?
    let createRecoverableSig: Bool?
}

public struct HWSignMessageResult: Codable {
    enum CodingKeys: String, CodingKey {
        case signature
        case signerCommitment = "signer_commitment"
    }
    let signature: String?
    let signerCommitment: String?
}

public struct HWResolverResult: Codable {
    enum CodingKeys: String, CodingKey {
        case xpubs
        case signerCommitment = "signer_commitment"
        case signature
        case signerCommitments = "signer_commitments"
        case signatures
        case assetblinders = "assetblinders"
        case amountblinders = "amountblinders"
        case masterBlindingKey = "master_blinding_key"
        case nonces
        case publicKeys = "public_keys"
    }
    let xpubs: [String?]?
    let signerCommitment: String?
    let signature: String?
    let signerCommitments: [String?]?
    let signatures: [String?]?
    let assetblinders: [String?]?
    let amountblinders: [String?]?
    let masterBlindingKey: String?
    let nonces: [String?]?
    let publicKeys: [String?]?
    internal init(xpubs: [String?]? = nil, signerCommitment: String? = nil, signature: String? = nil, signerCommitments: [String?]? = nil, signatures: [String?]? = nil, assetblinders: [String?]? = nil, amountblinders: [String?]? = nil, masterBlindingKey: String? = nil, nonces: [String?]? = nil, publicKeys: [String?]? = nil) {
        self.xpubs = xpubs
        self.signerCommitment = signerCommitment
        self.signature = signature
        self.signerCommitments = signerCommitments
        self.signatures = signatures
        self.assetblinders = assetblinders
        self.amountblinders = amountblinders
        self.masterBlindingKey = masterBlindingKey
        self.nonces = nonces
        self.publicKeys = publicKeys
    }
}
