import Foundation

struct Commitment: Codable {
    enum CodingKeys: String, CodingKey {
        case assetId = "asset_id"
        case value = "value"
        case abf = "abf"
        case vbf = "vbf"
        case assetGenerator = "asset_generator"
        case valueCommitment = "value_commitment"
        case hmac = "hmac"
        case blindingKey = "blinding_key"
    }
    let assetId: Data
    let value: UInt32
    let abf: Data
    let vbf: Data
    let assetGenerator: Data
    let valueCommitment: Data
    let hmac: Data
    let blindingKey: Data
}
