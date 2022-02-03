import Foundation

struct Connection: Codable {
    enum CodingKeys: String, CodingKey {
        case currentState = "current_state"
        case nextState = "next_state"
        case backoffMs = "backoff_ms"
    }
    let currentState: String
    let nextState: String
    let backoffMs: UInt8
}
