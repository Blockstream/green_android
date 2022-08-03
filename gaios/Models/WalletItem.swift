import Foundation
import PromiseKit

class WalletItem: Codable, Equatable {

    enum CodingKeys: String, CodingKey {
        case name
        case pointer
        case receiveAddress
        case receivingId = "receiving_id"
        case type
        case satoshi
        case recoveryChainCode = "recovery_chain_code"
        case recoveryPubKey = "recovery_pub_key"
        case bip44Discovered = "bip44_discovered"
        case recoveryXpub = "recovery_xpub"
        case hidden
    }

    private let name: String
    let pointer: UInt32
    var receiveAddress: String?
    let receivingId: String
    let type: String
    var satoshi: [String: UInt64]?
    var recoveryChainCode: String?
    var recoveryPubKey: String?
    let bip44Discovered: Bool?
    let recoveryXpub: String?
    let hidden: Bool?

    func localizedName() -> String {
        if !name.isEmpty {
            return name
        }
        if pointer == 0 {
            return NSLocalizedString("id_main_account", comment: "")
        }
        return NSLocalizedString("id_account", comment: "") + " \(pointer)"
    }

    func generateNewAddress() -> Promise<Address> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().then(on: bgq) {_ in
            SessionsManager.current!.getReceiveAddress(subaccount: self.pointer)
        }
    }

    func getAddress() -> Promise<String> {
        if let address = receiveAddress {
            return Guarantee().compactMap { _ in
                return address
            }
        }
        return generateNewAddress().compactMap { address in
            self.receiveAddress = address.address
            return address.address
        }
    }

    func getBalance() -> Promise<[String: UInt64]> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().then(on: bgq) {
            SessionsManager.current!.getBalance(subaccount: self.pointer, numConfs: 0)
        }.compactMap { satoshi in
            self.satoshi = satoshi
            return satoshi
        }
    }

    var btc: UInt64 {
        get {
            if let feeAsset = AccountsManager.shared.current?.gdkNetwork?.getFeeAsset() {
                return satoshi?[feeAsset] ?? 0
            }
            return 0
        }
    }

    static func == (lhs: WalletItem, rhs: WalletItem) -> Bool {
        return lhs.name == rhs.name &&
            lhs.pointer == rhs.pointer &&
            lhs.receivingId == rhs.receivingId &&
            lhs.type == rhs.type &&
            lhs.recoveryChainCode == rhs.recoveryChainCode &&
            lhs.recoveryPubKey == rhs.recoveryPubKey
    }

    func accountType() -> AccountType? {
        AccountType(rawValue: type)
    }
}
