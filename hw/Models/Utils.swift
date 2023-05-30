import Foundation

func compressPublicKey(_ publicKey: [UInt8]) -> [UInt8]? {
    switch publicKey[0] {
    case 0x04:
        if publicKey.count != 65 {
            return nil
        }
    case 0x02, 0x03:
        if publicKey.count != 33 {
            return nil
        }
        return publicKey
    default:
        return nil
    }
    let type = publicKey[64] & 1 != 0 ? 0x03 : 0x02
    return [UInt8(type)] + publicKey[1..<32+1]
}


// Helper to turn the BIP32 paths back into a list of Longs, rather than a list of Integers
// (which may well be expressed as negative [for hardened paths]).
func getUnsignedPath(_ signed: [Int]) -> [UInt32] {
    signed.map { $0 < 0 ? UInt32(abs($0)) : UInt32($0) }
}
