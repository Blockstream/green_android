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
            inputParams["input_tx"] = inputTx!
        }
        return inputParams
    }
}
