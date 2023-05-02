import Foundation
import PromiseKit

public class WalletItem: Codable, Equatable, Comparable, Hashable {

    enum CodingKeys: String, CodingKey {
        case name
        case pointer
        case receivingId = "receiving_id"
        case type
        case satoshi
        case recoveryChainCode = "recovery_chain_code"
        case recoveryPubKey = "recovery_pub_key"
        case recoveryXpub = "recovery_xpub"
        case hidden
        case bip44Discovered = "bip44_discovered"
        case coreDescriptors = "core_descriptors"
        case extendedPubkey = "slip132_extended_pubkey"
        case userPath = "user_path"
    }

    public let name: String
    public let pointer: UInt32
    public let receivingId: String
    public let type: AccountType
    public var recoveryChainCode: String?
    public var recoveryPubKey: String?
    public let bip44Discovered: Bool?
    public let recoveryXpub: String?
    public let hidden: Bool
    public var network: String?
    public let coreDescriptors: [String]?
    public let extendedPubkey: String?
    public let userPath: [Int]?
    public var hasTxs: Bool = false
    public var satoshi: [String: Int64]?

    public var gdkNetwork: GdkNetwork { getGdkNetwork(network!)}

    public var btc: Int64? {
        let feeAsset = gdkNetwork.getFeeAsset()
        return satoshi?[feeAsset]
    }

    public var bip32Pointer: UInt32 { isSinglesig ? pointer / 16 : pointer}
    public var accountNumber: UInt32 { bip32Pointer + 1 }

    public var isMultisig: Bool {
        switch type {
        case .standard, .amp, .twoOfThree:
            return true
        default:
            return false
        }
    }

    public var isSinglesig: Bool {
        switch type {
        case .legacy, .segwitWrapped, .segWit, .taproot:
            return true
        default:
            return false
        }
    }

    public static func == (lhs: WalletItem, rhs: WalletItem) -> Bool {
        return lhs.network == rhs.network &&
            lhs.name == rhs.name &&
            lhs.pointer == rhs.pointer &&
            lhs.receivingId == rhs.receivingId &&
            lhs.type == rhs.type &&
            lhs.recoveryChainCode == rhs.recoveryChainCode &&
            lhs.recoveryPubKey == rhs.recoveryPubKey
    }

    public static func < (lhs: WalletItem, rhs: WalletItem) -> Bool {
        let lhsNetwork = lhs.gdkNetwork
        let rhsNetwork = rhs.gdkNetwork
        if lhsNetwork == rhsNetwork {
            if lhs.type == rhs.type {
                return lhs.pointer < rhs.pointer
            }
            return lhs.type < rhs.type
        }
        return lhsNetwork < rhsNetwork
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(network)
        hasher.combine(pointer)
        hasher.combine(type)
    }
}
