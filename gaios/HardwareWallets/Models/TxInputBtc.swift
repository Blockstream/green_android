import Foundation

protocol TxInputProtocol: Codable {
    func encode() -> [String: Any]
}

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
    let inputTx: [UInt8]?
    let script: [UInt8]?
    let satoshi: UInt64?
    let path: [UInt32]?
    let aeHostEntropy: [UInt8]?
    let aeHostCommitment: [UInt8]?

    func encode() -> [String: Any] {
        return try! JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any] ?? [:]
    }
}
