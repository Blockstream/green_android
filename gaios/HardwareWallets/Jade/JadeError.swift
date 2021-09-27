import Foundation

enum JadeError: Error {
    case Abort(_ localizedDescription: String)
    case URLError(_ localizedDescription: String)
    case Declined(_ localizedDescription: String)

    static let CBOR_RPC_USER_CANCELLED = -32000

    static func from(code: Int, message: String) -> JadeError {
        switch code {
        case JadeError.CBOR_RPC_USER_CANCELLED:
            return .Declined(message)
        default:
            return .Abort(message)
        }
    }
}
