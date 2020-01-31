import Foundation
import ga.wally

public func hexToData(_ hex: String) -> Data {
    let hex_bytes: UnsafePointer<Int8> = UnsafePointer(hex)
    precondition(hex.count%2 == 0)
    let length = hex.count/2
    let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: length)
    var written = 0
    wally_hex_to_bytes(hex_bytes, buffer, length, &written)
    precondition(written == length)
    return Data(bytesNoCopy: buffer, count: length, deallocator: .custom({ (_, _)  in
        buffer.deallocate()
    }))
}
