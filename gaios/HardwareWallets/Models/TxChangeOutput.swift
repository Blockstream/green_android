import Foundation

struct TxChangeOutput: Codable {
    enum CodingKeys: String, CodingKey {
        case path = "path"
        case recoveryxpub = "recoveryxpub"
        case csvBlocks = "csv_blocks"
        case variant
    }
    let path: [UInt32]
    let recoveryxpub: String
    let csvBlocks: Int
    let variant: String?
}
