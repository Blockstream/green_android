import Foundation

struct EnrichedAsset: Codable {
    enum CodingKeys: String, CodingKey {
        case id
        case amp
    }
    let id: String
    let amp: Bool?
}
