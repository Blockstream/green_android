import Foundation
import PromiseKit

class WalletItem: Codable, Equatable, Comparable, Hashable {

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

    private let name: String
    let pointer: UInt32
    let receivingId: String
    let type: AccountType
    var satoshi: [String: Int64]?
    var recoveryChainCode: String?
    var recoveryPubKey: String?
    let bip44Discovered: Bool?
    let recoveryXpub: String?
    let hidden: Bool
    var network: String?
    let coreDescriptors: [String]?
    let extendedPubkey: String?
    let userPath: [Int]?

    var gdkNetwork: GdkNetwork { getGdkNetwork(network!)}
    var session: SessionManager? { WalletManager.current?.sessions[network ?? ""] }

    func localizedName() -> String {
        if !name.isEmpty {
            return name
        }
        switch type {
        case .legacy, .segwitWrapped, .segWit, .taproot:
            if accountNumber == 1 {
                return "\(NSLocalizedString(type.shortNameStringId, comment: "")) Account \(accountNumber)"
            } else {
                return "\(NSLocalizedString(type.shortNameStringId, comment: "")) \(accountNumber)"
            }
        default:
            if pointer == 0 {
                return NSLocalizedString("id_main_account", comment: "")
            }
            return NSLocalizedString("id_account", comment: "") + " \(pointer)"
        }
    }

    func localizedHint() -> String {
        return "\((NSLocalizedString(type.typeStringId, comment: "")).uppercased()) #\(self.accountNumber)"
    }

    var btc: Int64 {
        get {
            if let feeAsset = AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() {
                return satoshi?[feeAsset] ?? 0
            }
            return 0
        }
    }

    var bip32Pointer: UInt32 {
        switch type {
        case .legacy, .segwitWrapped, .segWit, .taproot:
            return pointer / 16
        default:
            return pointer
        }
    }

    var accountNumber: UInt32 { bip32Pointer + 1 }
    
    static func == (lhs: WalletItem, rhs: WalletItem) -> Bool {
        return lhs.network == rhs.network &&
            lhs.name == rhs.name &&
            lhs.pointer == rhs.pointer &&
            lhs.receivingId == rhs.receivingId &&
            lhs.type == rhs.type &&
            lhs.recoveryChainCode == rhs.recoveryChainCode &&
            lhs.recoveryPubKey == rhs.recoveryPubKey
    }

    static func < (lhs: WalletItem, rhs: WalletItem) -> Bool {
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

    func hash(into hasher: inout Hasher) {
        hasher.combine(network)
        hasher.combine(pointer)
        hasher.combine(type)
    }
}
