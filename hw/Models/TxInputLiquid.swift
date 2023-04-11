import Foundation

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
    let path: [UInt32]?
    let aeHostEntropy: Data?
    let aeHostCommitment: Data?

    init(isWitness: Bool, scriptHex: String?, valueCommitmentHex: String?, path: [UInt32]?, aeHostEntropyHex: String?, aeHostCommitmentHex: String?) {
        self.isWitness = isWitness
        self.script = scriptHex?.hexToData()
        self.valueCommitment = valueCommitmentHex?.hexToData()
        self.path = path
        self.aeHostEntropy = aeHostEntropyHex?.hexToData()
        self.aeHostCommitment = aeHostCommitmentHex?.hexToData()
    }
}
