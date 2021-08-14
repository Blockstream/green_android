import Foundation

struct TorNotification: Codable {
    enum CodingKeys: String, CodingKey {
        case tag
        case summary
        case progress
    }
    let tag: String
    let summary: String
    let progress: UInt32
}
