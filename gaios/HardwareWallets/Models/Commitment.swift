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
    let assetId: [UInt8]
    let value: UInt32
    let abf: [UInt8]
    let vbf: [UInt8]
    let assetGenerator: [UInt8]
    let valueCommitment: [UInt8]
    let hmac: [UInt8]
    var blindingKey: [UInt8]?
}
