import Foundation

protocol TxInputProtocol: Codable {}

public struct TxInputBtc: TxInputProtocol {
    enum CodingKeys: String, CodingKey {
        case inputTx = "input_tx"
        case script = "script"
        case isWitness = "is_witness"
        case path = "path"
        case satoshi = "satoshi"
        case aeHostEntropy = "ae_host_entropy"
        case aeHostCommitment = "ae_host_commitment"
    }
    let isWitness: Bool
    let inputTx: Data?
    let script: Data?
    let satoshi: UInt64?
    let path: [Int]?
    let aeHostEntropy: Data?
    let aeHostCommitment: Data?

    public init(isWitness: Bool,
                inputTx: Data?,
                script: Data?,
                satoshi: UInt64?,
                path: [Int]?,
                aeHostEntropy: Data?,
                aeHostCommitment: Data?) {
        self.isWitness = isWitness
        self.inputTx = inputTx
        self.script = script
        self.satoshi = satoshi
        self.path = path
        self.aeHostEntropy = aeHostEntropy
        self.aeHostCommitment = aeHostCommitment
    }
}

struct TxInputLiquid: TxInputProtocol {
    enum CodingKeys: String, CodingKey {
        case isWitness = "is_witness"
        case script = "script"
        case valueCommitment = "value_commitment"
        case path = "path"
        case aeHostEntropy = "ae_host_entropy"
        case aeHostCommitment = "ae_host_commitment"
    }
    let isWitness: Bool
    let script: Data?
    let valueCommitment: Data?
    let path: [Int]?
    let aeHostEntropy: Data?
    let aeHostCommitment: Data?

    init(isWitness: Bool,
         script: Data?,
         valueCommitment: Data?,
         path: [Int]?,
         aeHostEntropy: Data?,
         aeHostCommitment: Data?) {
        self.isWitness = isWitness
        self.script = script
        self.valueCommitment = valueCommitment
        self.path = path
        self.aeHostEntropy = aeHostEntropy
        self.aeHostCommitment = aeHostCommitment
    }
}
