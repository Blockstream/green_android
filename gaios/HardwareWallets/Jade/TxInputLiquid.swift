import Foundation

struct TxInputLiquid: Codable {
    enum CodingKeys: String, CodingKey {
        case isWitness = "is_witness"
        case script = "script"
        case valueCommitment = "value_commitment"
        case path = "path"
    }
    let isWitness: Bool
    let script: Data?
    let valueCommitment: Data?
    let path: [UInt32]?

    func encode() -> [String: Any] {
        var inputParams = try! JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any] ?? [:]
        if script != nil {
            inputParams["script"] = script!
        }
        if valueCommitment != nil {
            inputParams["value_commitment"] = valueCommitment!
        }
        return inputParams
    }
}
