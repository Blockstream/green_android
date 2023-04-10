import Foundation

public struct Connection: Codable {
    enum CodingKeys: String, CodingKey {
        case currentState = "current_state"
        case nextState = "next_state"
        case waitMs = "wait_ms"
    }
    public let currentState: String
    public let nextState: String
    public let waitMs: UInt8 = 0

    public var connected: Bool {
        return currentState == "connected"
    }
}
