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
    let script: [UInt8]?
    let valueCommitment: [UInt8]?
    let path: [UInt32]?
    let aeHostEntropy: [UInt8]?
    let aeHostCommitment: [UInt8]?

    func encode() -> [String: Any] {
        return try! JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any] ?? [:]
    }
}
