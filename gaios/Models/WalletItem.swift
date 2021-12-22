import Foundation
import PromiseKit

class WalletItem: Codable {

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

    func localizedName() -> String {
        if !name.isEmpty {
            return name
        }
        if pointer == 0 {
            return NSLocalizedString("id_main_account", comment: "")
        }
        return NSLocalizedString("id_account", comment: "") + " \(pointer)"
    }

    func generateNewAddress() -> Promise<String> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().compactMap(on: bgq) {_ in
            try SessionsManager.current.getReceiveAddress(details: ["subaccount": self.pointer])
        }.then(on: bgq) { call in
            call.resolve()
        }.compactMap(on: bgq) { data in
            let result = data["result"] as? [String: Any]
            return result?["address"] as? String ?? ""
        }
    }

    func getAddress() -> Promise<String> {
        if let address = receiveAddress {
            return Guarantee().compactMap { _ in
                return address
            }
        }
        return generateNewAddress().compactMap { address in
            self.receiveAddress = address
            return address
        }
    }

    func getBalance() -> Promise<[String: UInt64]> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().compactMap(on: bgq) {
            try SessionsManager.current.getBalance(details: ["subaccount": self.pointer, "num_confs": 0])
        }.then(on: bgq) { call in
            call.resolve()
        }.compactMap { data in
            let satoshi = data["result"] as? [String: UInt64]
            self.satoshi = satoshi ?? [:]
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
}
