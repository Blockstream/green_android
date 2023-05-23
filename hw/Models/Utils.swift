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
