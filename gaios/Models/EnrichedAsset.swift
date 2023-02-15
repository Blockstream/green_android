import Foundation

struct EnrichedAsset: Codable {
    enum CodingKeys: String, CodingKey {
        case id
        case amp
        case weight
    }

    let id: String
    let amp: Bool?
    let weight: Int?
}
