import Foundation

public enum HWError: Error {
    case Abort(_ localizedDescription: String)
    case URLError(_ localizedDescription: String)
    case Declined(_ localizedDescription: String)
    case Disconnected(_ localizedDescription: String)
    case InvalidResponse(_ localizedDescription: String)

    static let CBOR_RPC_USER_CANCELLED = -32000

    static func from(code: Int, message: String) -> HWError {
        switch code {
        case HWError.CBOR_RPC_USER_CANCELLED:
            return .Declined(message)
        default:
            return .Abort(message)
        }
    }
}
