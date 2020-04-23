import Foundation

struct TxInputBtc: Codable {
    enum CodingKeys: String, CodingKey {
        case inputTx = "inputTx"
        case script = "script"
        case isWitness = "is_witness"
        case path = "path"
        case satoshi = "satoshi"
    }
    let isWitness: Bool
    let inputTx: Data?
    let script: Data?
    let satoshi: UInt64?
    let path: [UInt32]?

    func encode() -> [String: Any] {
        var inputParams = try! JSONSerialization.jsonObject(with: JSONEncoder().encode(self), options: .allowFragments) as? [String: Any] ?? [:]
        if script != nil {
            inputParams["script"] = script!
        }
        if inputTx != nil {
            inputParams["inputTx"] = inputTx!
        }
        return inputParams
    }
}
