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

public func sigToDer(sigDecoded: [UInt8]) throws -> [UInt8] {
    let sig = sigDecoded[1..<sigDecoded.count]
    let sigPtr: UnsafePointer<UInt8> = UnsafePointer(Array(sig))
    let derPtr = UnsafeMutablePointer<UInt8>.allocate(capacity: Int(EC_SIGNATURE_DER_MAX_LEN))
    var written: Int = 0
    if wally_ec_sig_to_der(sigPtr, sig.count, derPtr, Int(EC_SIGNATURE_DER_MAX_LEN), &written) != WALLY_OK {
        throw GaError.GenericError
    }
    let der = Array(UnsafeBufferPointer(start: derPtr, count: written))
    //derPtr.deallocate()
    return der
}

public func bip32KeyFromParentToBase58(isMainnet: Bool = true, pubKey: [UInt8], chainCode: [UInt8], branch: UInt32 ) throws -> String {
    let version = isMainnet ? BIP32_VER_MAIN_PUBLIC : BIP32_VER_TEST_PUBLIC
    var subactkey: UnsafeMutablePointer<ext_key>?
    var branchkey: UnsafeMutablePointer<ext_key>?
    var xpubPtr: UnsafeMutablePointer<Int8>?
    let pubKey_: UnsafePointer<UInt8> = UnsafePointer(pubKey)
    let chainCode_: UnsafePointer<UInt8> = UnsafePointer(chainCode)
    defer {
        bip32_key_free(subactkey)
        bip32_key_free(branchkey)
        wally_free_string(xpubPtr)
    }

    if bip32_key_init(UInt32(version), UInt32(0), UInt32(0), chainCode_, chainCode.count,
                             pubKey_, pubKey.count, nil, 0, nil, 0, nil, 0, subactkey) != WALLY_OK {
        throw GaError.GenericError
    }
    if bip32_key_from_parent(subactkey, branch, UInt32(BIP32_FLAG_KEY_PUBLIC | BIP32_FLAG_SKIP_HASH), branchkey) != WALLY_OK {
        throw GaError.GenericError
    }
    if bip32_key_to_base58(branchkey, UInt32(BIP32_FLAG_KEY_PUBLIC), &xpubPtr) != WALLY_OK {
        throw GaError.GenericError
    }
    return String(cString: xpubPtr!)
}
