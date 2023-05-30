import Foundation
import ga.wally

public let WALLY_EC_SIGNATURE_RECOVERABLE_LEN = EC_SIGNATURE_RECOVERABLE_LEN
public let WALLY_BLINDING_FACTOR_LEN = BLINDING_FACTOR_LEN

public func hexToDataNil(_ hex: String?) -> Data? {
    guard let hex = hex else {
        return nil
    }
    return hexToData(hex)
}

public func hexToData(_ hex: String) -> Data {
    precondition(hex.count%2 == 0)
    var data = Data(capacity: hex.count/2)
    var indexIsEven = true
    for i in hex.indices {
        if indexIsEven {
            let byteRange = i...hex.index(after: i)
            let byte = UInt8(hex[byteRange], radix: 16)
            data.append(byte!)
        }
        indexIsEven.toggle()
    }
    return data
}

public func dataToHex(_ data: Data) -> String {
    return data.map { String(format: "%02hhx", $0) }.joined()
}

public func sigToDer(sig: [UInt8]) throws -> [UInt8] {
    let sigPtr = UnsafePointer(sig)
    let derPtr = UnsafeMutablePointer<UInt8>.allocate(capacity: Int(EC_SIGNATURE_DER_MAX_LEN))
    var written: Int = 0
    if wally_ec_sig_to_der(sigPtr, sig.count, derPtr, Int(EC_SIGNATURE_DER_MAX_LEN), &written) != WALLY_OK {
        throw GaError.GenericError()
    }
    let der = Array(UnsafeBufferPointer(start: derPtr, count: written))
    // derPtr.deallocate()
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

    if bip32_key_init_alloc(UInt32(version), UInt32(0), UInt32(0), chainCode_, chainCode.count,
                             pubKey_, pubKey.count, nil, 0, nil, 0, nil, 0, &subactkey) != WALLY_OK {
        throw GaError.GenericError()
    }
    if bip32_key_from_parent_alloc(subactkey, branch, UInt32(BIP32_FLAG_KEY_PUBLIC | BIP32_FLAG_SKIP_HASH), &branchkey) != WALLY_OK {
        throw GaError.GenericError()
    }
    if bip32_key_to_base58(branchkey, UInt32(BIP32_FLAG_KEY_PUBLIC), &xpubPtr) != WALLY_OK {
        throw GaError.GenericError()
    }
    return String(cString: xpubPtr!)
}

public func sha256d(_ input: [UInt8]) throws -> [UInt8] {
    let inputPtr: UnsafePointer<UInt8> = UnsafePointer(input)
    let outputPtr = UnsafeMutablePointer<UInt8>.allocate(capacity: Int(SHA256_LEN))
    if wally_sha256d(inputPtr, input.count, outputPtr, Int(SHA256_LEN)) != WALLY_OK {
        throw GaError.GenericError()
    }
    return Array(UnsafeBufferPointer(start: outputPtr, count: Int(SHA256_LEN)))
}

public func asset_final_vbf(values: [UInt64], numInputs: Int, abf: [UInt8], vbf: [UInt8]) throws -> [UInt8] {
    let valuesPtr: UnsafePointer<UInt64> = UnsafePointer(values)
    let abfPtr: UnsafePointer<UInt8> = UnsafePointer(abf)
    let vbfPtr: UnsafePointer<UInt8> = UnsafePointer(vbf)
    let len = Int(BLINDING_FACTOR_LEN)
    let bufferPtr = UnsafeMutablePointer<UInt8>.allocate(capacity: len)
    if wally_asset_final_vbf(valuesPtr, values.count, numInputs, abfPtr, abf.count, vbfPtr, vbf.count, bufferPtr, len) != WALLY_OK {
        throw GaError.GenericError()
    }
    return Array(UnsafeBufferPointer(start: bufferPtr, count: len))
}

public func flatten(_ inputs: [[UInt8]], fixedSize: Int32?) -> [UInt8] {
    return inputs
        .reduce([UInt8](), { (prev, item) in
            if let size = fixedSize, item.count < size {
                return prev + item + [UInt8](repeating: 0, count: Int(size) - item.count)
            }
            return prev + item
        })
}

