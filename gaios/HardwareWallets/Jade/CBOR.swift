import Foundation
import SwiftCBOR

extension CBOR {
    static func parser(_ cbor: CBOR) -> Any? {
        switch cbor {
        case .simple(let obj):
            return obj
        case .boolean(let obj):
            return obj
        case .unsignedInt(let obj):
            return obj
        case .utf8String(let obj):
            return obj
        case .array(let obj):
            return obj.map { CBOR.parser($0)}
        case .map(let obj):
            var out = [String: Any]()
            for (key, value) in obj {
                let k = CBOR.parser(key) as? String
                out[k ?? ""] = CBOR.parser(value)
            }
            return out
        case .byteString(let obj):
            return obj
        default:
            return cbor
        }
    }
}
