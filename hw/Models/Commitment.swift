import Foundation

struct Commitment: Codable {
    enum CodingKeys: String, CodingKey {
        case assetId = "asset_id"
        case value = "value"
        case abf = "abf"
        case vbf = "vbf"
        case assetGenerator = "asset_generator"
        case valueCommitment = "value_commitment"
        case blindingKey = "blinding_key"
    }
    let assetId: Data?
    let value: UInt64?
    let abf: Data?
    let vbf: Data?
    var assetGenerator: Data?
    var valueCommitment: Data?
    var blindingKey: Data?
}
