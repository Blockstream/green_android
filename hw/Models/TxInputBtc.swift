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
    let path: [UInt32]?
    let aeHostEntropy: Data?
    let aeHostCommitment: Data?

    public init(isWitness: Bool, inputTxHex: String?, scriptHex: String?, satoshi: UInt64?, path: [UInt32]?, aeHostEntropyHex: String?, aeHostCommitmentHex: String?) {
        self.isWitness = isWitness
        self.inputTx = inputTxHex?.hexToData()
        self.script = scriptHex?.hexToData()
        self.satoshi = satoshi
        self.path = path
        self.aeHostEntropy = aeHostEntropyHex?.hexToData()
        self.aeHostCommitment = aeHostCommitmentHex?.hexToData()
    }
}
