import Foundation

public typealias InputOutput = [String: Any]

public extension InputOutput {
    private func take<T>(_ key: String) -> T? {
        return self[key] as? T
    }
    var address: String? { take("address")}
    var addressee: String? { take("addressee")}
    var addressType: String? { take("address_type")}
    var isBlinded: Bool? { take("is_blinded")}
    var isConfidential: Bool? { take("is_confidential")}
    var unblindedAddress: String? { take("unblinded_address")}
    var isChange: Bool? { take("is_change")}
    var isOutput: Bool? { take("is_output")}
    var isRelevant: Bool? { take("is_relevant")}
    var isSpent: Bool? { take("is_spent")}
    var pointer: UInt32? { take("pointer")}
    var prevoutScript: String? { take("prevout_script")}
    var ptIdx: UInt32 { take("pt_idx") ?? 0 }
    var recoveryXpub: String? { take("recovery_xpub")}
    var satoshi: UInt64? { take("satoshi")}
    var script: String? { take("script")}
    var scriptType: UInt32? { take("script_type")}
    var scriptpubkey: String? { take("scriptpubkey")}
    var sequence: Int { take("sequence") ?? 0}
    var subaccount: UInt32? { take("subaccount")}
    var subtype: UInt32? { take("subtype")}
    var txHash: String? { take("txhash")}
    var serviceXpub: String? { take("service_xpub")}
    var userPath: [Int]? { take("user_path")}
    var aeHostCommitment: String? { take("ae_host_commitment")}
    var aeHostEntropy: String? { take("ae_host_entropy")}
    var commitment: String? { take("commitment")} // blinded value
    var assetblinder: String? { take("assetblinder")} // asset blinding factor
    var amountblinder: String? { take("amountblinder")} // value blinding factor
    var assetId: String? { take("asset_id")} // asset id for Liquid txs
    var blindingKey: String? { take("blinding_key")} // the blinding public key embedded into the blinded address we are sending to
    var ephPublicKey: String? { take("eph_public_key")} // our ephemeral public key for [un]blinding

    var hasUnblindingData: Bool {
        return assetId != nil && satoshi != nil && assetblinder != nil && amountblinder != nil && assetId.isNotEmpty && amountblinder.isNotEmpty && assetblinder.isNotEmpty
    }
    var getUnblindedString: String? {
        hasUnblindingData ? "\(satoshi ?? 0),\(assetId ?? ""),\(amountblinder ?? ""),\(assetblinder ?? "")" : nil
    }
    var isSegwit: Bool { [ "csv", "p2wsh", "p2wpkh", "p2sh-p2wpkh"].contains(addressType) }
    
    var getAssetIdBytes: [UInt8]? { assetId?.hexToBytes() }
    var getAbfs: [UInt8]? { assetblinder?.hexToBytes().reversed() }
    var getVbfs: [UInt8]? { amountblinder?.hexToBytes().reversed() }
    var getTxid: [UInt8]? { txHash?.hexToBytes().reversed() }
    var getEphKeypairPubBytes: [UInt8]? { ephPublicKey?.hexToBytes() }
    var getPublicKeyBytes: [UInt8]? { blindingKey?.hexToBytes() }
    var getRevertedAssetIdBytes: [UInt8]? { assetId?.hexToBytes().reversed() }
    var getCommitmentBytes: [UInt8]? { commitment?.hexToBytes() }
    var getUserPathAsInts: [Int]? { userPath?.map { Int($0) } }
}
