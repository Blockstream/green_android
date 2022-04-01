import Foundation
import PromiseKit

struct Address: Codable {

    enum CodingKeys: String, CodingKey {
        case address = "address"
        case pointer = "pointer"
        case branch = "branch"
        case userPath = "user_path"
        case subaccount = "subaccount"
        case scriptType = "script_type"
        case addressType = "address_type"
        case script = "script"
        case subtype = "subtype"
    }

    let address: String
    let pointer: Int?
    let branch: Int?
    let subtype: UInt32?
    let userPath: [UInt32]?
    let subaccount: Int?
    let scriptType: Int?
    let addressType: String?
    let script: String?

    static func generate(with session: SessionManager, wallet: WalletItem) -> Promise<Address> {
        let bgq = DispatchQueue.global(qos: .background)
        return Guarantee().then(on: bgq) {_ in
            try session.getReceiveAddress(details: ["subaccount": wallet.pointer]).resolve()
        }.compactMap(on: bgq) { res in
            let result = res["result"] as? [String: Any]
            let data = try? JSONSerialization.data(withJSONObject: result!, options: [])
            return try? JSONDecoder().decode(Address.self, from: data!)
        }
    }

    static func validate(with wallet: WalletItem, hw: HWProtocol, addr: Address, network: String) -> Promise<String> {
        return Promise { seal in
            _ = hw.newReceiveAddress(network: getGdkNetwork(network), wallet: wallet, path: addr.userPath ?? [], csvBlocks: addr.subtype ?? 0)
                .subscribe(onNext: { data in
                    seal.fulfill(data)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }
}
