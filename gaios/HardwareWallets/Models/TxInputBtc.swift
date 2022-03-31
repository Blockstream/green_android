import Foundation

protocol TxInputProtocol: Codable {}

struct TxInputBtc: TxInputProtocol {
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
    
    init(isWitness: Bool, inputTxHex: String?, scriptHex: String?, satoshi: UInt64?, path: [UInt32]?, aeHostEntropyHex: String?, aeHostCommitmentHex: String?) {
        self.isWitness = isWitness
        self.inputTx = hexToDataNil(inputTxHex)
        self.script = hexToDataNil(scriptHex)
        self.satoshi = satoshi
        self.path = path
        self.aeHostEntropy = hexToDataNil(aeHostEntropyHex)
        self.aeHostCommitment = hexToDataNil(aeHostCommitmentHex)
    }
}
