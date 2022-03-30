import Foundation
import PromiseKit

class Address: Codable {

    enum CodingKeys: String, CodingKey {
        case address = "address"
        case pointer = "pointer"
        case branch = "branch"
        case userPath = "user_path"
    }

    let address: String
    let pointer: UInt32? = nil
    var branch: UInt32? = nil
    let subtype: UInt32? = nil
    let userPath: [UInt32]? = nil

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
            _ = hw.newReceiveAddress(network: getGdkNetwork(network), wallet: wallet, path: addr.userPath!, csvBlocks: addr.subtype ?? 0)
                .subscribe(onNext: { data in
                    seal.fulfill(data)
                }, onError: { err in
                    seal.reject(err)
                })
        }
    }
}
