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
    }

    private let name: String
    let pointer: UInt32
    var receiveAddress: String?
    let receivingId: String
    let type: String
    var satoshi: [String: UInt64]?
    var btc: UInt64 { get { return satoshi?[getGdkNetwork(getNetwork()).getFeeAsset()]! ?? 0}}
    var recoveryChainCode: String?
    var recoveryPubKey: String?

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
            try SessionManager.shared.getReceiveAddress(details: ["subaccount": self.pointer])
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
            try SessionManager.shared.getBalance(details: ["subaccount": self.pointer, "num_confs": 0])
        }.then(on: bgq) { call in
            call.resolve()
        }.compactMap { data in
            let satoshi = data["result"] as? [String: UInt64]
            self.satoshi = satoshi ?? [:]
            return satoshi
        }
    }
}
