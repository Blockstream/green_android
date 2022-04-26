import Foundation

struct Connection: Codable {
    enum CodingKeys: String, CodingKey {
        case currentState = "current_state"
        case nextState = "next_state"
        case waitMs = "wait_ms"
    }
    let currentState: String
    let nextState: String
    let waitMs: UInt8 = 0

    var connected: Bool {
        return currentState == "connected"
    }
}
