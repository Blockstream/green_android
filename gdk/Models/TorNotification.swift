import Foundation

public struct TorNotification: Codable {
    enum CodingKeys: String, CodingKey {
        case tag
        case summary
        case progress
    }
    public let tag: String
    public let summary: String
    public let progress: UInt32
}