public func bip32KeyFromBase58(_ input: String) throws -> ga.ext_key {
    var output: UnsafeMutablePointer<ga.ext_key>?
    let base58: UnsafeMutablePointer<CChar> = strdup(input)!
    if bip32_key_from_base58_alloc(base58, &output) != WALLY_OK {
        throw GaError.GenericError()
    }
    guard let output = output else {
        throw GaError.GenericError()
    }
    return output.pointee
}

public func bip85FromMnemonic(
    mnemonic: String,
    passphrase: String?,
    isTestnet: Bool = false,
    index: UInt32 = 0,
    numOfWords: UInt32 = 12
) -> String? {
    let version = isTestnet ? BIP32_VER_TEST_PRIVATE : BIP32_VER_MAIN_PRIVATE
    let seed512Ptr = UnsafeMutablePointer<UInt8>.allocate(capacity: Int(BIP39_SEED_LEN_512))
    var bip32KeyPtr: UnsafeMutablePointer<ga.ext_key>?
    let bip85Ptr = UnsafeMutablePointer<UInt8>.allocate(capacity: Int(HMAC_SHA512_LEN))
    var bip85Len: Int = 0
    var wordlistPtr: OpaquePointer?
    var resultPtr: UnsafeMutablePointer<CChar>?
    defer {
        bip32_key_free(bip32KeyPtr)
    }
    if bip39_mnemonic_to_seed512(strdup(mnemonic)!, passphrase != nil ? strdup(passphrase ?? "")! : nil , seed512Ptr, Int(BIP39_SEED_LEN_512)) != WALLY_OK {
        return nil
    }
    if bip32_key_from_seed_alloc(seed512Ptr, Int(BIP39_SEED_LEN_512), UInt32(version), UInt32(BIP32_FLAG_SKIP_HASH), &bip32KeyPtr) != WALLY_OK {
        return nil
    }
    if bip85_get_bip39_entropy(bip32KeyPtr, strdup(BIP39_WORD_LIST_LANG)!, numOfWords, index, bip85Ptr, Int(HMAC_SHA512_LEN), &bip85Len) != WALLY_OK {
        return nil
    }
    if bip39_get_wordlist(strdup(BIP39_WORD_LIST_LANG)!, &wordlistPtr) != WALLY_OK {
        return nil
    }
    if bip39_mnemonic_from_bytes(wordlistPtr, bip85Ptr, bip85Len, &resultPtr) != WALLY_OK {
        return nil
    }
    guard let resultPtr = resultPtr else {
        return nil
    }
    return String(cString: resultPtr)
}

public func getHashPrevouts(
    txhashes: [UInt8],
    outputIdxs: [UInt32]
) -> [UInt8]? {
    let txhashesPtr: UnsafePointer<UInt8> = UnsafePointer(txhashes)
    let outputIdxsPtr: UnsafePointer<UInt32> = UnsafePointer(outputIdxs)
    let len = Int(SHA256_LEN)
    let bufferPtr = UnsafeMutablePointer<UInt8>.allocate(capacity: len)
    if wally_get_hash_prevouts(txhashesPtr, txhashes.count, outputIdxsPtr, outputIdxs.count, bufferPtr, len) != WALLY_OK {
        return nil
    }
    return Array(UnsafeBufferPointer(start: bufferPtr, count: len))
}

public func wallyTxFromBytes(
    tx: [UInt8]
) -> UnsafeMutablePointer<ga.wally_tx>? {
    let txPtr: UnsafePointer<UInt8> = UnsafePointer(tx)
    var bufferPtr: UnsafeMutablePointer<ga.wally_tx>?
    
    if wally_tx_from_bytes(txPtr, tx.count, UInt32(WALLY_TX_FLAG_USE_ELEMENTS), &bufferPtr) != WALLY_OK {
        return nil
    }
    return bufferPtr
}

public func wallyTxGetOutputAsset(
    wallyTx: UnsafeMutablePointer<ga.wally_tx>,
    index: Int
) -> [UInt8]? {
    let output = wallyTx.pointee.outputs[index]
    return Array(UnsafeBufferPointer(start: output.asset, count: output.asset_len))
}

public func wallyTxGetOutputValue(
    wallyTx: UnsafeMutablePointer<ga.wally_tx>,
    index: Int
) -> [UInt8]? {
    let output = wallyTx.pointee.outputs[index]
    return Array(UnsafeBufferPointer(start: output.value, count: output.value_len))
}
